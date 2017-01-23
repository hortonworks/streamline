package com.hortonworks.streamline.streams.runtime.storm.hdfs;


import com.hortonworks.streamline.streams.StreamlineEvent;

import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;


public class HdfsTextOutputFormat implements RecordFormat {
    public static final String DEFAULT_FIELD_DELIMITER = ",";
    public static final String DEFAULT_RECORD_DELIMITER = "\n";
    private String fieldDelimiter = DEFAULT_FIELD_DELIMITER;
    private String recordDelimiter = DEFAULT_RECORD_DELIMITER;
    private Fields fields = null;

    /**
     * Only output the specified fields.
     *
     * @param commaSeparatedFields  Names of output fields, in the order they should appear in file
     * @return
     */
    public HdfsTextOutputFormat withFields(String commaSeparatedFields){
        String[] rawFields = commaSeparatedFields.split(",");
        String[] trimmedFields = new String[rawFields.length];
        for (int i = 0; i < rawFields.length; i++)   {
            trimmedFields[i] = rawFields[i].trim();
        }

        fields = new Fields(trimmedFields);
        return this;
    }

    /**
     * Overrides the default field delimiter.
     *
     * @param delimiter
     * @return
     */
    public HdfsTextOutputFormat withFieldDelimiter(String delimiter){
        this.fieldDelimiter = delimiter;
        return this;
    }

    /**
     * Overrides the default record delimiter.
     *
     * @param delimiter
     * @return
     */
    public HdfsTextOutputFormat withRecordDelimiter(String delimiter){
        this.recordDelimiter = delimiter;
        return this;
    }

    @Override
    public byte[] format(Tuple tuple) {
        if (fields==null || fields.size()==0) {
            throw new IllegalArgumentException("Output field names not specified. Set them using withFields().");
        }

        StreamlineEvent event = ((StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT));
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < fields.size(); i++) {
            Object value = event.get(fields.get(i));
            if (value!=null) {
                sb.append( value );
            }
            if ( i != fields.size()-1 ) {
                sb.append(this.fieldDelimiter);
            }
        }
        sb.append(this.recordDelimiter);
        return sb.toString().getBytes();
    }
}
