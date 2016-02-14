package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationImpl;
import com.hortonworks.iotas.notification.store.hbase.Serializer;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the base class that implements the methods for mapping to and from
 * Notification and HBase entities.
 */
public abstract class AbstractNotificationMapper implements Mapper<Notification> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNotificationMapper.class);

    protected static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final byte[] CF_FIELDS = "f".getBytes(CHARSET);
    private static final byte[] CF_STATUS = "s".getBytes(CHARSET);
    private static final byte[] CF_NOTIFIER_NAME = "nn".getBytes(CHARSET);
    private static final byte[] CF_EVENTIDS = "e".getBytes(CHARSET);
    private static final byte[] CF_DATASOURCE_IDS = "d".getBytes(CHARSET);
    private static final byte[] CF_RULEID = "r".getBytes(CHARSET);
    private static final byte[] CF_TIMESTAMP = "ts".getBytes(CHARSET);

    private static final byte[] CQ_STATUS = "qs".getBytes(CHARSET);

    protected static final byte[] CV_DEFAULT = "1".getBytes(CHARSET);

    // a map of Notification member name to hbase cf:cq
    private static final Map<String, List<byte[]>> memberMap = new HashMap<>();

    // serializer instance
    private final Serializer serializer = new Serializer();

    static {
        // Right now support queries for status in addition to the indexed fields.
        memberMap.put("status", Arrays.asList(CF_STATUS, CQ_STATUS));
        memberMap.put("ruleId", Arrays.asList(CF_RULEID));
        memberMap.put("dataSourceId", Arrays.asList(CF_DATASOURCE_IDS));
        memberMap.put("notifierName", Arrays.asList(CF_NOTIFIER_NAME));
    }

    @Override
    public List<byte[]> mapMemberValue(String memberName, String value) {
        List<byte[]> cfcq = memberMap.get(memberName);
        List<byte[]> res = null;
        if (cfcq != null) {
            res = new ArrayList<>();
            res.addAll(cfcq);
            if (cfcq.size() == 2) { // both cf and cq are present
                res.add(value.getBytes(CHARSET));
            } else if (cfcq.size() == 1) {
                res.add(value.getBytes(CHARSET)); // cq is the member value
                res.add(CV_DEFAULT); // cv is default value
            }
        }
        LOG.debug("memberName {} mapped to {}", memberName, res);
        return res;
    }

    @Override
    public List<TableMutation> tableMutations(Notification notification) {
        List<TableMutation> tableMutations = new ArrayList<>();
        for (byte[] rowKey : getRowKeys(notification)) {
            Put put = new Put(rowKey);
            addColumns(put, notification);
            tableMutations.add(new TableMutationImpl(getTableName(), put));
        }
        return tableMutations;
    }

    protected void addColumns(Put put, Notification notification) {

        // fields
        for (Map.Entry<String, Object> field : notification.getFieldsAndValues().entrySet()) {
            put.add(CF_FIELDS,
                    field.getKey().getBytes(CHARSET),
                    serializer.serialize(field.getValue()));
        }

        // status
        put.add(CF_STATUS, CQ_STATUS, notification.getStatus().toString().getBytes(CHARSET));

        // event-ids
        for (String eventId : notification.getEventIds()) {
            put.add(CF_EVENTIDS, eventId.getBytes(CHARSET), CV_DEFAULT);
        }

        // data source ids
        for (String dataSourceId : notification.getDataSourceIds()) {
            put.add(CF_DATASOURCE_IDS, dataSourceId.getBytes(CHARSET), CV_DEFAULT);
        }

        // rule-id
        put.add(CF_RULEID, notification.getRuleId().getBytes(CHARSET), CV_DEFAULT);

        // notifier name
        put.add(CF_NOTIFIER_NAME, notification.getNotifierName().getBytes(CHARSET), CV_DEFAULT);

        // ts
        put.add(CF_TIMESTAMP, Long.toString(notification.getTs()).getBytes(CHARSET), CV_DEFAULT);

        LOG.trace("Formed Put {} from notification {}", put, notification);
    }

    @Override
    public Notification entity(Result result) {

        String id = getNotificationId(result);

        Map<String, Object> fieldsAndValues = new HashMap<>();
        for (Map.Entry<byte[], byte[]> entry : result.getFamilyMap(CF_FIELDS).entrySet()) {
            fieldsAndValues.put(Bytes.toString(entry.getKey()), serializer.deserialize(entry.getValue()));
        }

        List<String> eventIds = new ArrayList<>();
        for (Map.Entry<byte[], byte[]> entry : result.getFamilyMap(CF_EVENTIDS).entrySet()) {
            eventIds.add(Bytes.toString(entry.getKey()));
        }


        List<String> dataSourceIds = new ArrayList<>();
        for (Map.Entry<byte[], byte[]> entry : result.getFamilyMap(CF_DATASOURCE_IDS).entrySet()) {
            dataSourceIds.add(Bytes.toString(entry.getKey()));
        }

        String ruleId = Bytes.toString(result.getFamilyMap(CF_RULEID).firstEntry().getKey());

        Notification.Status status = Notification.Status.valueOf(Bytes.toString(result.getValue(CF_STATUS, CQ_STATUS)));

        String notifierName = Bytes.toString(result.getFamilyMap(CF_NOTIFIER_NAME).firstEntry().getKey());

        long ts = Long.parseLong(Bytes.toString(result.getFamilyMap(CF_TIMESTAMP).firstEntry().getKey()));

        Notification notification = new NotificationImpl
                .Builder(fieldsAndValues)
                .id(id)
                .eventIds(eventIds)
                .dataSourceIds(dataSourceIds)
                .ruleId(ruleId)
                .notifierName(notifierName)
                .timestamp(ts)
                .status(status).build();

        LOG.trace("Created Notification {} from Result {}", notification, result);
        return notification;
    }

    public List<TableMutation> status(Notification notification, Notification.Status status) {
        List<TableMutation> tableMutations = new ArrayList<>();
        for (byte[] rowKey : getRowKeys(notification)) {
            Put put = new Put(rowKey);
            put.add(CF_STATUS, CQ_STATUS, status.toString().getBytes(CHARSET));
            tableMutations.add(new TableMutationImpl(getTableName(), put));
        }
        return tableMutations;
    }

    /**
     * Return one or more row keys for mapping the notification object.
     */
    protected abstract List<byte[]> getRowKeys(Notification notification);

    /**
     * return the notification id from the result
     */
    protected abstract String getNotificationId(Result result);

}
