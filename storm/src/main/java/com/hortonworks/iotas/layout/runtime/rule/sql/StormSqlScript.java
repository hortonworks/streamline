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

import backtype.storm.tuple.Values;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.Expression;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.StormSqlExpression;
import com.hortonworks.iotas.layout.runtime.script.Script;
import com.hortonworks.iotas.layout.runtime.script.engine.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

import static com.hortonworks.iotas.layout.runtime.rule.condition.expression.StormSqlExpression.RULE_SCHEMA;
import static com.hortonworks.iotas.layout.runtime.rule.condition.expression.StormSqlExpression.RULE_TABLE;

/**
 * Evaluates the {@link Expression} for each {@code Input} using the provided {@code Storm} SQL Engine
 */
public class StormSqlScript<O> extends Script<IotasEvent, O, StormSqlEngine> {
    private static final Logger LOG = LoggerFactory.getLogger(StormSqlScript.class);
    private final ValuesConverter<O> valuesConverter;

    public StormSqlScript(Expression expression, ScriptEngine<StormSqlEngine> scriptEngine) {
        this(expression, scriptEngine, null);
    }

    public StormSqlScript(Expression expression, ScriptEngine<StormSqlEngine> scriptEngine, ValuesConverter<O> valuesConverter) {
        super(expression.asString(), scriptEngine);
        this.valuesConverter = valuesConverter;

        // This is needed to avoid ServiceLoader limitation. Please read comments in RulesDataSourcesProvider
        // The delegate must be set before compiling the query
        RulesDataSourcesProvider.setDelegate(((StormSqlEngine)scriptEngine).getDataSourceProvider());

        ((StormSqlEngine)scriptEngine).compileQuery(createQuery((StormSqlExpression) expression));
    }

    private List<String> createQuery(StormSqlExpression expression) {
        final List<String> statements = new ArrayList<>(2);
        statements.add(expression.createTable(RULE_SCHEMA, RULE_TABLE));
        statements.add(expression.select(RULE_TABLE));
        return statements;
    }

    @Override
    public O evaluate(IotasEvent iotasEvent) throws ScriptException {
        final String expressionStr = expression;
        LOG.debug("Evaluating [{}] with [{}]", expressionStr, iotasEvent);
        Values result = null;
        if (iotasEvent != null) {
            result = scriptEngine.eval(createValues(iotasEvent));
        }
        LOG.debug("Expression [{}] evaluated to [{}]", expressionStr, result);
        return convert(result);
    }

    private Values createValues(IotasEvent iotasEvent) {
        final Values values = new Values();
        for (Object value : iotasEvent.getFieldsAndValues().values()) {
            values.add(value);
        }
        return values;
    }

    private O convert(Values result) {
        O output = null;
        if(valuesConverter != null) {
            output = valuesConverter.convert(result);
        } else {
            output = (O) result;
        }
        LOG.debug("Expression evaluation result [{}] converted to [{}]", result, output);
        return output;
    }


    public interface ValuesConverter<O> {
        /**
         * Converts the input Values to the specified output object
         */
        O convert(Values input);
    }

    public static class ValuesToBooleanConverter implements ValuesConverter<Boolean> {
        @Override
        public Boolean convert(Values input) {
            return input != null;
        }
    }
}
