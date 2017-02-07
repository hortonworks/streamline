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

package com.hortonworks.streamline.streams.runtime.processor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.exception.ProcessingException;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import com.hortonworks.streamline.streams.layout.component.rule.action.Action;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Expression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.FieldExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.GroupBy;
import com.hortonworks.streamline.streams.runtime.ProcessorRuntime;
import com.hortonworks.streamline.streams.runtime.rule.RuleRuntime;
import org.apache.commons.lang3.StringUtils;
import com.hortonworks.streamline.streams.runtime.rule.action.ActionRuntime;
import com.hortonworks.streamline.streams.runtime.rule.action.ActionRuntimeContext;
import com.hortonworks.streamline.streams.runtime.rule.condition.expression.GroovyExpression;
import com.hortonworks.streamline.streams.runtime.rule.condition.expression.StormSqlExpression;
import com.hortonworks.streamline.streams.runtime.rule.sql.SqlEngine;
import com.hortonworks.streamline.streams.runtime.rule.sql.SqlScript;
import com.hortonworks.streamline.streams.runtime.script.GroovyScript;
import com.hortonworks.streamline.streams.runtime.script.Script;
import com.hortonworks.streamline.streams.runtime.script.engine.GroovyScriptEngine;
import com.hortonworks.streamline.streams.runtime.transform.ActionRuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.common.StreamlineEventImpl.GROUP_BY_TRIGGER_EVENT;
import static com.hortonworks.streamline.streams.layout.component.rule.expression.Window.WINDOW_ID;

/**
 * Represents a runtime rules processor
 */
public class RuleProcessorRuntime implements Serializable, ProcessorRuntime {
    protected static final Logger LOG = LoggerFactory.getLogger(RuleProcessorRuntime.class);
    private static final GroupBy GROUP_BY_WINDOWID = new GroupBy(new FieldExpression(Schema.Field.of(WINDOW_ID, Schema.Type.LONG)));

    public enum ScriptType {GROOVY, SQL}

    private final RulesProcessor rulesProcessor;
    private final ScriptType scriptType;
    private List<RuleRuntime> rulesRuntime = new ArrayList<>();
    private Map<String, List<RuleRuntime>> streamToRuleRuntimes;
    private List<RuleRuntime> allRuleRuntimes;
    private boolean processAll = true;

    public RuleProcessorRuntime(RulesProcessor rulesProcessor, ScriptType scriptType) {
        this.rulesProcessor = rulesProcessor;
        this.scriptType = scriptType;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        initializeRuleRuntimes(config);
        buildStreamToRulesRuntime();
        this.processAll = this.rulesProcessor.getProcessAll();
    }

    //for testing
    void initializeWithRuleRuntimesForTesting(Map<String, Object> config, List<RuleRuntime> ruleRuntimes) {
        rulesRuntime = ruleRuntimes;
        buildStreamToRulesRuntime();
        this.processAll = this.rulesProcessor.getProcessAll();
    }

    @Override
    public List<Result> process(StreamlineEvent event) throws ProcessingException {
        List<Result> results = new ArrayList<>();
        try {
            List<RuleRuntime> ruleRuntimes = getRulesRuntime(event);
            LOG.debug("Process event {}, rule runtimes {}", event, ruleRuntimes);
            for (RuleRuntime rr : ruleRuntimes) {
                boolean succeeded = false;
                for (StreamlineEvent result : rr.evaluate(event)) {
                    if (result != null) {
                        results.addAll(rr.process(result));
                        succeeded = true;
                    }
                }
                if(!processAll && succeeded)
                    break;
            }
        } catch (Exception e) {
            String message = String.format("Error evaluating rule processor with id: %s, error: %s",
                    rulesProcessor.getId(), e.getMessage());
            LOG.error(message, e);
            throw new ProcessingException(message, e);
        }
        return results;
    }

    @Override
    public String toString() {
        return "RuleProcessorRuntime{" + rulesProcessor + ", " + rulesRuntime + '}';
    }

    @Override
    public void cleanup() {

    }

    private List<RuleRuntime> getRulesRuntime(StreamlineEvent event) throws ProcessingException {
        if (event == GROUP_BY_TRIGGER_EVENT) {
            return allRuleRuntimes;
        }
        String inputStream = event.getSourceStream();
        if (StringUtils.isEmpty(inputStream)) {
            throw new ProcessingException("Event SourceStream is empty");
        }
        List<RuleRuntime> result = streamToRuleRuntimes.get(inputStream);
        if (result == null) {
            LOG.debug("Could not find matching rules for input stream {}. Will not process event.", inputStream);
            result = Collections.emptyList();
        }
        return result;
    }

