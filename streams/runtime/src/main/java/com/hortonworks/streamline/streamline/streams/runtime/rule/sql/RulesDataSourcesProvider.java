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

package com.hortonworks.streamline.streams.runtime.rule.sql;

import com.hortonworks.streamline.streams.runtime.rule.condition.expression.StormSqlExpression;
import org.apache.storm.sql.runtime.DataSource;
import org.apache.storm.sql.runtime.DataSourcesProvider;
import org.apache.storm.sql.runtime.FieldInfo;
import org.apache.storm.sql.runtime.ISqlTridentDataSource;

import java.net.URI;
import java.util.List;
import java.util.Properties;

public class RulesDataSourcesProvider implements DataSourcesProvider {
    private static final ThreadLocal<RulesDataSource> dataSource = new ThreadLocal<RulesDataSource>() {
        @Override
        protected RulesDataSource initialValue() {
            return new RulesDataSource();
        }
    };

    @Override
    public String scheme() {
        return StormSqlExpression.RULE_SCHEMA;
    }

    @Override
    public DataSource construct(URI uri, String s, String s1, List<FieldInfo> list) {
        return dataSource.get();
    }

    @Override public ISqlTridentDataSource constructTrident(URI uri, String s, String s1,
        Properties properties, List<FieldInfo> list) {
        return null;
    }

    public static RulesDataSource getDataSource() {
        return dataSource.get();
    }

    @Override
    public String toString() {
        return "RulesDataSourcesProvider{" +
                "dataSource=" + dataSource.get() +
                '}';
    }
}
