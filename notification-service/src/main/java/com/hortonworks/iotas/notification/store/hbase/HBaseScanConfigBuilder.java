package com.hortonworks.iotas.notification.store.hbase;

import com.hortonworks.iotas.notification.store.Criteria;
import com.hortonworks.iotas.notification.store.NotificationStoreException;
import com.hortonworks.iotas.notification.store.hbase.mappers.IndexMapper;
import com.hortonworks.iotas.notification.store.hbase.mappers.Mapper;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helps build a {@link HBaseScanConfig} from the {@link Criteria} that the client passes.
 */
public class HBaseScanConfigBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseScanConfigBuilder.class);

    public static final String DEFAULT_INDEX_FIELD_NAME = "ts";

    /**
     * A mapping of the entity class to a map of indexed field name and corresponding mapper.
     * E.g. {Notification.class -> {dataSourceId -> DataSourceNotificationMapper()}}
     */
    private Map<Class<?>, Map<String, IndexMapper<?>>> mappers = new HashMap<>();

    /**
     * Adds a list of index mappers for the entity (E.g. Notification, IotasEvent etc). The Class of the entity
     * is passed as the first argument to ensure type safety.
     *
     * @param clazz        the class of the entity for which the index mappers are added.
     * @param indexMappers the index mappers for the entity
     */
    public <T> void addMappers(Class<T> clazz, List<? extends IndexMapper<T>> indexMappers) {
        if (clazz == null) {
            throw new NotificationStoreException("clazz is null");
        }
        LOG.debug("HBaseScanConfigBuilder adding indexMappers for {}", clazz.getName());
        Map<String, IndexMapper<?>> indexMap = mappers.get(clazz);
        if (indexMap == null) {
            indexMap = new HashMap<>();
            mappers.put(clazz, indexMap);
        }
        for (IndexMapper<T> im : indexMappers) {
            String indexedFieldName = im.getIndexedFieldName();
            indexMap.put(indexedFieldName, im);
            LOG.debug("Added {} -> {}", indexedFieldName, im.getClass().getName());
        }
    }

    private String fieldsKey(List<Criteria.Field> fields) {
        StringBuilder sb = new StringBuilder();
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    sb.append(Mapper.ROWKEY_SEP);
                }
                sb.append(fields.get(i).getName());
            }
        }
        LOG.debug("fieldsKey for fields {} is {}", fields, sb);
        return sb.toString();
    }

    private String fieldsValue(List<Criteria.Field> fields) {
        StringBuilder sb = new StringBuilder();
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    sb.append(Mapper.ROWKEY_SEP);
                }
                sb.append(fields.get(i).getValue());
            }
        }
        LOG.debug("fieldsValue for fields {} is {}", fields, sb);
        return sb.toString();
    }

    /**
     * returns a HBaseScanConfig corresponding to the passed in Criteria object.
     * NOTE: The field restrictions are expected to be in order.
     * e.g. for a Hbase index table that maps datasourceId:status:ts -> Notification,
     * the query criteria should be {dataSourceId=x, status=y}
     */
    @SuppressWarnings("unchecked")
    public <T> HBaseScanConfig<T> getScanConfig(Criteria<T> criteria) {
        Map<String, IndexMapper<?>> indexMap = mappers.get(criteria.clazz());
        HBaseScanConfig<T> hBaseScanConfig = null;
        if (indexMap != null) {
            hBaseScanConfig = new HBaseScanConfig<>();
            /*
             * Right now the approach is to keep shrinking the field list from the end and try to match.
             * This should be fine as long as the query fields are few.
             * TODO: evaluate something like trie to do longest prefix match if needed.
             */
            IndexMapper<T> indexMapper = null;
            List<Criteria.Field> nonIndexedFields = new ArrayList<>();
            List<Criteria.Field> fieldRestrictions = criteria.fieldRestrictions();
            if (!fieldRestrictions.isEmpty()) {
                LinkedList<Criteria.Field> scanFields = new LinkedList<>(fieldRestrictions);
                while (!scanFields.isEmpty()) {
                    // its ok to cast since we have ensured the type while inserting
                    indexMapper = (IndexMapper<T>) indexMap.get(fieldsKey(scanFields));
                    if (indexMapper != null) {
                        hBaseScanConfig.setMapper(indexMapper);
                        hBaseScanConfig.setIndexedFieldValue(fieldsValue(scanFields));
                        break;
                    } else {
                        // strip off the last field and put it into non indexed field.
                        nonIndexedFields.add(scanFields.removeLast());
                    }
                }
            }

            // we haven't found an index mapper, use the default index table if available
            if (indexMapper == null) {
                if ((indexMapper = (IndexMapper<T>) indexMap.get(DEFAULT_INDEX_FIELD_NAME)) == null) {
                    return null; // no default index table, we cant proceed with the scan
                }
                hBaseScanConfig.setMapper(indexMapper);
            }

            // add filters for non-indexed fields
            for (Criteria.Field field : nonIndexedFields) {
                List<byte[]> CfCqCv = indexMapper.mapMemberValue(field.getName(), field.getValue());
                if (CfCqCv != null) {
                    SingleColumnValueFilter filter = new SingleColumnValueFilter(CfCqCv.get(0),
                                                                                 CfCqCv.get(1),
                                                                                 CompareFilter.CompareOp.EQUAL,
                                                                                 CfCqCv.get(2));
                    hBaseScanConfig.addFilter(filter);
                } else {
                    return null; // field not found
                }
            }
            hBaseScanConfig.setStartTs(criteria.startTs());
            hBaseScanConfig.setEndTs(criteria.endTs());
            hBaseScanConfig.setNumRows(criteria.numRows());
        }

        return hBaseScanConfig;
    }
}
