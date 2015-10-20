package com.hortonworks.iotas.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.util.exception.BadDataStreamLayoutException;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for validating json string representing a IoTaS DataStream
 * layout
 */
public class DataStreamLayoutValidator {
    public final static String JSON_KEY_CATALOG_ROOT_URL = "catalogRootUrl";
    public final static String JSON_KEY_DATA_SOURCES = "dataSources";
    public final static String JSON_KEY_UINAME = "uiname";
    public final static String JSON_KEY_ID = "id";
    public final static String JSON_KEY_TYPE = "type";
    public final static String JSON_KEY_CONFIG = "config";
    public final static String JSON_KEY_ZK_URL = "zkUrl";
    public final static String JSON_KEY_TOPIC = "topic";
    public final static String JSON_KEY_PROCESSORS = "processors";
    public final static String JSON_KEY_DATA_SINKS = "dataSinks";
    public final static String JSON_KEY_ROOT_DIR = "rootDir";
    public final static String JSON_KEY_TABLE = "table";
    public final static String JSON_KEY_COLUMN_FAMILY = "columnFamily";
    public final static String JSON_KEY_ROW_KEY = "rowKey";
    public final static String JSON_KEY_FS_URL = "fsUrl";
    public final static String JSON_KEY_PATH = "path";
    public final static String JSON_KEY_NAME = "name";
    public final static String JSON_KEY_LINKS = "links";
    public final static String JSON_KEY_FROM = "from";
    public final static String JSON_KEY_TO = "to";

    public final static String ERR_MSG_UINAME_DUP = "Uiname %s is already " +
            "used by other component.";
    public final static String ERR_MSG_LINK_FROM = "Link from property %s " +
            "has to be a data source component or a processor out component " +
            "(for e.g. a rule).";
    public final static String ERR_MSG_LINK_TO = "Link to property %s " +
            "has to be a data sink component or a processor in component.";
    public final static String ERR_MSG_DISCONNETED_DATA_SOURCE = "Data Source" +
            " %s is not linked to any component.";
    public final static String ERR_MSG_DISCONNETED_DATA_SINK = "Data Sink " +
            "%s is not linked to any component.";
    public static final String ERR_MSG_DISCONNETED_PROCESSOR_IN = "Processor " +
            "%s does not take an input.";
    public static final String ERR_MSG_DISCONNETED_PROCESSOR_OUT = "Processor" +
            " %s does not connect to an output.";
    public static final String ERR_MSG_DATA_SOURCE_NOT_FOUND = "Data Source " +
            "with id %s not found in catalog.";
    public static final String ERR_MSG_DATA_SOURCE_INVALID_TYPE = "Data " +
            "Source type %s is not a valid type.";
    public static final String ERR_MSG_DATA_SINK_INVALID_TYPE = "Data " +
            "Sink type %s is not a valid type.";
    public static final String ERR_MSG_DATA_SOURCE_MISSING_CONFIG = "Config " +
            "parameters missing for Data Source %s.";
    public static final String ERR_MSG_DATA_SINK_MISSING_CONFIG = "Config " +
            "parameters missing for Data Sink %s.";

    enum DataSourceType {
        KAFKA {
            boolean isValidConfig (Map config) {
                String zkUrl = (String) config.get(JSON_KEY_ZK_URL);
                String topic = (String) config.get(JSON_KEY_TOPIC);
                if (StringUtils.isEmpty(zkUrl) || StringUtils.isEmpty(topic)) {
                    return false;
                }
                return true;
            }
        };
        abstract boolean isValidConfig (Map config);
    }

    enum ProcessorType {
        RULE {
            boolean isValidConfig (Map config) {
                //TODO: Add rule related config check here
                return true;
            }
        };
        abstract boolean isValidConfig (Map config);
    }

    enum DataSinkType {
        HBASE {
            boolean isValidConfig (Map config) {
                String rootDir = (String) config.get(JSON_KEY_ROOT_DIR);
                String table = (String) config.get(JSON_KEY_TABLE);
                String columnFamily = (String) config.get(JSON_KEY_COLUMN_FAMILY);
                String rowKey = (String) config.get(JSON_KEY_ROW_KEY);
                if (StringUtils.isEmpty(rootDir) || StringUtils.isEmpty
                        (table) || StringUtils.isEmpty(columnFamily) ||
                        StringUtils.isEmpty(rowKey)) {
                    return false;
                }
                return true;
            }
        },
        HDFS {
            boolean isValidConfig (Map config) {
                String fsUrl = (String) config.get(JSON_KEY_FS_URL);
                String path = (String) config.get(JSON_KEY_PATH);
                String name = (String) config.get(JSON_KEY_NAME);
                if (StringUtils.isEmpty(fsUrl) || StringUtils.isEmpty (path)
                        || StringUtils.isEmpty(name)) {
                    return false;
                }
                return true;
            }
        };
        abstract boolean isValidConfig (Map config);
    }

