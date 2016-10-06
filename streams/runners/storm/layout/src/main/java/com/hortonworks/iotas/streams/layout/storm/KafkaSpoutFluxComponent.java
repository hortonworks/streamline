package com.hortonworks.iotas.streams.layout.storm;

import com.hortonworks.iotas.streams.layout.ConfigFieldValidation;
import com.hortonworks.iotas.streams.layout.TopologyLayoutConstants;
import com.hortonworks.iotas.streams.layout.component.impl.KafkaSource;
import com.hortonworks.iotas.streams.layout.exception.BadTopologyLayoutException;
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

    private KafkaSource kafkaSource;

    public KafkaSpoutFluxComponent() {
    }

    public KafkaSpoutFluxComponent(KafkaSource kafkaSource) {
        this.kafkaSource = kafkaSource;
    }

    @Override
    protected void generateComponent () {
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
                TopologyLayoutConstants.JSON_KEY_MULTI_SCHEME_IMPL,
                TopologyLayoutConstants.JSON_KEY_IGNORE_ZK_OFFSETS,
                TopologyLayoutConstants.JSON_KEY_MAX_OFFSET_BEHIND,
                TopologyLayoutConstants.JSON_KEY_USE_START_OFFSET_IF_OFFSET_OUT_OF_RANGE,
                TopologyLayoutConstants.JSON_KEY_METRICS_TIME_BUCKET_SIZE_IN_SECS,
                TopologyLayoutConstants.JSON_KEY_ZK_SERVERS,
                TopologyLayoutConstants.JSON_KEY_ZK_PORT,
                TopologyLayoutConstants.JSON_KEY_STATE_UPDATE_INTERVAL_MS,
                TopologyLayoutConstants.JSON_KEY_RETRY_INITIAL_DELAY_MS,
                TopologyLayoutConstants.JSON_KEY_RETRY_DELAY_MULTIPLIER,
                TopologyLayoutConstants.JSON_KEY_RETRY_DELAY_MAX_MS
        };

        List propertiesYaml = getPropertiesYaml(properties);
        List spoutConfigConstructorArgs = new ArrayList();
        Map ref = getRefYaml(zkHostsRef);
        spoutConfigConstructorArgs.add(ref);
        String[] constructorArgNames = {
            TopologyLayoutConstants.JSON_KEY_TOPIC,
            TopologyLayoutConstants.JSON_KEY_ZK_ROOT,
            TopologyLayoutConstants.JSON_KEY_SPOUT_CONFIG_ID
        };
        spoutConfigConstructorArgs.addAll(getConstructorArgsYaml(constructorArgNames));
        addToComponents(createComponent(spoutConfigComponentId, spoutConfigClassName,
                propertiesYaml, spoutConfigConstructorArgs, null));

        return spoutConfigComponentId;
    }

    private String addSchemeComponent() {
        String streamsSchemeId = "streamsScheme" + UUID_FOR_COMPONENTS;
        String schemeClassName = "com.hortonworks.iotas.streams.runtime.storm.spout.StreamsKafkaSpoutScheme";
        String schemaName = (String) conf.get("topic");
        String[] constructorArgNames = {(kafkaSource != null ? kafkaSource.getId() : ""), schemaName};
        List constructorArgs = getConstructorArgsYaml(constructorArgNames);
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
    public void validateConfig () throws BadTopologyLayoutException {
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
                throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
            }
            List zkServers = (List) value;
            int listLength = zkServers.size();
            for (int i = 0; i < listLength; ++i) {
                if (!ConfigFieldValidation.isStringAndNotEmpty(zkServers.get(i))) {
                    throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
                }
            }
        }
    }

    private void validateBooleanFields () throws BadTopologyLayoutException {
        String[] optionalBooleanFields = {
            TopologyLayoutConstants.JSON_KEY_IGNORE_ZK_OFFSETS,
            TopologyLayoutConstants.JSON_KEY_USE_START_OFFSET_IF_OFFSET_OUT_OF_RANGE
        };
        validateBooleanFields(optionalBooleanFields, false);
    }

    private void validateStringFields () throws BadTopologyLayoutException {
        String[] requiredStringFields = {
            TopologyLayoutConstants.JSON_KEY_ZK_URL,
            TopologyLayoutConstants.JSON_KEY_TOPIC,
            TopologyLayoutConstants.JSON_KEY_ZK_ROOT,
            TopologyLayoutConstants.JSON_KEY_SPOUT_CONFIG_ID
        };
        validateStringFields(requiredStringFields, true);
        String[] optionalStringFields = {
            TopologyLayoutConstants.JSON_KEY_ZK_PATH
        };
        validateStringFields(optionalStringFields, false);
    }

    private void validateIntegerFields () throws BadTopologyLayoutException {
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

    private void validateLongFields () throws BadTopologyLayoutException {
        String[] optionalLongFields = {
            TopologyLayoutConstants.JSON_KEY_MAX_OFFSET_BEHIND,
            TopologyLayoutConstants.JSON_KEY_STATE_UPDATE_INTERVAL_MS,
            TopologyLayoutConstants.JSON_KEY_RETRY_INITIAL_DELAY_MS,
            TopologyLayoutConstants.JSON_KEY_RETRY_DELAY_MAX_MS
        };
        Long[] mins = {
            0l, 0l, 0l, 0l
        };
        Long[] maxes = {
            Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE
        };
        validateLongFields(optionalLongFields, false, mins, maxes);
    }

    private void validateFloatOrDoubleFields () throws BadTopologyLayoutException {
        String[] optionalFields = {
            TopologyLayoutConstants.JSON_KEY_RETRY_DELAY_MULTIPLIER
        };
        validateFloatOrDoubleFields(optionalFields, false);
    }

}
