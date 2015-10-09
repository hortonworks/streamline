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
     * The notification field that is indexed
     */
    private static final String INDEX_FIELD_NAME = "dataSourceId" + Mapper.ROWKEY_SEP + "status";


    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        List<byte[]> rowKeys = new ArrayList<>();
        for (String dataSourceId : notification.getDataSourceIds()) {
            rowKeys.add(new StringBuilder(dataSourceId)
                                .append(ROWKEY_SEP)
                                .append(notification.getStatus())
                                .append(ROWKEY_SEP)
                                .append(notification.getTs())
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
    public String getIndexedFieldName() {
        return INDEX_FIELD_NAME;
    }
}
