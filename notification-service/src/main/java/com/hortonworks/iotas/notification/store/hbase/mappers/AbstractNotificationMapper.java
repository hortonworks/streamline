package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationImpl;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the base class that implements the methods for mapping to and from
 * Notification and HBase entities.
 */
public abstract class AbstractNotificationMapper implements Mapper<Notification> {
    protected static final Charset CHARSET = StandardCharsets.UTF_8;

    protected static final byte[] CF_FIELDS           = "f".getBytes(CHARSET);
    protected static final byte[] CF_STATUS           = "s".getBytes(CHARSET);
    protected static final byte[] CF_NOTIFIER_NAME    = "nn".getBytes(CHARSET);
    protected static final byte[] CF_EVENTIDS         = "e".getBytes(CHARSET);
    protected static final byte[] CF_DATASOURCE_IDS   = "d".getBytes(CHARSET);
    protected static final byte[] CF_RULEID           = "r".getBytes(CHARSET);

    protected static final byte[] CQ_STATUS           = "qs".getBytes(CHARSET);

    protected static final byte[] CV_DEFAULT          = "1".getBytes(CHARSET);

    protected static final String ROWKEY_SEP          = "|";


    @Override
    public List<TableMutation> tableMutations(Notification notification) {
        List<TableMutation> tableMutations = new ArrayList<>();
        for(byte[] rowKey: getRowKeys(notification)) {
            Put put = new Put(rowKey);
            addColumns(put, notification);
            tableMutations.add(new TableMutationImpl(getTableName(), put));
        }
        return tableMutations;
    }

    protected void addColumns(Put put, Notification notification) {

        // fields
        for(Map.Entry<String, String> field : notification.getFieldsAndValues().entrySet()) {
            put.add(CF_FIELDS,
                    field.getKey().getBytes(CHARSET),
                          field.getValue().getBytes(CHARSET));
        }

        // status
        put.add(CF_STATUS, CQ_STATUS, notification.getStatus().toString().getBytes(CHARSET));

        // event-ids
        for (String eventId : notification.getEventIds()) {
            put.add(CF_EVENTIDS, eventId.getBytes(CHARSET), CV_DEFAULT);
        }

        // data source ids
        for (String eventId : notification.getDataSourceIds()) {
            put.add(CF_DATASOURCE_IDS, eventId.getBytes(CHARSET), CV_DEFAULT);
        }

        // rule-id
        put.add(CF_RULEID, notification.getRuleId().getBytes(CHARSET), CV_DEFAULT);

        // notifier name
        put.add(CF_NOTIFIER_NAME, notification.getNotifierName().getBytes(CHARSET), CV_DEFAULT);

    }

    @Override
    public Notification entity(Result result) {

        String id = getNotificationId(result);

        Map<String, String> fieldsAndValues = new HashMap<>();
        for(Map.Entry<byte[], byte[]> entry: result.getFamilyMap(CF_FIELDS).entrySet()) {
            fieldsAndValues.put(Bytes.toString(entry.getKey()), Bytes.toString(entry.getValue()));
        }

        List<String> eventIds = new ArrayList<>();
        for(Map.Entry<byte[], byte[]> entry: result.getFamilyMap(CF_EVENTIDS).entrySet()) {
            eventIds.add(Bytes.toString(entry.getKey()));
        }


        List<String> dataSourceIds = new ArrayList<>();
        for(Map.Entry<byte[], byte[]> entry: result.getFamilyMap(CF_DATASOURCE_IDS).entrySet()) {
            dataSourceIds.add(Bytes.toString(entry.getKey()));
        }

        String ruleId = Bytes.toString(result.getFamilyMap(CF_RULEID).firstEntry().getKey());

        Notification.Status status = Notification.Status.valueOf(Bytes.toString(result.getValue(CF_STATUS, CQ_STATUS)));

        String notifierName = Bytes.toString(result.getFamilyMap(CF_NOTIFIER_NAME).firstEntry().getKey());

        return new NotificationImpl
                .Builder(fieldsAndValues)
                .id(id)
                .eventIds(eventIds)
                .dataSourceIds(dataSourceIds)
                .ruleId(ruleId)
                .notifierName(notifierName)
                .status(status).build();
    }


    /**
     * The mapping table name
     */
    public abstract String getTableName();

    /**
     * Return one or more row keys for mapping the notification object.
     */
    protected abstract List<byte[]> getRowKeys(Notification notification);

    /**
     * return the notification id from the result
     */
    protected abstract String getNotificationId(Result result);

}
