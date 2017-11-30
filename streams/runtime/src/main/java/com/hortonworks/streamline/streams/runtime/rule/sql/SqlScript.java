/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/


package com.hortonworks.streamline.streams.runtime.rule.sql;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.common.event.correlation.EventCorrelationInjector;
import com.hortonworks.streamline.streams.layout.component.rule.exception.ConditionEvaluationException;
import com.hortonworks.streamline.streams.runtime.rule.condition.expression.ExpressionRuntime;
import com.hortonworks.streamline.streams.runtime.rule.condition.expression.StormSqlExpression;
import com.hortonworks.streamline.streams.runtime.script.Script;
import com.hortonworks.streamline.streams.runtime.script.engine.ScriptEngine;
import com.hortonworks.streamline.streams.sql.runtime.CorrelatedEventsAwareValues;
import com.hortonworks.streamline.streams.sql.runtime.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.hortonworks.streamline.streams.common.StreamlineEventImpl.GROUP_BY_TRIGGER_EVENT;
import static com.hortonworks.streamline.streams.runtime.rule.condition.expression.StormSqlExpression.RULE_SCHEMA;
import static com.hortonworks.streamline.streams.runtime.rule.condition.expression.StormSqlExpression.RULE_TABLE;

/**
 * Evaluates the {@link ExpressionRuntime} for each {@code Input} using the provided {@code Storm} SQL Engine
 */
public class SqlScript extends Script<StreamlineEvent, Collection<StreamlineEvent>, SqlEngine> {
    private static final Logger LOG = LoggerFactory.getLogger(SqlScript.class);
    private ValuesConverter<StreamlineEvent> valuesConverter;
    private final List<Schema.Field> stormSqlFields;
    private final List<String> projectedFields;
    private final List<String> outputFields;
    /*
     * when there are no references to input fields, we add a dummy field so that the
     * rule table is created with the dummy field and the values can be processed by storm-sql.
     * e.g. SELECT RAND() from inputStream results in
     *      CREATE EXTERNAL TABLE RULETABLE (dummy INTEGER)
     */
    private static final Schema.Field DUMMY_FIELD = Schema.Field.of("dummy", Schema.Type.INTEGER);
    private static final Integer DUMMY_FIELD_VALUE = 0;

    public SqlScript(ExpressionRuntime expressionRuntime, ScriptEngine<SqlEngine> scriptEngine) {
        this(expressionRuntime, scriptEngine, null);
    }
    public SqlScript(ExpressionRuntime expressionRuntime, ScriptEngine<SqlEngine> scriptEngine,
                     ValuesConverter<StreamlineEvent> valuesConverter) {
        super(expressionRuntime.asString(), scriptEngine);
        this.valuesConverter = valuesConverter;
        StormSqlExpression stormSqlExpression;
        if (expressionRuntime instanceof  StormSqlExpression) {
            stormSqlExpression = (StormSqlExpression) expressionRuntime;
            stormSqlFields = stormSqlExpression.getStormSqlFields();
        } else {
            throw new IllegalArgumentException("ExpressionRuntime " + expressionRuntime + " not an instance of StormSqlExpression");
        }
        SqlEngine sqlEngine;
        if (scriptEngine instanceof SqlEngine) {
            sqlEngine = (SqlEngine) scriptEngine;
        } else {
            throw new IllegalArgumentException("ScriptEngine " + scriptEngine + " not an instance of SqlEngine");
        }
        if (stormSqlFields.isEmpty()) {
            stormSqlExpression.addStormSqlField(DUMMY_FIELD);
            stormSqlFields.add(DUMMY_FIELD);
        }
        sqlEngine.compileQuery(createQuery(stormSqlExpression));
        projectedFields = stormSqlExpression.getProjectedFields();
        outputFields = stormSqlExpression.getOutputFields();
    }

    private List<String> createQuery(StormSqlExpression expression) {
        final List<String> statements = new ArrayList<>(2);
        statements.add(expression.createTable(RULE_SCHEMA, RULE_TABLE));
        statements.addAll(expression.createFunctions());
        statements.add(expression.select(RULE_TABLE));
        return statements;
    }

