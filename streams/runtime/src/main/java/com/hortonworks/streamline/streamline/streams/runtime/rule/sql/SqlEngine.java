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


import org.apache.streamline.streams.runtime.script.engine.ScriptEngine;
import org.apache.storm.sql.StormSql;
import org.apache.storm.sql.runtime.ChannelContext;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of Storm SQL engine that evaluates pre-compiled queries for each input
 */
public class SqlEngine implements ScriptEngine<SqlEngine> {
    protected static final Logger LOG = LoggerFactory.getLogger(SqlEngine.class);

    @Override
    public SqlEngine getEngine() {
        return this;
    }

    private volatile ChannelContext channelContext;
    private final RulesChannelHandler channelHandler;

    public SqlEngine() {
        channelHandler = new RulesChannelHandler();
    }

    public void compileQuery(List<String> statements) {
        try {
            LOG.info("Compiling query statements {}", statements);
            StormSql stormSql = StormSql.construct();
            stormSql.execute(statements, channelHandler);
            channelContext = RulesDataSourcesProvider.getDataSource().getChannelContext();
            LOG.info("Query statements successfully compiled, channelContext set to {}", channelContext);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error compiling query. Statements [%s]", statements), e);
        }
    }

    public List<Values> eval(Values input) {
        channelContext.emit(input);
        List<Values> res = channelHandler.getResult();
        channelHandler.clearResult();
        return res;
    }

    /*
     * force evaluation of pending results, for e.g. evaluate last group in case of group-by
     */
    public List<Values> flush() {
        channelContext.flush();
        List<Values> res = channelHandler.getResult();
        channelHandler.clearResult();
        return res;
    }

    public void execute(Tuple tuple, OutputCollector outputCollector) {
        outputCollector.emit(tuple, createValues(tuple));
    }

    private Values createValues(Tuple input) {
        return (Values) input.getValues();
    }


    @Override
    public String toString() {
        return "SqlEngine{" +
                "channelContext=" + channelContext +
                ", channelHandler=" + channelHandler +
                '}';
    }
}
