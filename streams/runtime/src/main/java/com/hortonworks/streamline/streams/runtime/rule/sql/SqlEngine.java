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


import com.hortonworks.streamline.streams.runtime.script.engine.ScriptEngine;
import com.hortonworks.streamline.streams.sql.StreamlineSql;
import com.hortonworks.streamline.streams.sql.runtime.ChannelContext;
import com.hortonworks.streamline.streams.sql.runtime.CorrelatedValues;
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
            StreamlineSql streamlineSql = StreamlineSql.construct();
            streamlineSql.execute(statements, channelHandler);
            channelContext = RulesDataSourcesProvider.getDataSource().getChannelContext();
            LOG.info("Query statements successfully compiled, channelContext set to {}", channelContext);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error compiling query. Statements [%s]", statements), e);
        }
    }

    public List<CorrelatedValues> eval(CorrelatedValues input) {
        channelContext.emit(input);
        List<CorrelatedValues> res = channelHandler.getResult();
        channelHandler.clearResult();
        return res;
    }

    /*
     * force evaluation of pending results, for e.g. evaluate last group in case of group-by
     */
    public List<CorrelatedValues> flush() {
        channelContext.flush();
        List<CorrelatedValues> res = channelHandler.getResult();
        channelHandler.clearResult();
        return res;
    }

    @Override
    public String toString() {
        return "SqlEngine{" +
                "channelContext=" + channelContext +
                ", channelHandler=" + channelHandler +
                '}';
    }
}
