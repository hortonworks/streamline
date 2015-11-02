package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mapper for the IotasEvent
 */
public class IotasEventMapper implements Mapper<IotasEvent> {

    // TODO: the table should be changed to "Iotasevent"
    private static final String TABLE_NAME = "nest";

    private static final byte[] CF_FIELDS = "cf".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CF_DATASOURCE_ID = "d".getBytes(StandardCharsets.UTF_8);

    @Override
    public List<TableMutation> tableMutations(IotasEvent iotasEvent) {
        throw new UnsupportedOperationException("Not implemented, IotasEvents are currently inserted via HbaseBolt.");
    }

    @Override
    public IotasEvent entity(Result result) {
        String id = Bytes.toString(result.getRow());
        Map<String, Object> fieldsAndValues = new HashMap<>();
        for(Map.Entry<byte[], byte[]> entry: result.getFamilyMap(CF_FIELDS).entrySet()) {
            fieldsAndValues.put(Bytes.toString(entry.getKey()), Bytes.toString(entry.getValue()));
        }
        String dataSourceId = Bytes.toString(result.getFamilyMap(CF_DATASOURCE_ID).firstEntry().getKey());
        return new IotasEventImpl(fieldsAndValues, dataSourceId, id);
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public List<byte[]> mapMemberValue(String memberName, String value) {
        // Does not support querying by field.
        return null;
    }
}
