package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Secondary index mapping for dataSourceId + status to notification
 */
public class DatasourceStatusNotificationMapper extends NotificationStatusIndexMapper {
    /**
     * The HBase index table
     */
    private static final String TABLE_NAME = "Datasource_Status_Notification";
    /**
     * The notification fields that are indexed
     */
    private static final List<String> INDEX_FIELD_NAMES = Arrays.asList("dataSourceId", "status");


    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        List<byte[]> rowKeys = new ArrayList<>();
        for (String dataSourceId : notification.getDataSourceIds()) {
            rowKeys.add(new StringBuilder(dataSourceId)
                                .append(ROWKEY_SEP)
                                .append(getIndexSuffix(notification))
                                .toString().getBytes(CHARSET));
        }
        return rowKeys;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public List<String> getIndexedFieldNames() {
        return INDEX_FIELD_NAMES;
    }
}
