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

import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation for KafkaSpout
 */
public class KafkaSpoutFluxComponent extends AbstractFluxComponent {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaSpoutFluxComponent.class);
    static final String SASL_JAAS_CONFIG_KEY = "saslJaasConfig";
    static final String SASL_KERBEROS_SERVICE_NAME = "kafkaServiceName";
    private KafkaSource kafkaSource;

    // for unit tests
    public KafkaSpoutFluxComponent () {
    }

    @Override
    protected void generateComponent () {
        kafkaSource = (KafkaSource) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        // add the output stream to conf so that the kafka spout declares output stream properly
        if (kafkaSource != null && kafkaSource.getOutputStreams().size() == 1) {
            conf.put(TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAM_ID,
                    kafkaSource.getOutputStreams().iterator().next().getId());
        } else {
            String msg = "Kafka source component [" + kafkaSource + "] should define exactly one output stream for Storm";
            LOG.error(msg, kafkaSource);
            throw new IllegalArgumentException(msg);
        }
        validateSSLConfig();
        setSaslJaasConfig();
        String spoutConfigRef = addKafkaSpoutConfigComponent();
        String spoutId = "kafkaSpout" + UUID_FOR_COMPONENTS;
        String spoutClassName = "org.apache.storm.kafka.spout.KafkaSpout";
        List<Map<String, String>> spoutConstructorArgs = new ArrayList<>();
        Map<String, String> ref = getRefYaml(spoutConfigRef);
        spoutConstructorArgs.add(ref);
        component = createComponent(spoutId, spoutClassName, null, spoutConstructorArgs, null);
        addParallelismToComponent();
    }

    private String addKafkaSpoutConfigComponent () {
        String kafkaSpoutConfigComponentId = "kafkaSpoutConfig" + UUID_FOR_COMPONENTS;
        String kafkaSpoutConfigClassName = "org.apache.storm.kafka.spout.KafkaSpoutConfig";
        String kafkaSpoutConfigBuilderRef = addKafkaSpoutConfigBuilder();
        List<Object> spoutConfigConstructorArgs = new ArrayList<Object>();
        Map<String, String> ref = getRefYaml(kafkaSpoutConfigBuilderRef);
        spoutConfigConstructorArgs.add(ref);
        addToComponents(createComponent(kafkaSpoutConfigComponentId, kafkaSpoutConfigClassName, null, spoutConfigConstructorArgs, null));
        return kafkaSpoutConfigComponentId;
    }



    // Add KafkaSpoutConfig$Builder yaml component and return its yaml id to further use
    // it as a ref
    private String addKafkaSpoutConfigBuilder() {
        String kafkaSpoutConfigBuilderId = "kafkaSpoutConfigBuilder" + UUID_FOR_COMPONENTS;
        String kafkaSpoutConfigBuilderClassName = "org.apache.storm.kafka.spout.KafkaSpoutConfig$Builder";

        //constructor args
        String[] constructorArgNames = { "bootstrapServers", TopologyLayoutConstants.JSON_KEY_TOPIC };
        List<Object> constructorArgs = new ArrayList<>();
        List bootstrapServersAndTopic = getConstructorArgsYaml (constructorArgNames);
        constructorArgs.add(bootstrapServersAndTopic.get(0));
        constructorArgs.add(new String[] {(String) bootstrapServersAndTopic.get(1)});

        List<Object> configMethods = new ArrayList<>();
        String[] configMethodNames = {
                "setPollTimeoutMs", "setOffsetCommitPeriodMs", "setMaxUncommittedOffsets",
                "setFirstPollOffsetStrategy", "setPartitionRefreshPeriodMs", "setEmitNullTuples", "setConsumerStartDelayMs"
        };
        String[] configKeys = {
                "pollTimeoutMs", "offsetCommitPeriodMs", "maximumUncommittedOffsets",
                "firstPollOffsetStrategy", "partitionRefreshPeriodMs", "emitNullTuples", "consumerStartupDelayMs"
        };
        configMethods.addAll(getConfigMethodsYaml(configMethodNames, configKeys));
        String[] moreConfigMethodNames = {
            "setRetry", "setRecordTranslator", "setProp"
        };
        String[] configMethodArgRefs = new String[moreConfigMethodNames.length];
        configMethodArgRefs[0] = addRetry();
        configMethodArgRefs[1] = addRecordTranslator();
        configMethodArgRefs[2] = addConsumerProperties();
        configMethods.addAll(getConfigMethodWithRefArg(moreConfigMethodNames, configMethodArgRefs));
        this.addToComponents(this.createComponent(kafkaSpoutConfigBuilderId, kafkaSpoutConfigBuilderClassName, null, constructorArgs, configMethods));
        return kafkaSpoutConfigBuilderId;
    }

    private String addRetry () {
        String componentId = "kafkaSpoutRetryService" + UUID_FOR_COMPONENTS;
        String className = "org.apache.storm.kafka.spout.KafkaSpoutRetryExponentialBackoff";
        List<Object> constructorArgs = new ArrayList<>();
        String retryInitialDelayMsId = "retryInitialDelayMs" + UUID_FOR_COMPONENTS;
        addTimeInterval(retryInitialDelayMsId, conf.get("retryInitialDelayMs") != null ? conf.get("retryInitialDelayMs") : new Long(0));
        constructorArgs.add(getRefYaml(retryInitialDelayMsId));
        String retryDelayPeriodMsId = "retryDelayPeriodMs" + UUID_FOR_COMPONENTS;
        addTimeInterval(retryDelayPeriodMsId, conf.get("retryDelayPeriodMs") != null ? conf.get("retryDelayPeriodMs") : new Long(2));
        constructorArgs.add(getRefYaml(retryDelayPeriodMsId));
        constructorArgs.add(conf.get("maximumRetries") != null ? conf.get("maximumRetries") : Integer.MAX_VALUE);
        String retryDelayMaximumMs = "retryDelayMaximumMs" + UUID_FOR_COMPONENTS;
        addTimeInterval(retryDelayMaximumMs, conf.get("retryDelayMaximumMs") != null ? conf.get("retryDelayMaximumMs") : new Long(10000));
        constructorArgs.add(getRefYaml(retryDelayMaximumMs));
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    private void addTimeInterval (String componentId, Object value) {
        String className = "org.apache.storm.kafka.spout.KafkaSpoutRetryExponentialBackoff$TimeInterval";
        List<Object> constructorArgs = new ArrayList<>();
        constructorArgs.add(value);
        constructorArgs.add("MILLISECONDS");
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
    }

    private String addRecordTranslator () {
        String translatorId = "avroKafkaSpoutTranslator" + UUID_FOR_COMPONENTS;
        String translatorClassname = "com.hortonworks.streamline.streams.runtime.storm.spout.AvroKafkaSpoutTranslator";
        List<Object> constructorArgs = new ArrayList<>();
        constructorArgs.addAll(getConstructorArgsYaml(new String[]{TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAM_ID, TopologyLayoutConstants
                .JSON_KEY_TOPIC}));
        constructorArgs.add(kafkaSource != null ? kafkaSource.getId() : "");
        constructorArgs.add(conf.get(TopologyLayoutConstants.SCHEMA_REGISTRY_URL));

        // add readerSchemaVersion to constructor arg only when it's not null and not empty
        String readerSchemaVersion = (String) conf.get("readerSchemaVersion");
        if(readerSchemaVersion != null && !readerSchemaVersion.isEmpty()) {
            constructorArgs.add(Integer.parseInt(readerSchemaVersion));
        }
        addToComponents(createComponent(translatorId, translatorClassname, null, constructorArgs, null));
        return translatorId;
    }

    private String addConsumerProperties () {
        String consumerPropertiesComponentId = "consumerProperties" + UUID_FOR_COMPONENTS;
        String consumerPropertiesClassName = "java.util.Properties";
        String methodName = "put";
        String[] specialPropertyNames = { "key.deserializer", "value.deserializer" };
        //fieldNames and propertyNames arrays should be of same length
        String[] propertyNames = {
                "group.id", "fetch.min.bytes", "max.partition.fetch.bytes", "max.poll.records", "security.protocol", "sasl.kerberos.service.name",
                "sasl.jaas.config", "ssl.keystore.location", "ssl.keystore.password", "ssl.key.password", "ssl.truststore.location", "ssl.truststore.password",
                "ssl.enabled.protocols", "ssl.keystore.type", "ssl.truststore.type", "ssl.protocol", "ssl.provider", "ssl.cipher.suites",
                "ssl.endpoint.identification.algorithm", "ssl.keymanager.algorithm", "ssl.secure.random.implementation", "ssl.trustmanager.algorithm"
        };
        String[] fieldNames = {
                "consumerGroupId", "fetchMinimumBytes", "fetchMaximumBytesPerPartition", "maxRecordsPerPoll",  "securityProtocol", SASL_KERBEROS_SERVICE_NAME,
                SASL_JAAS_CONFIG_KEY, "sslKeystoreLocation", "sslKeystorePassword", "sslKeyPassword", "sslTruststoreLocation", "sslTruststorePassword",
                "sslEnabledProtocols", "sslKeystoreType", "sslTruststoreType", "sslProtocol", "sslProvider", "sslCipherSuites", "sslEndpointIdAlgo",
                "sslKeyManagerAlgo", "sslSecureRandomImpl", "sslTrustManagerAlgo"
        };
        List<String> methodNames = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        methodNames.add(methodName);
        args.add(new String[]{specialPropertyNames[0], "org.apache.kafka.common.serialization.ByteArrayDeserializer"});
        methodNames.add(methodName);
        args.add(new String[]{specialPropertyNames[1], "org.apache.kafka.common.serialization.ByteBufferDeserializer"});
        for (int j = 0; j < propertyNames.length; ++j) {
            if (conf.get(fieldNames[j]) != null) {
                methodNames.add(methodName);
                args.add(new Object[]{propertyNames[j], conf.get(fieldNames[j])});
            }
        }
        addToComponents(createComponent(consumerPropertiesComponentId, consumerPropertiesClassName, null, null,
                getConfigMethodsYaml(methodNames.toArray(new String[methodNames.size()]), args.toArray())));
        return consumerPropertiesComponentId;
    }

    private void setSaslJaasConfig () {
        String securityProtocol = (String) conf.get("securityProtocol");
        if (securityProtocol != null && !securityProtocol.isEmpty() && securityProtocol.startsWith("SASL")) {
            StringBuilder saslConfigStrBuilder = new StringBuilder();
            String kafkaServiceName = (String) conf.get(SASL_KERBEROS_SERVICE_NAME);
            String principal = (String) conf.get("principal");
            String keytab = (String) conf.get("keytab");
            if (kafkaServiceName == null || kafkaServiceName.isEmpty()) {
                throw new IllegalArgumentException("Kafka service name must be provided for SASL GSSAPI Kerberos");
            }
            if (principal == null || principal.isEmpty()) {
                throw new IllegalArgumentException("Kafka client principal must be provided for SASL GSSAPI Kerberos");
            }
            if (keytab == null || keytab.isEmpty()) {
                throw new IllegalArgumentException("Kafka client principal keytab must be provided for SASL GSSAPI Kerberos");
            }
            saslConfigStrBuilder.append("com.sun.security.auth.module.Krb5LoginModule required useKeyTab=true storeKey=true keyTab=\"");
            saslConfigStrBuilder.append(keytab).append("\"  principal=\"").append(principal).append("\";");
            conf.put(SASL_JAAS_CONFIG_KEY, saslConfigStrBuilder.toString());
        }
    }

    private void validateSSLConfig () {
        String securityProtocol = (String) conf.get("securityProtocol");
        if (securityProtocol != null && !securityProtocol.isEmpty() && securityProtocol.endsWith("SSL")) {
            String truststoreLocation = (String) conf.get("sslTruststoreLocation");
            String truststorePassword = (String) conf.get("sslTruststorePassword");
            if (truststoreLocation == null || truststoreLocation.isEmpty()) {
                throw new IllegalArgumentException("Truststore location must be provided for SSL");
            }
            if (truststorePassword == null || truststorePassword.isEmpty()) {
                throw new IllegalArgumentException("Truststore password must be provided for SSL");
            }
        }
    }
}
