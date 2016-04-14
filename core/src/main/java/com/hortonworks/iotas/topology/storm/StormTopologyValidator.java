package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.topology.ConfigFieldValidation;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.CoreUtils;
import com.hortonworks.iotas.util.ReflectionHelper;
import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A storm topology validator that takes a topology json created by the UI
 * and validates different configurations for different components in the
 * topology and the connections between them
 */
class StormTopologyValidator {
    private static final Logger LOG = LoggerFactory.getLogger(StormTopologyValidator.class);
    private final Map topologyConfig;
    private final String catalogRootUrl;
    StormTopologyValidator (Map topologyConfig, String catalogRootUrl) {
        this.topologyConfig = topologyConfig;
        this.catalogRootUrl = catalogRootUrl;
    }

    void validate () throws Exception {
        String[] componentKeys = {
                TopologyLayoutConstants.JSON_KEY_DATA_SOURCES,
                TopologyLayoutConstants.JSON_KEY_DATA_SINKS,
                TopologyLayoutConstants.JSON_KEY_PROCESSORS,
                TopologyLayoutConstants.JSON_KEY_LINKS
        };
        for (String componentKey: componentKeys) {
            List<Map> components = (List<Map>) this.topologyConfig.get(componentKey);
            for (Map component: components) {
                String transformationClass = (String) component.get(TopologyLayoutConstants.JSON_KEY_TRANSFORMATION_CLASS);
                Map<String, Object> config = (Map<String, Object>) component.get(TopologyLayoutConstants.JSON_KEY_CONFIG);
                FluxComponent fluxComponent = ReflectionHelper.newInstance(transformationClass);
                fluxComponent.withConfig(config);
                fluxComponent.withCatalogRootUrl(catalogRootUrl);
                fluxComponent.validateConfig();
            }
        }
        validateParserProcessorLinks();
        // Uncomment this with https://hwxiot.atlassian.net/browse/IOT-126
        //validateRuleProcessorLinks();
        validateCustomProcessorLinks();
    }

