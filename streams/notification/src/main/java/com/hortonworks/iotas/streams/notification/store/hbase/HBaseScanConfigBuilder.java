/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.hortonworks.iotas.streams.notification.store.hbase;

import com.hortonworks.iotas.streams.notification.store.Criteria;
import com.hortonworks.iotas.streams.notification.store.NotificationStoreException;
import com.hortonworks.iotas.streams.notification.store.hbase.mappers.IndexMapper;
import com.hortonworks.iotas.streams.notification.store.hbase.mappers.Mapper;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helps build a {@link HBaseScanConfig} from the {@link Criteria} that the client passes.
 */
public class HBaseScanConfigBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseScanConfigBuilder.class);

    public static final List<String> DEFAULT_INDEX_FIELD_NAMES = Arrays.asList("ts");

    /**
     * A mapping of the entity class to a map of indexed field name and corresponding mapper.
     * E.g. {Notification.class -> {[dataSourceId] -> DataSourceNotificationMapper()}}
     */
    private Map<Class<?>, Map<List<String>, IndexMapper<?>>> mappers = new HashMap<>();

    private static final Comparator<IndexMapper<?>> reverseIndexFieldsLengthComparator =
            new Comparator<IndexMapper<?>>() {
                @Override
                public int compare(IndexMapper<?> im1, IndexMapper<?> im2) {
                    return im2.getIndexedFieldNames().size() - im1.getIndexedFieldNames().size();
                }
            };

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
        Map<List<String>, IndexMapper<?>> indexMap = mappers.get(clazz);
        if (indexMap == null) {
            indexMap = new LinkedHashMap<>();
            mappers.put(clazz, indexMap);
        }
        /*
         * store the mappers in descending order of index field length so that
         * the lookup for finding the best match can be slightly more efficient
         * when there are multiple indexed fields in the query.
         */
        List<IndexMapper<?>> sorted = new ArrayList<>(indexMap.values());
        sorted.addAll(indexMappers);
        Collections.sort(sorted, reverseIndexFieldsLengthComparator);
        indexMap.clear();
        for (IndexMapper<?> im : sorted) {
            List<String> indexedFieldNames = im.getIndexedFieldNames();
            indexMap.put(indexedFieldNames, im);
            LOG.debug("Added {} -> {}", indexedFieldNames, im.getClass().getName());
        }
        LOG.debug("indexMap {}", indexMap);
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
     * Loop through the index mappers and find the best one to use for the
     * given fields in the query. The best index is the one with the max number
     * of index fields that is also present in the query fields.
     * Since there are typically only a handful of index mappers, it should be ok to loop.
     */
    private Map.Entry<List<String>, IndexMapper<?>> bestMatch(Map<List<String>, IndexMapper<?>> indexMap,
                                                              Set<String> queryFieldNames) {
        LOG.debug("Finding best index mapper for queryFieldNames {}", queryFieldNames);
        // find the first entry with the index fields which are in the query fields
        for (Map.Entry<List<String>, IndexMapper<?>> entry : indexMap.entrySet()) {
            LOG.debug("Checking Entry {}", entry);
            List<String> indexedFields = entry.getKey();
            boolean candidate = true;
            for (String indexedField : indexedFields) {
                if (!queryFieldNames.contains(indexedField)) {
                    candidate = false; // since all index fields are not in query fields
                    break;
                }
            }
            if (candidate && indexedFields.size() > 0) {
                /*
                 * since the indexMap in the reverse order of index fields length,
                 * the first match should be the best
                 */
                return entry;
            }
        }
        LOG.debug("Did not find a match");
        return null;
    }

    /**
     * returns a HBaseScanConfig corresponding to the passed in Criteria object.
     */
    @SuppressWarnings("unchecked")
    public <T> HBaseScanConfig<T> getScanConfig(Criteria<T> criteria) {
        Map<List<String>, IndexMapper<?>> indexMap = mappers.get(criteria.clazz());
        HBaseScanConfig<T> hBaseScanConfig = null;
        if (indexMap != null) {
            hBaseScanConfig = new HBaseScanConfig<>();
            IndexMapper<T> indexMapper = null;
            List<Criteria.Field> nonIndexedFields = new ArrayList<>();
            List<Criteria.Field> indexedFields = new ArrayList<>();
            List<Criteria.Field> fieldRestrictions = criteria.fieldRestrictions();
            if (!fieldRestrictions.isEmpty()) {
                // construct query field name -> Field map
                Map<String, Criteria.Field> queryFieldMap = new HashMap<>();
                for (Criteria.Field field : fieldRestrictions) {
                    queryFieldMap.put(field.getName(), field);
                }
                // find the best index mapper to use
                Map.Entry<List<String>, IndexMapper<?>> bestEntry = bestMatch(indexMap, queryFieldMap.keySet());
                if (bestEntry != null) {
                    LOG.debug("Found bestEntry {} for fieldRestrictions {}", bestEntry, fieldRestrictions);
                    indexMapper = (IndexMapper<T>) bestEntry.getValue();
                    hBaseScanConfig.setMapper(indexMapper);
                    // add the fields available in the query to indexedFields
                    for (String indexedFieldName : bestEntry.getKey()) {
                        Criteria.Field field;
                        if ((field = queryFieldMap.remove(indexedFieldName)) != null) {
                            indexedFields.add(field);
                        } else {
                            break; // no more fields can be used as prefix so stop.
                        }
                    }
                    hBaseScanConfig.setIndexedFieldValue(fieldsValue(indexedFields));
                    nonIndexedFields.addAll(queryFieldMap.values()); // remaining fields
                }
            }

            // we haven't found an index mapper, use the default index table if available
            if (indexMapper == null) {
                if ((indexMapper = (IndexMapper<T>) indexMap.get(DEFAULT_INDEX_FIELD_NAMES)) == null) {
                    return null; // no default index table, we can't proceed with the scan
                }
                hBaseScanConfig.setMapper(indexMapper);
                nonIndexedFields.addAll(fieldRestrictions);
            }

            LOG.debug("nonIndexedFields {}", nonIndexedFields);
            // add filters for non-indexed fields
            for (Criteria.Field field : nonIndexedFields) {
                List<byte[]> CfCqCv = indexMapper.mapMemberValue(field.getName(), field.getValue());
                if (CfCqCv != null) {
                    SingleColumnValueFilter filter = new SingleColumnValueFilter(CfCqCv.get(0),
                                                                                 CfCqCv.get(1),
                                                                                 CompareFilter.CompareOp.EQUAL,
                                                                                 CfCqCv.get(2));
                    filter.setFilterIfMissing(true);
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
