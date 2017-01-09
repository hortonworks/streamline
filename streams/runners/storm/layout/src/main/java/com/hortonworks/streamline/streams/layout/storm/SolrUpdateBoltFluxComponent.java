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
package com.hortonworks.streamline.streams.layout.storm;

import java.util.ArrayList;
import java.util.List;

import static com.hortonworks.streamline.streams.layout.storm.DruidBoltFluxComponent.KEY_BATCH_SIZE;

/**
 *
 */
public class SolrUpdateBoltFluxComponent extends AbstractFluxComponent {

    public final static String JSON_KEY_SOLR_ZK_HOST_STRING = "solrZkHostString";
    public final static String JSON_KEY_SOLR_COLLECTION_NAME = "solrCollectionName";
    public final static String JSON_KEY_COMMIT_BATCH_SIZE = "commitBatchSize";
    public final static String JSON_KEY_JSON_TUPLE_FIELD = "jsonTupleField";
    public final static String JSON_KEY_SOLR_JSON_UPDATE_URL = "solrJsonUpdateUrl";

    @Override
    protected void generateComponent() {
        final String boltId = "solrUpdateBolt" + UUID_FOR_COMPONENTS;
        final String boltClassName = "org.apache.storm.solr.bolt.SolrUpdateBolt";
        final List<Object> boltConstructorArgs = new ArrayList<>();
        boltConstructorArgs.add(getRefYaml(addSolrConfig()));
        boltConstructorArgs.add(getRefYaml(addSolrMapper()));
        boltConstructorArgs.add(getRefYaml(addSolrCommitStrategy()));
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, null);
        addParallelismToComponent();
    }

    private String addSolrConfig() {
        final String componentId = "solrConfig" + UUID_FOR_COMPONENTS;
        final String className = "org.apache.storm.solr.config.SolrConfig";
        final List<Object> constructorArgs = new ArrayList<Object>() {{
            add(conf.get(JSON_KEY_SOLR_ZK_HOST_STRING));
        }};

        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    private String addSolrMapper() {
        final String componentId = "streamlineSolrJsonMapper" + UUID_FOR_COMPONENTS;
        final String className = "com.hortonworks.streamline.streams.runtime.storm.bolt.solr.StreamlineSolrJsonMapper";
        final List<Object> constructorArgs = new ArrayList<Object>() {{
            add(conf.get(JSON_KEY_SOLR_COLLECTION_NAME));
        }};

        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    private String addSolrCommitStrategy() {
        final String componentId = "countBasedCommit" + UUID_FOR_COMPONENTS;
        final String className = "org.apache.storm.solr.config.CountBasedCommit";
        final List<Object> constructorArgs = new ArrayList<Object>() {{
            add(conf.get(JSON_KEY_COMMIT_BATCH_SIZE));
        }};

        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }
}