    // if there is a link from a parser processor then the stream id has to
    // be present and it has to be one of the two streams - parsed or failed
    void validateParserProcessorLinks () throws BadTopologyLayoutException {
        List<Map> dataSources = (List) this.topologyConfig.get(TopologyLayoutConstants.JSON_KEY_DATA_SOURCES);
        Set<String> dataSourceNames = new HashSet<String>();
        for (Map dataSource: dataSources) {
            dataSourceNames.add((String) dataSource.get(TopologyLayoutConstants.JSON_KEY_UINAME));
        }
        List<Map> processors = (List) this.topologyConfig.get(TopologyLayoutConstants.JSON_KEY_PROCESSORS);
        Map<String, Map> parserProcessors = new LinkedHashMap<String, Map>();
        for (Map processor: processors) {
            String type = (String) processor.get(TopologyLayoutConstants.JSON_KEY_TYPE);
            if ("PARSER".equals(type)) {
                parserProcessors.put((String) processor.get(TopologyLayoutConstants.JSON_KEY_UINAME), (Map) processor.get(TopologyLayoutConstants.JSON_KEY_CONFIG));
            }
        }
        Set<String> parserProcessorKeys = parserProcessors.keySet();
        if (parserProcessorKeys.size() == 0) {
            throw new BadTopologyLayoutException(TopologyLayoutConstants.ERR_MSG_NO_PARSER_PROCESSOR);
        }
        List<Map> links = (List) this.topologyConfig.get(TopologyLayoutConstants.JSON_KEY_LINKS);
        for (Map link: links) {
            Map linkConfig = (Map) link.get(TopologyLayoutConstants.JSON_KEY_CONFIG);
            String from = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_FROM);
            String to = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_TO);
            if (parserProcessorKeys.contains(from)) {
                Map processor = parserProcessors.get(from);
                List<String> processorStreams = new ArrayList<String>();
                String parsedTuplesStream = (String) processor.get(TopologyLayoutConstants.JSON_KEY_PARSED_TUPLES_STREAM);
                processorStreams.add(parsedTuplesStream);
                String failedTuplesStream = (String) processor.get(TopologyLayoutConstants.JSON_KEY_FAILED_TUPLES_STREAM);
                if (failedTuplesStream != null) {
                    processorStreams.add(failedTuplesStream);
                }
                String streamId = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_STREAM_ID);
                if (StringUtils.isEmpty(streamId) || !processorStreams.contains(streamId)) {
                    throw new BadTopologyLayoutException (String.format(TopologyLayoutConstants.ERR_MSG_INVALID_STREAM_ID, link.get(TopologyLayoutConstants.JSON_KEY_UINAME)));
                }
            }
            if (parserProcessorKeys.contains(to)) {
                // link to a parser processor can only go from a data source
                if (!dataSourceNames.contains(from)) {
                    throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_INVALID_LINK_TO_PARSER, link.get(TopologyLayoutConstants.JSON_KEY_UINAME)));
                }
            }
        }
    }

    void validateRuleProcessorLinks () throws BadTopologyLayoutException {
        List<Map> dataSources = (List) this.topologyConfig.get(TopologyLayoutConstants.JSON_KEY_DATA_SOURCES);
        Set<String> dataSourceNames = new HashSet<String>();
        for (Map dataSource: dataSources) {
            dataSourceNames.add((String) dataSource.get(TopologyLayoutConstants.JSON_KEY_UINAME));
        }
        List<Map> processors = (List) this.topologyConfig.get(TopologyLayoutConstants.JSON_KEY_PROCESSORS);
        Map<String, Map> ruleProcessors = new LinkedHashMap<String, Map>();
        for (Map processor: processors) {
            String type = (String) processor.get(TopologyLayoutConstants.JSON_KEY_TYPE);
            if ("RULE".equals(type)) {
                Map config = (Map) processor.get(TopologyLayoutConstants.JSON_KEY_CONFIG);
                Map ruleProcessorConfig = (Map) config.get(TopologyLayoutConstants.JSON_KEY_RULES_PROCESSOR_CONFIG);
                Map<String, Set<String>> ruleInfo = this.getRuleProcessorStreamIdsToOutputFields(ruleProcessorConfig);
                ruleProcessors.put((String) processor.get(TopologyLayoutConstants.JSON_KEY_UINAME), ruleInfo);
            }
        }
        Set<String> ruleProcessorKeys = ruleProcessors.keySet();
        List<Map> links = (List) this.topologyConfig.get(TopologyLayoutConstants.JSON_KEY_LINKS);
        for (Map link: links) {
            Map linkConfig = (Map) link.get(TopologyLayoutConstants.JSON_KEY_CONFIG);
            String from = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_FROM);
            String to = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_TO);
            if (ruleProcessorKeys.contains(from)) {
                String streamId = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_STREAM_ID);
                if (StringUtils.isEmpty(streamId)) {
                    throw new BadTopologyLayoutException (String.format(TopologyLayoutConstants.ERR_MSG_INVALID_STREAM_ID, link.get(TopologyLayoutConstants.JSON_KEY_UINAME)));
                }
                Map<String, Set<String>> streamIdToOutput = ruleProcessors.get(from);
                Set<String> ruleStreams = streamIdToOutput.keySet();
                if (!ruleStreams.contains(streamId)) {
                    throw new BadTopologyLayoutException (String.format(TopologyLayoutConstants.ERR_MSG_INVALID_STREAM_ID, link.get(TopologyLayoutConstants.JSON_KEY_UINAME)));
                }
                if ("FIELDS".equals((String) link.get(TopologyLayoutConstants.JSON_KEY_TYPE))) {
                    Set<String> outputFields = streamIdToOutput.get(streamId);
                    List<String> groupingFields = (List) linkConfig.get(TopologyLayoutConstants.JSON_KEY_GROUPING_FIELDS);
                    if (!outputFields.containsAll(groupingFields)) {
                        throw new BadTopologyLayoutException (String.format(TopologyLayoutConstants.ERR_MSG_INVALID_GROUPING_FIELDS, link.get(TopologyLayoutConstants.JSON_KEY_UINAME)));
                    }
                }
            }
            if (ruleProcessorKeys.contains(to)) {
                // link to a rule processor can not go from a data source
                if (dataSourceNames.contains(from)) {
                    throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_INVALID_LINK_TO_PROCESSOR, link.get(TopologyLayoutConstants.JSON_KEY_UINAME)));
                }
            }
        }
    }

    void validateCustomProcessorLinks () throws BadTopologyLayoutException {
        List<Map> dataSources = (List) this.topologyConfig.get(TopologyLayoutConstants.JSON_KEY_DATA_SOURCES);
        Set<String> dataSourceNames = new HashSet<String>();
        for (Map dataSource: dataSources) {
            dataSourceNames.add((String) dataSource.get(TopologyLayoutConstants.JSON_KEY_UINAME));
        }
        List<Map> processors = (List) this.topologyConfig.get(TopologyLayoutConstants.JSON_KEY_PROCESSORS);
        Map<String, Schema> inputSchemas = new LinkedHashMap<String, Schema>();
        Map<String, Map<String, Schema>> outputSchemas = new LinkedHashMap<String, Map<String, Schema>>();
        for (Map processor: processors) {
            String type = (String) processor.get(TopologyLayoutConstants.JSON_KEY_TYPE);
            if ("CUSTOM".equals(type)) {
                Map config = (Map) processor.get(TopologyLayoutConstants.JSON_KEY_CONFIG);
                try {
                    inputSchemas.put((String) processor.get(TopologyLayoutConstants.JSON_KEY_UINAME), getCustomProcessorInputSchema(config));
                    outputSchemas.put((String) processor.get(TopologyLayoutConstants.JSON_KEY_UINAME), getCustomProcessorOutputSchema(config));
                } catch (IOException e) {
                    String message = "Invalid custom processor input or output schema config.";
                    LOG.error(message);
                    throw new BadTopologyLayoutException(message, e);
                }
            }
        }
        Set<String> customProcessorKeys = outputSchemas.keySet();
        List<Map> links = (List) this.topologyConfig.get(TopologyLayoutConstants.JSON_KEY_LINKS);
        for (Map link: links) {
            Map linkConfig = (Map) link.get(TopologyLayoutConstants.JSON_KEY_CONFIG);
            String from = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_FROM);
            String to = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_TO);
            if (customProcessorKeys.contains(from)) {
                String streamId = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_STREAM_ID);
                if (StringUtils.isEmpty(streamId)) {
                    throw new BadTopologyLayoutException (String.format(TopologyLayoutConstants.ERR_MSG_INVALID_STREAM_ID, link.get(TopologyLayoutConstants.JSON_KEY_UINAME)));
                }
                Map<String, Schema> streamIdToOutput = outputSchemas.get(from);
                Set<String> outputStreams = streamIdToOutput.keySet();
                if (!outputStreams.contains(streamId)) {
                    throw new BadTopologyLayoutException (String.format(TopologyLayoutConstants.ERR_MSG_INVALID_STREAM_ID, link.get(TopologyLayoutConstants.JSON_KEY_UINAME)));
                }
                if ("FIELDS".equals((String) link.get(TopologyLayoutConstants.JSON_KEY_TYPE))) {
                    Set<String> outputFields = getTopLevelFieldNamesFromSchema(streamIdToOutput.get(streamId));
                    List<String> groupingFields = (List) linkConfig.get(TopologyLayoutConstants.JSON_KEY_GROUPING_FIELDS);
                    if (!outputFields.containsAll(groupingFields)) {
                        throw new BadTopologyLayoutException (String.format(TopologyLayoutConstants.ERR_MSG_INVALID_GROUPING_FIELDS, link.get(TopologyLayoutConstants.JSON_KEY_UINAME)));
                    }
                }
            }
            if (customProcessorKeys.contains(to)) {
                // link to a custom processor can not go from a data source
                if (dataSourceNames.contains(from)) {
                    throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_INVALID_LINK_TO_PROCESSOR, link.get(TopologyLayoutConstants.JSON_KEY_UINAME)));
                }
            }
        }
    }

    // For a rule processor config object containing rules, this objects
    // returns a map of streamids(one per rule) and output fields for each
    // such stream. This is used in validation in validateRuleProcessorLinks
    // method above
    private Map<String, Set<String>> getRuleProcessorStreamIdsToOutputFields
            (Map ruleProcessorConfig) {
        Map<String, Set<String>> result = new LinkedHashMap<String, Set<String>>();
        String processorName = (String) ruleProcessorConfig.get(TopologyLayoutConstants.JSON_KEY_NAME);
        List<Map> rules = (List) ruleProcessorConfig.get(TopologyLayoutConstants.JSON_KEY_RULES);
        for (Map rule: rules) {
            long ruleId = 0l;
            Object ruleIdO = rule.get(TopologyLayoutConstants.JSON_KEY_ID);
            if (ConfigFieldValidation.isInteger(ruleIdO)) {
                ruleId = (Integer) ruleIdO;
            } else if (ConfigFieldValidation.isLong(ruleIdO)) {
                ruleId = (Long) ruleIdO;
            }
            String ruleName = (String) rule.get(TopologyLayoutConstants.JSON_KEY_NAME);
            Set<String> outputFields = new HashSet<String>();
            Map action = (Map) rule.get(TopologyLayoutConstants.JSON_KEY_RULE_ACTIONS);
            List<Map> declaredOutputs = (List<Map>) action.get(TopologyLayoutConstants.JSON_KEY_RULE_DECLARED_OUTPUT);
            for (Map declaredOutput: declaredOutputs) {
                outputFields.add((String) declaredOutput.get(TopologyLayoutConstants.JSON_KEY_NAME));
            }
            String streamId = processorName + "." + ruleName + "." + ruleId;
            result.put(streamId, outputFields);
        }
        return result;
    }

    // For a custom processor config object, this method returns a map of output streamids and schema for each
    // such stream. This is used in validation in validateCustomProcessorLinks method above
    private Map<String, Schema> getCustomProcessorOutputSchema (Map config) throws IOException {
        Map<String, Schema> result = new LinkedHashMap<String, Schema>();
        Map<String, Object> outputSchemaConfig = (Map<String, Object>) config.get(TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAMS_SCHEMA);
        for (Map.Entry<String, Object> entry: outputSchemaConfig.entrySet()) {
            result.put(entry.getKey(), CoreUtils.getSchemaFromConfig((Map) entry.getValue()));
        }
        return result;
    }

    // For a custom processor config object, this method returns its input schema.
    // This is used in validation in validateCustomProcessorLinks method above
    private Schema getCustomProcessorInputSchema (Map config) throws IOException {
        return CoreUtils.getSchemaFromConfig((Map) config.get(TopologyLayoutConstants.JSON_KEY_INPUT_SCHEMA));
    }


    private Set<String> getTopLevelFieldNamesFromSchema (Schema schema) {
        Set<String> result = new HashSet<>();
        if (schema != null) {
            List<Schema.Field> fields = schema.getFields();
            for (Schema.Field f: fields) {
                result.add(f.getName());
            }
        }
        return result;
    }
}