    private void initializeRuleRuntimes(Map<String, Object> config) {
        List<Rule> rules = rulesProcessor.getRules();
        if (rules != null) {
            for (Rule rule: rules) {
                RuleRuntime ruleRuntime;
                Script script = null;
                if (ScriptType.GROOVY.equals(scriptType)) {
                    script = createGroovyScript(rule);
                } else if (ScriptType.SQL.equals(scriptType)) {
                    script = createSqlScript(rule);
                } else {
                    throw new RuntimeException("Ruleruntime scriptType unsupported: " + scriptType );
                }
                ruleRuntime = new RuleRuntime(rule, script, createActionRuntimes(rule));
                rulesRuntime.add(ruleRuntime);
                ruleRuntime.initialize(config);
            }
            LOG.info("ruleRuntimes [{}]", rulesRuntime);
        }
    }

    private void buildStreamToRulesRuntime() {
        Map<String, List<RuleRuntime>> map = new HashMap<>();
        for (RuleRuntime rr : rulesRuntime) {
            for (String streamId : rr.getRule().getStreams()) {
                List<RuleRuntime> ruleRuntimes = map.get(streamId);
                if (ruleRuntimes == null) {
                    ruleRuntimes = new ArrayList<>();
                    map.put(streamId, ruleRuntimes);
                }
                ruleRuntimes.add(rr);
            }
        }
        streamToRuleRuntimes = ImmutableMap.copyOf(map);
        ImmutableSet.Builder<RuleRuntime> builder = ImmutableSet.builder();
        for(List<RuleRuntime> ruleRuntimes: streamToRuleRuntimes.values()) {
            builder.addAll(ruleRuntimes);
        }
        allRuleRuntimes = builder.build().asList();
    }

    private List<ActionRuntime> createActionRuntimes(Rule rule) {
        List<ActionRuntime> runtimeActions = new ArrayList<>();
        for (Action action : rule.getActions()) {
            final ActionRuntime actionRuntime = ActionRuntimeService.get().get(action);
            actionRuntime.setActionRuntimeContext(new ActionRuntimeContext(rule, action));
            runtimeActions.add(actionRuntime);
        }
        return runtimeActions;
    }


    private Script createGroovyScript(Rule rule) {
        LOG.info("Creating groovy execution script for rule {} ", rule);
        GroovyExpression groovyExpression = new GroovyExpression(rule.getCondition());
        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine();
        GroovyScript<Boolean> groovyScript = createHelperGroovyScript(groovyExpression, groovyScriptEngine);
        GroovyScript<Collection<StreamlineEvent>> wrapper = new GroovyScript<Collection<StreamlineEvent>>(groovyExpression.asString(),
                groovyScriptEngine) {
            @Override
            public Collection<StreamlineEvent> evaluate(StreamlineEvent event) throws ScriptException {
                if (groovyScript.evaluate(event)) {
                    return Collections.singletonList(event);
                } else {
                    return Collections.emptyList();
                }
            }
        };
        return wrapper;
    }

    private GroovyScript<Boolean> createHelperGroovyScript(GroovyExpression groovyExpression, GroovyScriptEngine groovyScriptEngine) {
        return new GroovyScript<Boolean>(groovyExpression.asString(), groovyScriptEngine) {
            @Override
            public Boolean evaluate(StreamlineEvent event) throws ScriptException {
                Boolean evaluates = false;
                try {
                    evaluates =  super.evaluate(event);
                } catch (ScriptException e) {
                    if (e.getCause() != null && e.getCause() instanceof groovy.lang.MissingPropertyException) {
                        // Occurs when not all the properties required for evaluating the script are set. This can happen for example
                        // when receiving an StreamlineEvent that does not have all the fields required to evaluate the expression
                        LOG.debug("Missing property required to evaluate expression. {}", e.getCause().getMessage());
                        LOG.trace("",e);
                        evaluates = false;
                    } else {
                        throw e;
                    }
                }
                return evaluates;
            }
        };
    }

    private Script createSqlScript(Rule rule) {
        SqlEngine sqlEngine = new SqlEngine();
        LOG.info("Built sqlEngine {}", sqlEngine);
        StormSqlExpression stormSqlExpression = createSqlExpression(rule);
        SqlScript sqlScript = new SqlScript(stormSqlExpression, sqlEngine);
        LOG.info("Built SqlScript {}", sqlScript);
        SqlScript.ValuesToStreamlineEventConverter valuesConverter = new SqlScript.ValuesToStreamlineEventConverter(sqlScript.getOutputFields());
        sqlScript.setValuesConverter(valuesConverter);
        LOG.info("valuesConverter {}", valuesConverter);
        return sqlScript;
    }

    private StormSqlExpression createSqlExpression(Rule rule) {
        List<Expression> groupByExpressions = new ArrayList<>();
        if (rule.getWindow() != null) {
            groupByExpressions.addAll(GROUP_BY_WINDOWID.getExpressions());
        }
        if (rule.getGroupBy() != null) {
            groupByExpressions.addAll(rule.getGroupBy().getExpressions());
        }
        StormSqlExpression stormSqlExpression = new StormSqlExpression(rule.getCondition(),
                                                    rule.getProjection(),
                                                    groupByExpressions.isEmpty() ? null : new GroupBy(groupByExpressions),
                                                    groupByExpressions.isEmpty() ? null : rule.getHaving());
        LOG.info("Built stormSqlExpression {}", stormSqlExpression);
        return stormSqlExpression;
    }
}

