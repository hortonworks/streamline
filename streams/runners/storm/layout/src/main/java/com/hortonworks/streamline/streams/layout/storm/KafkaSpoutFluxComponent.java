package com.hortonworks.streamline.streams.layout.storm;

import com.google.common.collect.Lists;
import com.hortonworks.streamline.streams.layout.ConfigFieldValidation;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hortonworks.streamline.streams.layout.exception.ComponentConfigException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation for KafkaSpout
 */
public class KafkaSpoutFluxComponent extends AbstractFluxComponent {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaSpoutFluxComponent.class);
    private KafkaSource kafkaSource;
    // for unit tests
    public KafkaSpoutFluxComponent() {
    }

    @Override
    protected void generateComponent () {
        kafkaSource = (KafkaSource) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        String spoutConfigRef = addSpoutConfigComponent();
        String spoutId = "kafkaSpout" + UUID_FOR_COMPONENTS;
        String spoutClassName = "org.apache.storm.kafka.KafkaSpout";
        List spoutConstructorArgs = new ArrayList();
        Map ref = getRefYaml(spoutConfigRef);
        spoutConstructorArgs.add(ref);
        component = createComponent(spoutId, spoutClassName, null, spoutConstructorArgs, null);
        addParallelismToComponent();
    }

    private String addSpoutConfigComponent () {
        String zkHostsRef = addBrokerHostsComponent();
        String schemeRef = addSchemeComponent();
        String spoutConfigComponentId = "spoutConfig" + UUID_FOR_COMPONENTS;
        String spoutConfigClassName = "org.apache.storm.kafka.SpoutConfig";
        String[] properties = {
                TopologyLayoutConstants.JSON_KEY_FETCH_SIZE_BYTES,
                TopologyLayoutConstants.JSON_KEY_SOCKET_TIMEOUT_MS,
                TopologyLayoutConstants.JSON_KEY_FETCH_MAX_WAIT,
                TopologyLayoutConstants.JSON_KEY_BUFFER_SIZE_BYTES,
                //ignore multi scheme impl for now. always use default
                // RawScheme. add check in validation
//                TopologyLayoutConstants.JSON_KEY_MULTI_SCHEME_IMPL,
                TopologyLayoutConstants.JSON_KEY_IGNORE_ZK_OFFSETS,
                TopologyLayoutConstants.JSON_KEY_MAX_OFFSET_BEHIND,
                TopologyLayoutConstants.JSON_KEY_USE_START_OFFSET_IF_OFFSET_OUT_OF_RANGE,
                TopologyLayoutConstants.JSON_KEY_METRICS_TIME_BUCKET_SIZE_IN_SECS,
                TopologyLayoutConstants.JSON_KEY_ZK_SERVERS,
                TopologyLayoutConstants.JSON_KEY_ZK_PORT,
                TopologyLayoutConstants.JSON_KEY_STATE_UPDATE_INTERVAL_MS,
                TopologyLayoutConstants.JSON_KEY_RETRY_INITIAL_DELAY_MS,
                TopologyLayoutConstants.JSON_KEY_RETRY_DELAY_MULTIPLIER,
                TopologyLayoutConstants.JSON_KEY_RETRY_DELAY_MAX_MS,
                TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAM_ID
        };
        // add the output stream to conf so that the kafka spout declares output stream properly
        if (kafkaSource != null && kafkaSource.getOutputStreams().size() == 1) {
            conf.put(TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAM_ID,
                    kafkaSource.getOutputStreams().iterator().next().getId());
        } else {
            String msg = "Kafka source component [" + kafkaSource + "] should define exactly one output stream for Storm";
            LOG.error(msg, kafkaSource);
            throw new IllegalArgumentException(msg);
        }
        List propertiesYaml = getPropertiesYaml(properties);

        propertiesYaml.add(getSchemeRefEntry(schemeRef));

        List spoutConfigConstructorArgs = new ArrayList();
        Map ref = getRefYaml(zkHostsRef);
        spoutConfigConstructorArgs.add(ref);
        Object[] constructorArgs = {
            conf.get(TopologyLayoutConstants.JSON_KEY_TOPIC),
            TopologyLayoutConstants.ZK_ROOT_NODE,
            conf.get(TopologyLayoutConstants.JSON_KEY_CONSUMER_GROUP_ID)
        };
        spoutConfigConstructorArgs.addAll(getConstructorArgsYaml(constructorArgs));
        addToComponents(createComponent(spoutConfigComponentId, spoutConfigClassName,
                propertiesYaml, spoutConfigConstructorArgs, null));

        return spoutConfigComponentId;
    }

    private Map<String, Object> getSchemeRefEntry(String schemeRef) {
        LinkedHashMap<String, Object> pair = new LinkedHashMap<>();
        pair.put(StormTopologyLayoutConstants.YAML_KEY_NAME, TopologyLayoutConstants.JSON_KEY_MULTI_SCHEME_IMPL);
        pair.put(StormTopologyLayoutConstants.YAML_KEY_REF, schemeRef);
        return pair;
    }

    private String addSchemeComponent() {
        String streamsSchemeId = "streamsScheme-" + UUID_FOR_COMPONENTS;
        String schemeClassName = "com.hortonworks.streamline.streams.runtime.storm.spout.AvroKafkaSpoutScheme";
        String topicName = (String) conf.get(TopologyLayoutConstants.JSON_KEY_TOPIC);
        String schemaRegistryUrl = (String) conf.get(TopologyLayoutConstants.SCHEMA_REGISTRY_URL);
        List<String> constructorArgs = Lists.newArrayList((kafkaSource != null ? kafkaSource.getId() : ""), topicName, schemaRegistryUrl);
        addToComponents(createComponent(streamsSchemeId, schemeClassName, null, constructorArgs, null));

        return streamsSchemeId;
    }

    // Add BrokerHosts yaml component and return its yaml id to further use
    // it as a ref
    private String addBrokerHostsComponent () {
        String zkHostsComponentId = "zkHosts" + UUID_FOR_COMPONENTS;

        // currently only BrokerHosts is supported.
        String zkHostsClassName = "org.apache.storm.kafka.ZkHosts";

        String[] propertyNames = {TopologyLayoutConstants
                .JSON_KEY_REFRESH_FREQ_SECS};
        //properties
        List properties = getPropertiesYaml(propertyNames);

        //constructor args
        String[] constructorArgNames = {
            TopologyLayoutConstants.JSON_KEY_ZK_URL,
            TopologyLayoutConstants.JSON_KEY_ZK_PATH
        };
        List zkHostsConstructorArgs = getConstructorArgsYaml
                (constructorArgNames);

        this.addToComponents(this.createComponent(zkHostsComponentId,
                zkHostsClassName, properties, zkHostsConstructorArgs, null));
        return zkHostsComponentId;
    }

    @Override
    public void validateConfig () throws ComponentConfigException {
        super.validateConfig();
        validateBooleanFields();
        validateStringFields();
        validateIntegerFields();
        validateLongFields();
        validateFloatOrDoubleFields();
        String fieldName = TopologyLayoutConstants.JSON_KEY_ZK_SERVERS;
        Object value = conf.get(fieldName);
        if (value != null) {
            if (!ConfigFieldValidation.isList(value)) {
                throw new ComponentConfigException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
            }
            List zkServers = (List) value;
            int listLength = zkServers.size();
            for (Object zkServer : zkServers) {
                if (!ConfigFieldValidation.isStringAndNotEmpty(zkServer)) {
                    throw new ComponentConfigException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
                }
            }
        }
    }

    private void validateBooleanFields () throws ComponentConfigException {
        String[] optionalBooleanFields = {
            TopologyLayoutConstants.JSON_KEY_IGNORE_ZK_OFFSETS,
            TopologyLayoutConstants.JSON_KEY_USE_START_OFFSET_IF_OFFSET_OUT_OF_RANGE
        };
        validateBooleanFields(optionalBooleanFields, false);
    }

    private void validateStringFields () throws ComponentConfigException {
        String[] requiredStringFields = {
            TopologyLayoutConstants.JSON_KEY_ZK_URL,
            TopologyLayoutConstants.JSON_KEY_TOPIC,
            TopologyLayoutConstants.JSON_KEY_CONSUMER_GROUP_ID
        };
        validateStringFields(requiredStringFields, true);
        String[] optionalStringFields = {
            TopologyLayoutConstants.JSON_KEY_ZK_PATH
        };
        validateStringFields(optionalStringFields, false);
    }

    private void validateIntegerFields () throws ComponentConfigException {
        String[] optionalIntegerFields = {
            TopologyLayoutConstants.JSON_KEY_REFRESH_FREQ_SECS,
            TopologyLayoutConstants.JSON_KEY_FETCH_SIZE_BYTES,
            TopologyLayoutConstants.JSON_KEY_SOCKET_TIMEOUT_MS,
            TopologyLayoutConstants.JSON_KEY_FETCH_MAX_WAIT,
            TopologyLayoutConstants.JSON_KEY_BUFFER_SIZE_BYTES,
            TopologyLayoutConstants.JSON_KEY_METRICS_TIME_BUCKET_SIZE_IN_SECS,
            TopologyLayoutConstants.JSON_KEY_ZK_PORT
        };
        Integer[] mins = {
            0, 0, 0, 0, 0, 0, 1025
        };
        Integer[] maxes = {
            Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer
                .MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 65536
        };
        validateIntegerFields(optionalIntegerFields, false, mins, maxes);
    }

    private void validateLongFields () throws ComponentConfigException {
        String[] optionalLongFields = {
            TopologyLayoutConstants.JSON_KEY_MAX_OFFSET_BEHIND,
            TopologyLayoutConstants.JSON_KEY_STATE_UPDATE_INTERVAL_MS,
            TopologyLayoutConstants.JSON_KEY_RETRY_INITIAL_DELAY_MS,
            TopologyLayoutConstants.JSON_KEY_RETRY_DELAY_MAX_MS
        };
        Long[] mins = {
                0L, 0L, 0L, 0L
        };
        Long[] maxes = {
            Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE
        };
        validateLongFields(optionalLongFields, false, mins, maxes);
    }

    private void validateFloatOrDoubleFields () throws ComponentConfigException {
        String[] optionalFields = {
            TopologyLayoutConstants.JSON_KEY_RETRY_DELAY_MULTIPLIER
        };
        validateFloatOrDoubleFields(optionalFields, false);
    }

}
