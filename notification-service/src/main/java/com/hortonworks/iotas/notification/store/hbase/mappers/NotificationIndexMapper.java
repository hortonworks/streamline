package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.store.hbase.mappers.AbstractNotificationMapper;
import com.hortonworks.iotas.notification.store.hbase.mappers.TableMutation;
import com.hortonworks.iotas.notification.store.hbase.mappers.TableMutationImpl;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.Arrays;
import java.util.List;

/**
 * Created by aiyer on 9/29/15.
 */
public abstract class NotificationIndexMapper extends AbstractNotificationMapper {

    protected static final byte[] CF_NOTIFICATION_ID = "ni".getBytes(CHARSET);

    @Override
    protected void addColumns(Put put, Notification notification) {
        super.addColumns(put, notification);
        put.add(CF_NOTIFICATION_ID, notification.getId().getBytes(CHARSET), CV_DEFAULT);
    }

    @Override
    protected String getNotificationId(Result result) {
        return Bytes.toString(result.getFamilyMap(CF_NOTIFICATION_ID).firstEntry().getKey());
    }

}
