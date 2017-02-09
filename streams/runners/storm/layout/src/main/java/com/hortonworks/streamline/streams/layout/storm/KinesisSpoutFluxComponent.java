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
package com.hortonworks.streamline.streams.layout.storm;

import java.util.ArrayList;
import java.util.List;

public class KinesisSpoutFluxComponent extends AbstractFluxComponent {
    private static final String KEY_STREAM_NAME = "streamName";
    private static final String KEY_SHARD_ITERATOR_TYPE = "shardIteratorType";
    private static final String KEY_TIMESTAMP = "timestamp";
    // properties for ExponentialBackoffRetrier
    private static final String KEY_RETRY_INITIAL_DELAY_MS = "retryInitialDelayMs";
    private static final String KEY_RETRY_BASE_SECONDS = "retryBaseSeconds";
    private static final String KEY_RETRY_MAX_TRIES = "retryMaxTries";
    // Zk related
    private static final String KEY_ZK_URL = "zkUrl";
    private static final String KEY_ZK_PATH = "zkPath";
    private static final String KEY_ZK_SESSION_TIMEOUT_MS = "zkSessionTimeoutMs";
    private static final String KEY_ZK_CONNECTION_TIMEOUT_MS = "zkConnectionTimeoutMs";
    private static final String KEY_ZK_COMMIT_INTERVAL_MS = "zkCommitIntervalMs";
    private static final String KEY_ZK_RETRY_ATTEMPTS = "zkRetryAttempts";
    private static final String KEY_ZK_RETRY_INTERVAL_MS = "zkRetryIntervalMs";
    // connection info related
    private static final String KEY_REGION = "region";
    private static final String KEY_RECORDS_LIMIT = "recordsLimit";

    private static final String KEY_MAX_UNCOMMITTED_RECORDS = "maxUncommittedRecords";

    @Override
    protected void generateComponent() {
        String spoutId = "kinesisSpout" + UUID_FOR_COMPONENTS;
        String spoutClassName = "org.apache.storm.kinesis.spout.KinesisSpout";
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, getRefYaml(addKinesisConfigComponent()));
        component = createComponent(spoutId, spoutClassName, null, constructorArgs, null);
        addParallelismToComponent();
    }

    private String addKinesisConfigComponent() {
        String componentId = "kinesisConfig" + UUID_FOR_COMPONENTS;
        String className = "org.apache.storm.kinesis.spout.KinesisConfig";
        final List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, KEY_STREAM_NAME);
        addArg(constructorArgs, KEY_SHARD_ITERATOR_TYPE);
        addArg(constructorArgs, getRefYaml(getRecordToTupleMapper()));
        addArg(constructorArgs, getRefYaml(getDate()));
        addArg(constructorArgs, getRefYaml(getFailedMessageRetryHandler()));
        addArg(constructorArgs, getRefYaml(getZkInfo()));
        addArg(constructorArgs, getRefYaml(getConnectionInfo()));
        addArg(constructorArgs, KEY_MAX_UNCOMMITTED_RECORDS, 10000);
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    private String getRecordToTupleMapper() {
        String componentId = "recordToTupleMapper" + UUID_FOR_COMPONENTS;
        String className = "com.hortonworks.streamline.streams.runtime.storm.spout.KinesisRecordToTupleMapper";
        addToComponents(createComponent(componentId, className, null, null, null));
        return componentId;
    }

    private String getDate() {
        String componentId = "date" + UUID_FOR_COMPONENTS;
        String className = "java.util.Date";
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, KEY_TIMESTAMP, System.currentTimeMillis());
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    private String getFailedMessageRetryHandler() {
        String componentId = "retryHandler" + UUID_FOR_COMPONENTS;
        String className = "org.apache.storm.kinesis.spout.ExponentialBackoffRetrier";
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, KEY_RETRY_INITIAL_DELAY_MS, 100);
        addArg(constructorArgs, KEY_RETRY_BASE_SECONDS, 2);
        addArg(constructorArgs, KEY_RETRY_MAX_TRIES, Long.MAX_VALUE);
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    private String getZkInfo() {
        String componentId = "zkInfo" + UUID_FOR_COMPONENTS;
        String className = "org.apache.storm.kinesis.spout.ZkInfo";
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, KEY_ZK_URL);
        addArg(constructorArgs, KEY_ZK_PATH);
        addArg(constructorArgs, KEY_ZK_SESSION_TIMEOUT_MS, 20000);
        addArg(constructorArgs, KEY_ZK_CONNECTION_TIMEOUT_MS, 20000);
        addArg(constructorArgs, KEY_ZK_COMMIT_INTERVAL_MS, 10000);
        addArg(constructorArgs, KEY_ZK_RETRY_ATTEMPTS, 3);
        addArg(constructorArgs, KEY_ZK_RETRY_INTERVAL_MS, 2000);
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    private String getConnectionInfo() {
        String componentId = "connectionInfo" + UUID_FOR_COMPONENTS;
        String className = "org.apache.storm.kinesis.spout.KinesisConnectionInfo";
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, getRefYaml(getCredeentialsProviderChain()));
        addArg(constructorArgs, getRefYaml(getClientConfiguration()));
        addArg(constructorArgs, KEY_REGION);
        addArg(constructorArgs, KEY_RECORDS_LIMIT, 1000);
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    private String getCredeentialsProviderChain() {
        String componentId = "credentialsProviderChain" + UUID_FOR_COMPONENTS;
        String className = "org.apache.storm.kinesis.spout.CredentialsProviderChain";
        addToComponents(createComponent(componentId, className, null, null, null));
        return componentId;
    }

    private String getClientConfiguration() {
        String componentId = "clientConfiguration" + UUID_FOR_COMPONENTS;
        String className = "com.amazonaws.ClientConfiguration";
        addToComponents(createComponent(componentId, className, null, null, null));
        return componentId;
    }

}
