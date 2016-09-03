package com.hortonworks.iotas.notification.notifiers.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.store.hbase.mappers.NotificationIndexMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Secondary index mapping for dataSourceId. This is to enable device Notification
 * lookup based on dataSourceId.
 */
public class DatasourceDeviceNotificationMapper extends NotificationIndexMapper {
    /**
     * The HBase index table
     */
    private static final String TABLE_NAME = "Datasource_Device_Notification";

    /**
     * The device notification field that is indexed
     */
    private static final List<String> INDEX_FIELD_NAMES = Arrays.asList("dataSourceId");

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
