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

import org.apache.storm.sql.runtime.DataSource;
import org.apache.storm.sql.runtime.DataSourcesProvider;
import org.apache.storm.sql.runtime.FieldInfo;
import org.apache.storm.sql.runtime.ISqlTridentDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

/** This is an auxiliary service class that is needed in order to circumvent a limitation of {@link java.util.ServiceLoader},
 * which uses reflection to create the service classes, but does not cover the case where the service class is an
 * inner class. We need the service class to be an inner class for the reasons justified in {@link StormSqlEngine}.
 * */

public class RulesDataSourcesProvider implements DataSourcesProvider {
    private static DataSourcesProvider delegate;
    protected static final Logger log = LoggerFactory.getLogger(RulesDataSourcesProvider.class);

    public RulesDataSourcesProvider() {
        log.debug("Created RulesDataSourcesProvider with delegate [{}]", delegate);
    }

    @Override
    public String scheme() {
        return delegate.scheme();
    }

    @Override
    public DataSource construct(URI uri, String s, String s1, List<FieldInfo> list) {
        return delegate.construct(uri, s, s1, list);
    }

    @Override
    public ISqlTridentDataSource constructTrident(URI uri, String s, String s1, String s2, List<FieldInfo> list) {
        return delegate.constructTrident(uri, s, s1, s2, list);
    }

    static void setDelegate(DataSourcesProvider delegate) {
        RulesDataSourcesProvider.delegate = delegate;
        log.debug("Set DataSourcesProvider delegate to [{}]", delegate.getClass().getName());
    }
}