    public void setValuesConverter(ValuesConverter<StreamlineEvent> valuesConverter) {
        this.valuesConverter = valuesConverter;
    }

    @Override
    public Collection<StreamlineEvent> evaluate(StreamlineEvent event) throws ScriptException {
        LOG.debug("Evaluating [{}] with script engine [{}]", event, scriptEngine);
        List<CorrelatedEventsAwareValues> result = null;
        if (stormSqlFields == null || stormSqlFields.isEmpty()) {
            if (event == GROUP_BY_TRIGGER_EVENT) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(event);
            }
        }
        try {
            if (event == GROUP_BY_TRIGGER_EVENT) {
                result = scriptEngine.flush();
            } else if (event != null) {
                result = scriptEngine.eval(createValues(event));
            } else {
                LOG.error("Cannot evaluate null event");
            }
        } catch (ConditionEvaluationException ex) {
            LOG.error("Got exception {} while processing StreamlineEvent {}", ex, event);
        }
        LOG.debug("Result [{}]", result);
        return convert(result, event);
    }

    private CorrelatedEventsAwareValues createValues(StreamlineEvent event) {
        Values values = new Values();
        for (Schema.Field field : stormSqlFields) {
            Object value;
            if (field == DUMMY_FIELD) {
                value = DUMMY_FIELD_VALUE;
            } else {
                value = event.get(field.getName());
                if (value == null) {
                    throw new ConditionEvaluationException("Missing property " + field.getName());
                }
            }
            values.add(value);
        }
        return CorrelatedEventsAwareValues.of(Collections.singletonList(event), values);
    }

    private Collection<StreamlineEvent> convert(List<CorrelatedEventsAwareValues> result,
                                                final StreamlineEvent inputEvent) {
        Collection<StreamlineEvent> output = Collections.emptyList();
        if (result != null) {
            if (valuesConverter != null) {
                output = Collections2.transform(result, new Function<CorrelatedEventsAwareValues, StreamlineEvent>() {
                    @Override
                    public StreamlineEvent apply(CorrelatedEventsAwareValues values) {
                        return valuesConverter.convert(values, inputEvent);
                    }
                });
            } else {
                throw new IllegalArgumentException("valueConverter is null");
            }
        }
        LOG.debug("Expression evaluation result [{}] converted to [{}]", result, output);
        return output;
    }

    public List<String> getOutputFields() {
        return outputFields;
    }

    public interface ValuesConverter<O> extends Serializable {
        /**
         * Converts the input Values to the specified output object
         */
        O convert(CorrelatedEventsAwareValues input, StreamlineEvent inputEvent);
    }

    public static class CorrelatedValuesToStreamlineEventConverter implements ValuesConverter<StreamlineEvent> {
        private final List<String> outputFields;
        private transient EventCorrelationInjector eventCorrelationInjector;

        public CorrelatedValuesToStreamlineEventConverter(List<String> projectedFields) {
            this.outputFields = projectedFields;
        }

        @Override
        public StreamlineEvent convert(CorrelatedEventsAwareValues input, StreamlineEvent inputEvent) {
            if (this.eventCorrelationInjector == null) {
                this.eventCorrelationInjector = new EventCorrelationInjector();
            }

            StreamlineEvent result;
            if (input == null) {
                return null;
            } else if (outputFields != null && !outputFields.isEmpty()) {
                StreamlineEventImpl.Builder builder = StreamlineEventImpl.builder();
                for (int i = 0; i < outputFields.size(); i++) {
                    builder.put(outputFields.get(i), input.get(i));
                }
                if (inputEvent != null) {
                    result = builder.dataSourceId(inputEvent.getDataSourceId())
                            .header(inputEvent.getHeader())
                            .sourceStream(inputEvent.getSourceStream())
                            .build();
                } else {
                    result = builder.build();
                }
            } else {
                result = inputEvent;
            }

            return eventCorrelationInjector.injectCorrelationInformation(result, input.getCorrelated());
        }

        @Override
        public String toString() {
            return "ValuesToStreamlineEventConverter{" +
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
