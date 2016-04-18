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

package com.hortonworks.iotas.layout.runtime.rule.sql;

import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.rule.exception.ConditionEvaluationException;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.ExpressionRuntime;
import org.apache.storm.tuple.Values;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.StormSqlExpression;
import com.hortonworks.iotas.layout.runtime.script.Script;
import com.hortonworks.iotas.layout.runtime.script.engine.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.iotas.layout.runtime.rule.condition.expression.StormSqlExpression.RULE_SCHEMA;
import static com.hortonworks.iotas.layout.runtime.rule.condition.expression.StormSqlExpression.RULE_TABLE;

/**
 * Evaluates the {@link ExpressionRuntime} for each {@code Input} using the provided {@code Storm} SQL Engine
 */
public class StormSqlScript<O> extends Script<IotasEvent, O, StormSqlEngine> {
    private static final Logger LOG = LoggerFactory.getLogger(StormSqlScript.class);
    private ValuesConverter<O> valuesConverter;
    private final List<Schema.Field> fieldsToEmit;
    private final List<String> projectedFields;
    public StormSqlScript(ExpressionRuntime expressionRuntime, ScriptEngine<StormSqlEngine> scriptEngine) {
        this(expressionRuntime, scriptEngine, null);
    }
    public StormSqlScript(ExpressionRuntime expressionRuntime, ScriptEngine<StormSqlEngine> scriptEngine,
                          ValuesConverter<O> valuesConverter) {
        super(expressionRuntime.asString(), scriptEngine);
        this.valuesConverter = valuesConverter;

        // This is needed to avoid ServiceLoader limitation. Please read comments in RulesDataSourcesProvider
        // The delegate must be set before compiling the query
        RulesDataSourcesProvider.setDelegate(((StormSqlEngine)scriptEngine).getDataSourceProvider());
        ((StormSqlEngine)scriptEngine).compileQuery(createQuery((StormSqlExpression) expressionRuntime));
        fieldsToEmit = ((StormSqlExpression) expressionRuntime).getFieldsToEmit();
        projectedFields = ((StormSqlExpression) expressionRuntime).getProjectedFields();
    }

    public void setValuesConverter(ValuesConverter<O> valuesConverter) {
        this.valuesConverter = valuesConverter;
    }

    private List<String> createQuery(StormSqlExpression expression) {
        final List<String> statements = new ArrayList<>(2);
        statements.add(expression.createTable(RULE_SCHEMA, RULE_TABLE));
        statements.addAll(expression.createFunctions());
        statements.add(expression.select(RULE_TABLE));
        return statements;
    }

    @Override
    public O evaluate(IotasEvent iotasEvent) throws ScriptException {
        final String expressionStr = expression;
        LOG.debug("Evaluating [{}] with [{}]", expressionStr, iotasEvent);
        Values result = null;
        if (iotasEvent != null) {
            try {
                result = scriptEngine.eval(createValues(iotasEvent));
            } catch (ConditionEvaluationException ex) {
                LOG.error("Got exception {} while processing IotasEvent {}", ex, iotasEvent);
            }
        }
        LOG.debug("Expression [{}] evaluated to [{}]", expressionStr, result);
        return convert(result, iotasEvent);
    }

    private Values createValues(IotasEvent iotasEvent) {
        final Values values = new Values();
        for(Schema.Field field: fieldsToEmit) {
            Object value = iotasEvent.getFieldsAndValues().get(field.getName());
            if (value == null) {
                throw new ConditionEvaluationException("Missing property " + field.getName());
            }
            values.add(value);
        }
        return values;
    }

    private O convert(Values result, IotasEvent inputEvent) {
        O output = null;
        if(valuesConverter != null) {
            output = valuesConverter.convert(result, inputEvent);
        } else {
            output = (O) result;
        }
        LOG.debug("Expression evaluation result [{}] converted to [{}]", result, output);
        return output;
    }

    public List<String> getProjectedFields() {
        return projectedFields;
    }

    public interface ValuesConverter<O> {
        /**
         * Converts the input Values to the specified output object
         */
        O convert(Values input, IotasEvent inputEvent);
    }

    public static class ValuesToBooleanConverter implements ValuesConverter<Boolean> {
        @Override
        public Boolean convert(Values input, IotasEvent inputEvent) {
            return input != null;
        }
    }

    public static class ValuesToIotasEventConverter implements ValuesConverter<IotasEvent> {
        private final List<String> projectedFields;

        public ValuesToIotasEventConverter(List<String> projectedFields) {
            this.projectedFields = projectedFields;
        }

        @Override
        public IotasEvent convert(Values input, IotasEvent inputEvent) {
            IotasEvent result;
            if (input == null) {
                result = null;
            } else if (projectedFields != null) {
                Map<String, Object> fieldsAndValues = new HashMap<>();
                for (int i = 0; i < projectedFields.size(); i++) {
                    fieldsAndValues.put(projectedFields.get(i), input.get(i));
                }
                result = new IotasEventImpl(fieldsAndValues, inputEvent.getDataSourceId(), inputEvent.getId(),
                                            inputEvent.getHeader(), inputEvent.getSourceStream());
            } else {
                result = inputEvent;
            }
            return result;
        }
    }
}
