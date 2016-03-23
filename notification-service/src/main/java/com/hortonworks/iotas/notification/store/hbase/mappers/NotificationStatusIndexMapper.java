package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.Notification;
import com.hortonworks.iotas.notification.common.NotificationImpl;
import org.apache.hadoop.hbase.client.Delete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base class for all the index mappers whose index includes status (which is mutable).
 *
 */
public abstract class NotificationStatusIndexMapper extends NotificationIndexMapper {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationStatusIndexMapper.class);

    @Override
    public List<TableMutation> status(Notification notification, Notification.Status status) {
        // insert a new row
        Notification updated = new NotificationImpl.Builder(notification).status(status).build();
        List<TableMutation> tableMutations = tableMutations(updated);
        // delete existing
        for (byte[] rowKey : getRowKeys(notification)) {
            Delete delete = new Delete(rowKey);
            tableMutations.add(new TableMutationImpl(getTableName(), delete));
        }
        LOG.trace("TableMutations for status update {}", tableMutations);
        return tableMutations;
    }
}
