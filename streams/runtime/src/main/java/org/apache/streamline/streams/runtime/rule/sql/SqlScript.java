/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streamline.streams.runtime.rule.sql;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.apache.streamline.common.Schema;
import org.apache.streamline.streams.IotasEvent;
import org.apache.streamline.streams.common.IotasEventImpl;
import org.apache.streamline.streams.layout.component.rule.exception.ConditionEvaluationException;
import org.apache.streamline.streams.runtime.rule.condition.expression.ExpressionRuntime;
import org.apache.streamline.streams.runtime.rule.condition.expression.StormSqlExpression;
import org.apache.streamline.streams.runtime.script.Script;
import org.apache.streamline.streams.runtime.script.engine.ScriptEngine;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.streamline.streams.common.IotasEventImpl.GROUP_BY_TRIGGER_EVENT;
import static org.apache.streamline.streams.runtime.rule.condition.expression.StormSqlExpression.RULE_SCHEMA;
import static org.apache.streamline.streams.runtime.rule.condition.expression.StormSqlExpression.RULE_TABLE;

/**
 * Evaluates the {@link ExpressionRuntime} for each {@code Input} using the provided {@code Storm} SQL Engine
 */
public class SqlScript extends Script<IotasEvent, Collection<IotasEvent>, SqlEngine> {
    private static final Logger LOG = LoggerFactory.getLogger(SqlScript.class);
    private ValuesConverter<IotasEvent> valuesConverter;
    private final List<Schema.Field> stormSqlFields;
    private final List<String> projectedFields;
    private final List<String> outputFields;
    public SqlScript(ExpressionRuntime expressionRuntime, ScriptEngine<SqlEngine> scriptEngine) {
        this(expressionRuntime, scriptEngine, null);
    }
    public SqlScript(ExpressionRuntime expressionRuntime, ScriptEngine<SqlEngine> scriptEngine,
                     ValuesConverter<IotasEvent> valuesConverter) {
        super(expressionRuntime.asString(), scriptEngine);
        this.valuesConverter = valuesConverter;
        stormSqlFields = ((StormSqlExpression) expressionRuntime).getStormSqlFields();
        if (!stormSqlFields.isEmpty()) {
            SqlEngine sqlEngine = (SqlEngine) scriptEngine;
            sqlEngine.compileQuery(createQuery((StormSqlExpression) expressionRuntime));
        }
        projectedFields = ((StormSqlExpression) expressionRuntime).getProjectedFields();
        outputFields = ((StormSqlExpression) expressionRuntime).getOutputFields();
    }

    private List<String> createQuery(StormSqlExpression expression) {
        final List<String> statements = new ArrayList<>(2);
        statements.add(expression.createTable(RULE_SCHEMA, RULE_TABLE));
        statements.addAll(expression.createFunctions());
        statements.add(expression.select(RULE_TABLE));
        return statements;
    }

    public void setValuesConverter(ValuesConverter<IotasEvent> valuesConverter) {
        this.valuesConverter = valuesConverter;
    }

    @Override
    public Collection<IotasEvent> evaluate(IotasEvent iotasEvent) throws ScriptException {
        LOG.debug("Evaluating [{}] with script engine [{}]", iotasEvent, scriptEngine);
        List<Values> result = null;
        if (stormSqlFields == null || stormSqlFields.isEmpty()) {
            if (iotasEvent == GROUP_BY_TRIGGER_EVENT) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(iotasEvent);
            }
        }
        try {
            if (iotasEvent == GROUP_BY_TRIGGER_EVENT) {
                result = scriptEngine.flush();
            } else if (iotasEvent != null) {
                result = scriptEngine.eval(createValues(iotasEvent));
            } else {
                LOG.error("Cannot evaluate null iotasEvent");
            }
        } catch (ConditionEvaluationException ex) {
            LOG.error("Got exception {} while processing IotasEvent {}", ex, iotasEvent);
        }
        LOG.debug("Result [{}]", result);
        return convert(result, iotasEvent);
    }

    private Values createValues(IotasEvent iotasEvent) {
        Values values = new Values();
        for (Schema.Field field : stormSqlFields) {
            Object value = iotasEvent.getFieldsAndValues().get(field.getName());
            if (value == null) {
                throw new ConditionEvaluationException("Missing property " + field.getName());
            }
            values.add(value);
        }
        return values;
    }

    private Collection<IotasEvent> convert(List<Values> result, final IotasEvent inputEvent) {
        Collection<IotasEvent> output = Collections.emptyList();
        if (result != null) {
            if (valuesConverter != null) {
                output = Collections2.transform(result, new Function<Values, IotasEvent>() {
                    @Override
                    public IotasEvent apply(Values values) {
                        return valuesConverter.convert(values, inputEvent);
                    }
                });
            } else {
                output = Collections.singletonList((IotasEvent) result);
            }
        }
        LOG.debug("Expression evaluation result [{}] converted to [{}]", result, output);
        return output;
    }

    public List<String> getOutputFields() {
        return outputFields;
    }

    public interface ValuesConverter<O> {
        /**
         * Converts the input Values to the specified output object
         */
        O convert(Values input, IotasEvent inputEvent);
    }

    public static class ValuesToIotasEventConverter implements ValuesConverter<IotasEvent> {
        private final List<String> outputFields;

        public ValuesToIotasEventConverter(List<String> projectedFields) {
            this.outputFields = projectedFields;
        }

        @Override
        public IotasEvent convert(Values input, IotasEvent inputEvent) {
            IotasEvent result;
            if (input == null) {
                result = null;
            } else if (outputFields != null && !outputFields.isEmpty()) {
                Map<String, Object> fieldsAndValues = new HashMap<>();
                for (int i = 0; i < outputFields.size(); i++) {
                    fieldsAndValues.put(outputFields.get(i), input.get(i));
                }
                if (inputEvent != null) {
                    result = new IotasEventImpl(fieldsAndValues, inputEvent.getDataSourceId(), inputEvent.getId(),
                                                inputEvent.getHeader(), inputEvent.getSourceStream());
                } else {
                    result = new IotasEventImpl(fieldsAndValues, "");
                }
            } else {
                result = inputEvent;
            }
            return result;
        }

        @Override
        public String toString() {
            return "ValuesToIotasEventConverter{" +
                    "outputFields=" + outputFields +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "SqlScript{" +
                "valuesConverter=" + valuesConverter +
                ", stormSqlFields=" + stormSqlFields +
                ", projectedFields=" + projectedFields +
                ", outputFields=" + outputFields +
                "} " + super.toString();
    }
}
