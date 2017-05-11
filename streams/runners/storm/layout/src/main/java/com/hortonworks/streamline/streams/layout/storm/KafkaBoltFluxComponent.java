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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for KafkaBolt
 */
public class KafkaBoltFluxComponent extends AbstractFluxComponent {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaBoltFluxComponent.class);

    public KafkaBoltFluxComponent () {}

    @Override
    protected void generateComponent() {
        setSaslJaasConfig();
        String boltId = "kafkaBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.storm.kafka.bolt.KafkaBolt";
        List<Object> configMethods = new ArrayList<>();
        String[]  configMethodNames = {"setFireAndForget", "setAsync"};
        String[] configKeys = {"fireAndForget", "async"};
        configMethods.addAll(getConfigMethodsYaml(configMethodNames, configKeys));
        String[] moreConfigMethodNames = {
            "withTupleToKafkaMapper",
            "withTopicSelector",
            "withProducerProperties"
        };
        String[] configMethodArgRefs = new String[moreConfigMethodNames.length];
        configMethodArgRefs[0] = addTupleToKafkaMapper();
        configMethodArgRefs[1] = addTopicSelector();
        configMethodArgRefs[2] = addProducerProperties();
        configMethods.addAll(getConfigMethodWithRefArg(moreConfigMethodNames, configMethodArgRefs));
        component = createComponent(boltId, boltClassName, null, null, configMethods);
        addParallelismToComponent();
    }

    private String addTupleToKafkaMapper () {
        String mapperComponentId = "tupleToKafkaMapper" + UUID_FOR_COMPONENTS;
        String mapperClassName = "com.hortonworks.streamline.streams.runtime.storm.bolt.kafka.StreamlineEventToKafkaMapper";
        String[] constructorArgNames = { "keyField" };
        List<Object> constructorArgs = getConstructorArgsYaml(constructorArgNames);
        addToComponents(createComponent(mapperComponentId, mapperClassName, null, constructorArgs, null));
        return mapperComponentId;
    }

    private String addTopicSelector () {
        String topicSelectorComponentId = "topicSelector" + UUID_FOR_COMPONENTS;
        String topicSelectorClassName = "org.apache.storm.kafka.bolt.selector.DefaultTopicSelector";
        String[] constructorArgNames = { "topic" };
        List<Object> constructorArgs = getConstructorArgsYaml(constructorArgNames);
        addToComponents(createComponent(topicSelectorComponentId, topicSelectorClassName, null, constructorArgs, null));
        return topicSelectorComponentId;
    }

    private String addProducerProperties () {
        String producerPropertiesComponentId = "producerProperties" + UUID_FOR_COMPONENTS;
        String producerPropertiesClassName = "java.util.Properties";
        String methodName = "put";
        String[] specialPropertyNames = {
                "key.serializer", "value.serializer", "acks"
        };
        //fieldNames and propertyNames arrays should be of same length
        String[] propertyNames = {
            "bootstrap.servers", "buffer.memory", "compression.type", "retries", "batch.size", "client.id", "connections.max.idle.ms",
            "linger.ms", "max.block.ms", "max.request.size", "receive.buffer.bytes", "request.timeout.ms", "security.protocol", "send.buffer.bytes",
            "timeout.ms", "block.on.buffer.full", "max.in.flight.requests.per.connection", "metadata.fetch.timeout.ms", "metadata.max.age.ms",
            "reconnect.backoff.ms", "retry.backoff.ms", "schema.registry.url", "sasl.kerberos.service.name", "sasl.jaas.config","ssl.keystore.location",
            "ssl.keystore.password", "ssl.key.password", "ssl.truststore.location", "ssl.truststore.password", "ssl.enabled.protocols", "ssl.keystore.type",
            "ssl.truststore.type", "ssl.protocol", "ssl.provider", "ssl.cipher.suites", "ssl.endpoint.identification.algorithm", "ssl.keymanager.algorithm",
            "ssl.secure.random.implementation"
        };
        String[] fieldNames = {
            "bootstrapServers", "bufferMemory", "compressionType", "retries", "batchSize", "clientId", "maxConnectionIdle",
            "lingerTime", "maxBlock", "maxRequestSize", "receiveBufferSize", "requestTimeout", "securityProtocol", "sendBufferSize",
            "timeout", "blocKOnBufferFull", "maxInflighRequests", "metadataFetchTimeout", "metadataMaxAge", "reconnectBackoff", "retryBackoff",
            TopologyLayoutConstants.SCHEMA_REGISTRY_URL, KafkaSpoutFluxComponent.SASL_KERBEROS_SERVICE_NAME, KafkaSpoutFluxComponent.SASL_JAAS_CONFIG_KEY,
            "sslKeystoreLocation", "sslKeystorePassword", "sslKeyPassword", "sslTruststoreLocation", "sslTruststorePassword", "sslEnabledProtocols",
            "sslKeystoreType", "sslTruststoreType", "sslProtocol", "sslProvider", "sslCipherSuites", "sslEndpointIdAlgo", "sslKeyManagerAlgo",
            "sslSecureRandomImpl", "sslTrustManagerAlgo"
        };
        List<String> methodNames = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        methodNames.add(methodName);
        args.add(new String[]{specialPropertyNames[0], getKeySerializer()});
        methodNames.add(methodName);
        args.add(new String[]{specialPropertyNames[1], "com.hortonworks.streamline.streams.runtime.storm.bolt.kafka.StreamlineEventSerializer"});
        methodNames.add(methodName);
        args.add(new String[]{specialPropertyNames[2], getAckMode()});
        for (int j = 0; j < propertyNames.length; ++j) {
            if (conf.get(fieldNames[j]) != null) {
                methodNames.add(methodName);
                args.add(new Object[]{propertyNames[j], conf.get(fieldNames[j])});
            }
        }
        addToComponents(createComponent(producerPropertiesComponentId, producerPropertiesClassName, null, null, getConfigMethodsYaml(methodNames.toArray(new String[methodNames.size()]), args.toArray())));
        return producerPropertiesComponentId;
    }

    private String getKeySerializer () {
        String keySerializer = (String) conf.get("keySerializer");
        if ("String".equals(keySerializer)) {
            return "org.apache.kafka.common.serialization.StringSerializer";
        } else if ("Integer".equals(keySerializer)) {
            return "org.apache.kafka.common.serialization.IntegerSerializer";
        } else if ("Long".equals(keySerializer)) {
            return "org.apache.kafka.common.serialization.LongSerializer";
        } else if ("ByteArray".equals(keySerializer)) {
            return "org.apache.kafka.common.serialization.ByteArraySerializer";
        } else {
            throw new IllegalArgumentException("Key serializer for kafka sink is not supported: " + keySerializer);
        }
    }

    private String getAckMode () {
        String ackMode = (String) conf.get("ackMode");
        if ("None".equals(ackMode)) {
            return "0";
        } else if ("Leader".equals(ackMode) || (ackMode == null)) {
            return "1";
        } else if ("All".equals(ackMode)) {
            return "all";
        } else {
            throw new IllegalArgumentException("Ack mode for kafka sink is not supported: " + ackMode);
        }
    }

    private void setSaslJaasConfig () {
        String securityProtocol = (String) conf.get("securityProtocol");
        if (securityProtocol != null && !securityProtocol.isEmpty() && securityProtocol.startsWith("SASL")) {
            StringBuilder saslConfigStrBuilder = new StringBuilder();
            String kafkaServiceName = (String) conf.get(KafkaSpoutFluxComponent.SASL_KERBEROS_SERVICE_NAME);
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
            saslConfigStrBuilder.append("com.sun.security.auth.module.Krb5LoginModule required \\ useKeyTab=true \\ storeKey=true \\ keyTab=\"");
            saslConfigStrBuilder.append(keytab).append("\" \\ principal=\"").append(principal).append("\";");
            conf.put(KafkaSpoutFluxComponent.SASL_JAAS_CONFIG_KEY, saslConfigStrBuilder.toString());
        }
    }
}
