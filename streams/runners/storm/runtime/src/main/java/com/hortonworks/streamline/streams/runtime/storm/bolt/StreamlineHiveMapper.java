package com.hortonworks.streamline.streams.runtime.storm.bolt;

import com.hortonworks.streamline.streams.StreamlineEvent;
import org.apache.hive.hcatalog.streaming.DelimitedInputWriter;
import org.apache.hive.hcatalog.streaming.HiveEndPoint;
import org.apache.hive.hcatalog.streaming.RecordWriter;
import org.apache.hive.hcatalog.streaming.StreamingException;
import org.apache.hive.hcatalog.streaming.TransactionBatch;
import org.apache.storm.hive.bolt.mapper.HiveMapper;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Tuple;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

  public class StreamlineHiveMapper implements HiveMapper {
    private static final String DEFAULT_FIELD_DELIMITER = ",";
    private final List<String> fields;
    private final List<String> partitionFields;
    private String fieldDelimiter = DEFAULT_FIELD_DELIMITER;
    private String timeFormat;
    private SimpleDateFormat parseDate;


    public StreamlineHiveMapper(List<String> fields, List<String> partitionFields) {
        Objects.requireNonNull(fields, "Empty fields");
        Objects.requireNonNull(partitionFields, "Empty partitionFields");
        this.fields = fields.stream().map(field -> field.substring(field.indexOf(":") + 1)).collect(Collectors.toList());
        this.partitionFields = partitionFields.stream().map(field -> field.substring(field.indexOf(":") + 1)).collect(Collectors.toList());
    }

    @Override
    public RecordWriter createRecordWriter(HiveEndPoint hiveEndPoint) throws StreamingException, IOException, ClassNotFoundException {
      List<String> result = fields.stream().map(String::toLowerCase).collect(Collectors.toList());
      return new DelimitedInputWriter(result.toArray(new String[0]), fieldDelimiter, hiveEndPoint);
    }

    @Override
    public void write(TransactionBatch transactionBatch, Tuple tuple) throws StreamingException, IOException, InterruptedException {
        transactionBatch.write(mapRecord(tuple));
    }

    @Override
    public List<String> mapPartitions(Tuple tuple) {
        StreamlineEvent event = (StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        List<String> partitionList = new ArrayList<>();
        for (String field : this.partitionFields) {
            Object val = event.get(field);
            if (val == null) {
                throw new IllegalArgumentException(String.format("Partition field '%s' value is missing in the streamline event", field));
            }
            partitionList.add(val.toString());
        }
        if (this.timeFormat != null) {
            partitionList.add(parseDate.format(System.currentTimeMillis()));
        }
        return partitionList;
    }

    @Override
    public byte[] mapRecord(Tuple tuple) {
        StreamlineEvent event = (StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        StringBuilder builder = new StringBuilder();
        for (String field : this.fields) {
            Object val = event.get(field);
            if (val == null) {
                throw new IllegalArgumentException(String.format("Field '%s' value is missing in the streamline event", field));
            }
            builder.append(val);
            builder.append(fieldDelimiter);
        }
        return builder.toString().getBytes();
    }

    @Override
    public List<String> mapPartitions(TridentTuple tridentTuple) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public byte[] mapRecord(TridentTuple tridentTuple) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setFieldDelimiter(String fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
        parseDate = new SimpleDateFormat(timeFormat);
    }

}