    /**
     *
     * @param json string representation of json representing data stream
     *             layout for IoTaS (a.k.a. DAG for IoTaS generated by user)
     * @param dao storage manager for validating components of layout
     * @throws BadDataStreamLayoutException
     */
    public static void validateDataStreamLayout (String json,
                                                 StorageManager dao) throws
            BadDataStreamLayoutException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map jsonMap = objectMapper.readValue(json, Map
                    .class);
            Set<String> componentKeys = new HashSet<String>();
            Set<String> dataSourceComponentKeys = new HashSet<String>();
            Set<String> dataSinkComponentKeys = new HashSet<String>();
            Set<String> processorInComponentKeys = new HashSet<String>();
            Set<String> processorOutComponentKeys = new HashSet<String>();
            Set<String> linkFromComponentKeys = new HashSet<String>();
            Set<String> linkToComponentKeys = new HashSet<String>();
            List<Map> dataSources = (List<Map>) jsonMap.get(
                    JSON_KEY_DATA_SOURCES);
            for (Map dataSource: dataSources) {
                // data source name given by the user in UI
                String dataSourceName = (String) dataSource.get(JSON_KEY_UINAME);
                if (componentKeys.contains(dataSourceName)) {
                    throw new BadDataStreamLayoutException(String.format
                            (ERR_MSG_UINAME_DUP, dataSourceName));
                }
                dataSourceComponentKeys.add(dataSourceName);
                componentKeys.add(dataSourceName);
                validateDataSource(dataSource, dao);
            }
            List<Map> dataSinks = (List<Map>) jsonMap.get(JSON_KEY_DATA_SINKS);
            for (Map dataSink: dataSinks) {
                // data sink name given by the user in UI
                String dataSinkName = (String) dataSink.get(JSON_KEY_UINAME);
                if (componentKeys.contains(dataSinkName)) {
                    throw new BadDataStreamLayoutException(String.format
                            (ERR_MSG_UINAME_DUP, dataSinkName));
                }
                dataSinkComponentKeys.add(dataSinkName);
                componentKeys.add(dataSinkName);
                validateDataSink(dataSink);
            }
            List<Map> processors = (List<Map>) jsonMap.get(JSON_KEY_PROCESSORS);
            for (Map processor: processors) {
                // processor name given by the user in UI
                String processorName = (String) processor.get(JSON_KEY_UINAME);
                if (componentKeys.contains(processorName)) {
                    throw new BadDataStreamLayoutException(String.format
                            (ERR_MSG_UINAME_DUP, processorName));
                }
                processorInComponentKeys.add(processorName);
                componentKeys.add(processorName);
                // presumption is all processor components are rules. might
                // need to change later
                List<Map> rules = (List<Map>) processor.get(JSON_KEY_CONFIG);
                for (Map rule: rules) {
                    // rule name given by the user in UI
                    String ruleName = (String) rule.get(JSON_KEY_UINAME);
                    if (componentKeys.contains(ruleName)) {
                        throw new BadDataStreamLayoutException(String.format
                                (ERR_MSG_UINAME_DUP, ruleName));
                    }
                    processorOutComponentKeys.add(ruleName);
                    componentKeys.add(ruleName);
                    validateRule(rule, dao);
                }
            }
            List<Map> links = (List<Map>)  jsonMap.get(JSON_KEY_LINKS);
            //TODO: may be add a cycle check for a link that involves processor
            for (Map link: links) {
                // link name given by the user in UI
                String linkName = (String) link.get(JSON_KEY_UINAME);
                if (componentKeys.contains(linkName)) {
                    throw new BadDataStreamLayoutException(String.format
                            (ERR_MSG_UINAME_DUP, linkName));
                }
                String linkFrom = (String) link.get(JSON_KEY_FROM);
                String linkTo = (String) link.get(JSON_KEY_TO);
                if ((!dataSourceComponentKeys.contains(linkFrom) &&
                        !processorOutComponentKeys.contains(linkFrom)) ||
                        dataSinkComponentKeys.contains(linkFrom) ||
                        processorInComponentKeys.contains(linkFrom)) {
                    throw new BadDataStreamLayoutException(String.format
                            (ERR_MSG_LINK_FROM, linkFrom));
                }
                if ((!dataSinkComponentKeys.contains(linkTo) &&
                        !processorInComponentKeys.contains(linkTo)) ||
                        dataSourceComponentKeys.contains(linkTo) ||
                        processorOutComponentKeys.contains(linkTo)) {
                    throw new BadDataStreamLayoutException(String.format
                            (ERR_MSG_LINK_TO, linkTo));
                }
                linkFromComponentKeys.add(linkFrom);
                linkToComponentKeys.add(linkTo);
            }
            for (String source: dataSourceComponentKeys) {
                if (!linkFromComponentKeys.contains(source)) {
                    throw new BadDataStreamLayoutException(String.format
                            (ERR_MSG_DISCONNETED_DATA_SOURCE, source));
                }
            }
            for (String sink: dataSinkComponentKeys) {
                if (!linkToComponentKeys.contains(sink)) {
                    throw new BadDataStreamLayoutException(String.format
                            (ERR_MSG_DISCONNETED_DATA_SINK, sink));
                }
            }
            for (String processorIn: processorInComponentKeys) {
                if (!linkToComponentKeys.contains(processorIn)) {
                    throw new BadDataStreamLayoutException(String.format
                            (ERR_MSG_DISCONNETED_PROCESSOR_IN, processorIn));
                }
            }
            for (String processorOut: processorOutComponentKeys) {
                if (!linkFromComponentKeys.contains(processorOut)) {
                    throw new BadDataStreamLayoutException(String.format
                            (ERR_MSG_DISCONNETED_PROCESSOR_OUT, processorOut));
                }
            }
        } catch (Exception ex) {
            throw new BadDataStreamLayoutException(ex);
        }

    }

    private static void validateDataSource (Map dataSource,
                                            StorageManager dao) throws
            BadDataStreamLayoutException {

        Long dataSourceId = ((Integer) dataSource.get(JSON_KEY_ID)).longValue();
        DataSource ds = new DataSource();
        ds.setDataSourceId(dataSourceId);
        DataSource result = dao.get(ds.getStorableKey());
        if (result == null) {
            throw new BadDataStreamLayoutException(String.format
                    (ERR_MSG_DATA_SOURCE_NOT_FOUND, dataSourceId.toString()));
        }
        String dataSourceType = (String) dataSource.get(JSON_KEY_TYPE);
        boolean isDataSourceTypeValid = false;
        DataSourceType matchedDst = null;
        // type of data source has to be one of predefiend enums
        for (DataSourceType dst: DataSourceType.values()) {
            if (dst.name().equals(dataSourceType)) {
                matchedDst = dst;
                isDataSourceTypeValid = true;
                break;
            }
        }
        if (!isDataSourceTypeValid) {
            throw new BadDataStreamLayoutException(String.format
                    (ERR_MSG_DATA_SOURCE_INVALID_TYPE, dataSourceType));
        }
        Map config = (Map) dataSource.get(JSON_KEY_CONFIG);
        // for the matched enum it has to have all the config properties needed
        if (!matchedDst.isValidConfig(config)) {
            throw new BadDataStreamLayoutException(String.format
                    (ERR_MSG_DATA_SOURCE_MISSING_CONFIG, matchedDst.name()));
        }
    }

    private static void validateDataSink (Map dataSink) throws
            BadDataStreamLayoutException {
        String dataSinkType = (String) dataSink.get(JSON_KEY_TYPE);
        boolean isDataSinkTypeValid = false;
        DataSinkType matchedDst = null;
        // type of data sink has to be one of predefiend enums
        for (DataSinkType dst: DataSinkType.values()) {
            if (dst.name().equals(dataSinkType)) {
                matchedDst = dst;
                isDataSinkTypeValid = true;
                break;
            }
        }
        if (!isDataSinkTypeValid) {
            throw new BadDataStreamLayoutException(String.format
                    (ERR_MSG_DATA_SINK_INVALID_TYPE, dataSinkType));
        }
        Map config = (Map) dataSink.get(JSON_KEY_CONFIG);
        // for the matched enum it has to have all the config properties needed
        if (!matchedDst.isValidConfig(config)) {
            throw new BadDataStreamLayoutException(String.format
                    (ERR_MSG_DATA_SINK_MISSING_CONFIG, matchedDst.name()));
        }
    }
    private static void validateRule (Map rule, StorageManager
            dao) throws BadDataStreamLayoutException {
        //TODO: rule validation to be done here
        return;
    }

}
