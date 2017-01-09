package org.apache.streamline.streams.runtime.storm.hdfs;

import org.apache.streamline.streams.StreamlineEvent;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.tuple.Tuple;

/**
 * Implementation of RecordFormat for unparsed tuples that need to be written
 * in to hdfs. Plainly returns the byte array field from the tuple
 * DelimitedRecordFormat implementation does not work for byte arrays
 */

public class IdentityHdfsRecordFormat implements RecordFormat {
    @Override
    public byte[] format(Tuple tuple) {
        byte[] data = ((StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT)).getBytes();
        byte[] recordDelimiter = "\n".getBytes();
        byte[] result = new byte[data.length + recordDelimiter.length];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(recordDelimiter, 0, result, data.length,
                recordDelimiter.length);
        return result;
    }
}
