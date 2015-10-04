package com.hortonworks.iotas.notification.store.hbase;

import com.hortonworks.iotas.notification.store.Criteria;
import com.hortonworks.iotas.notification.store.NotificationStoreException;
import com.hortonworks.iotas.notification.store.hbase.mappers.IndexMapper;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helps build a {@link HBaseScanConfig} from the {@link Criteria} that the client passes.
 */
public class HBaseScanConfigBuilder {

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
        Map<String, IndexMapper<?>> indexMap = mappers.get(clazz);
        if (indexMap == null) {
            indexMap = new HashMap<>();
            mappers.put(clazz, indexMap);
        }
        for (IndexMapper<T> im : indexMappers) {
            indexMap.put(im.getIndexedFieldName(), im);
        }
    }

    /**
     * returns a HBaseScanConfig corresponding to the passed in Criteria object.
     */
    @SuppressWarnings("unchecked")
    public <T> HBaseScanConfig<T> getScanConfig(Criteria<T> criteria) {
        Map<String, IndexMapper<?>> indexMap = mappers.get(criteria.clazz());
        HBaseScanConfig<T> hBaseScanConfig = null;
        if (indexMap != null) {
            hBaseScanConfig = new HBaseScanConfig<>();
            Map<String, String> fieldRestrictions = criteria.fieldRestrictions();
            Set<String> scanFields = fieldRestrictions.keySet();
            IndexMapper<T> indexMapper = null;
            for (String field : scanFields) {
                // its ok to cast since we have ensured the type while inserting
                indexMapper = (IndexMapper<T>) indexMap.get(field) ;
                if (indexMapper != null) {
                    hBaseScanConfig.setMapper(indexMapper);
                    hBaseScanConfig.setIndexedFieldValue(fieldRestrictions.get(field));
                    scanFields.remove(field); // this field is taken care of
                    break;
                }
            }

            //TODO: use a default index mapper e.g. timestamp:notificationid, if index key is not found

            // we haven't found an index mapper, so we can't proceed with the scan
            if (indexMapper == null) {
                return  null;
            }

            for (String field : scanFields) {
                List<byte[]> CfCqCv = indexMapper.mapMemberValue(field, fieldRestrictions.get(field));
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
            hBaseScanConfig.setNumRows(criteria.numRows());
        }

        return hBaseScanConfig;
    }
}
