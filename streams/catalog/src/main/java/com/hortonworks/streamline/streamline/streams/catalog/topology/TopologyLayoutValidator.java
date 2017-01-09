package org.apache.streamline.streams.catalog.topology;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.layout.exception.ComponentConfigException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for validating json string representing a Streamline Topology
 * layout
 */
public class TopologyLayoutValidator {
    private final String json;
    private Map jsonMap;
    private final Set<String> componentKeys = new HashSet<>();
    private final Set<String> dataSourceComponentKeys = new HashSet<>();
    private final Set<String> dataSinkComponentKeys = new HashSet<>();
    private final Set<String> processorComponentKeys = new HashSet<>();
    private final Set<String> linkFromComponentKeys = new HashSet<>();
    private final Set<String> linkToComponentKeys = new HashSet<>();

    // Constructor to initialize the object with json to be validated where
    // json is the string representation of an streamline topology created using UI
    public TopologyLayoutValidator (String json) {
        this.json = json;
    }
    /**
     * @throws ComponentConfigException
     */
    public void validate () throws ComponentConfigException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonMap = objectMapper.readValue(json, Map.class);
            validateDataSources();
            validateDataSinks();
            validateProcessors();
            validateLinks();
            checkDisconnectedDataSources();
            checkDisconnectedDataSinks();
            checkDisconnectedProcessors();
        } catch (Exception ex) {
            throw new ComponentConfigException(ex);
        }
    }

    private void validateDataSources () throws ComponentConfigException {
        List<Map> dataSources = (List<Map>) jsonMap.get(TopologyLayoutConstants.JSON_KEY_DATA_SOURCES);
        for (Map dataSource: dataSources) {
            // data source name given by the user in UI
            String dataSourceName = (String) dataSource.get(TopologyLayoutConstants.JSON_KEY_UINAME);
            if (componentKeys.contains(dataSourceName)) {
                throw new ComponentConfigException(String.format
                        (TopologyLayoutConstants.ERR_MSG_UINAME_DUP, dataSourceName));
            }
            dataSourceComponentKeys.add(dataSourceName);
            componentKeys.add(dataSourceName);
        }
    }

    private void validateDataSinks () throws ComponentConfigException {
        List<Map> dataSinks = (List<Map>) jsonMap.get(TopologyLayoutConstants.JSON_KEY_DATA_SINKS);
        for (Map dataSink: dataSinks) {
            // data sink name given by the user in UI
            String dataSinkName = (String) dataSink.get(TopologyLayoutConstants.JSON_KEY_UINAME);
            if (componentKeys.contains(dataSinkName)) {
                throw new ComponentConfigException(String.format
                        (TopologyLayoutConstants.ERR_MSG_UINAME_DUP, dataSinkName));
            }
            dataSinkComponentKeys.add(dataSinkName);
            componentKeys.add(dataSinkName);
        }
    }

    private void validateProcessors () throws ComponentConfigException {
        List<Map> processors = (List<Map>) jsonMap.get(TopologyLayoutConstants.JSON_KEY_PROCESSORS);
        for (Map processor: processors) {
            // processor name given by the user in UI
            String processorName = (String) processor.get(TopologyLayoutConstants.JSON_KEY_UINAME);
            if (componentKeys.contains(processorName)) {
                throw new ComponentConfigException(String.format
                        (TopologyLayoutConstants.ERR_MSG_UINAME_DUP, processorName));
            }
            processorComponentKeys.add(processorName);
            componentKeys.add(processorName);
        }
    }

    private void validateLinks () throws ComponentConfigException {
        List<Map> links = (List<Map>)  jsonMap.get(TopologyLayoutConstants.JSON_KEY_LINKS);
        for (Map link: links) {
            // link name given by the user in UI
            String linkName = (String) link.get(TopologyLayoutConstants.JSON_KEY_UINAME);
            if (componentKeys.contains(linkName)) {
                throw new ComponentConfigException(String.format
                        (TopologyLayoutConstants.ERR_MSG_UINAME_DUP, linkName));
            }
            Map linkConfig = (Map) link.get(TopologyLayoutConstants.JSON_KEY_CONFIG);
            String linkFrom = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_FROM);
            String linkTo = (String) linkConfig.get(TopologyLayoutConstants.JSON_KEY_TO);
            if (linkFrom.equals(linkTo)) {
                throw new ComponentConfigException(String.format(TopologyLayoutConstants.ERR_MSG_LOOP, linkFrom, linkTo));
            }
            // linkFrom can only be a source or a processor
            if ((!dataSourceComponentKeys.contains(linkFrom) &&
                    !processorComponentKeys.contains(linkFrom)) ||
                    dataSinkComponentKeys.contains(linkFrom)) {
                throw new ComponentConfigException(String.format
                        (TopologyLayoutConstants.ERR_MSG_LINK_FROM, linkFrom));
            }
            // linkTo can only be a sink or a processor
            if ((!dataSinkComponentKeys.contains(linkTo) &&
                    !processorComponentKeys.contains(linkTo)) ||
                    dataSourceComponentKeys.contains(linkTo)) {
                throw new ComponentConfigException(String.format
                        (TopologyLayoutConstants.ERR_MSG_LINK_TO, linkTo));
            }
            linkFromComponentKeys.add(linkFrom);
            linkToComponentKeys.add(linkTo);
        }
    }

    private void checkDisconnectedDataSources () throws ComponentConfigException {
        for (String source: dataSourceComponentKeys) {
            if (!linkFromComponentKeys.contains(source)) {
                throw new ComponentConfigException(String.format
                        (TopologyLayoutConstants.ERR_MSG_DISCONNETED_DATA_SOURCE, source));
            }
        }
    }

    private void checkDisconnectedDataSinks () throws ComponentConfigException {
        for (String sink: dataSinkComponentKeys) {
            if (!linkToComponentKeys.contains(sink)) {
                throw new ComponentConfigException(String.format
                        (TopologyLayoutConstants.ERR_MSG_DISCONNETED_DATA_SINK, sink));
            }
        }
    }

    private void checkDisconnectedProcessors () throws ComponentConfigException {
        for (String processorIn: processorComponentKeys) {
            if (!linkToComponentKeys.contains(processorIn)) {
                throw new ComponentConfigException(String.format
                        (TopologyLayoutConstants.ERR_MSG_DISCONNETED_PROCESSOR_IN, processorIn));
            }
        }
        for (String processorOut: processorComponentKeys) {
            if (!linkFromComponentKeys.contains(processorOut)) {
                throw new ComponentConfigException(String.format
                        (TopologyLayoutConstants.ERR_MSG_DISCONNETED_PROCESSOR_OUT, processorOut));
            }
        }
    }
}
