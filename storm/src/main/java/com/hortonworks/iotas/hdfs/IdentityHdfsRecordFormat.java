package com.hortonworks.iotas.hdfs;

import com.hortonworks.iotas.bolt.ParserBolt;

import org.apache.storm.tuple.Tuple;
import org.apache.storm.hdfs.bolt.format.RecordFormat;

/**
 * Implementation of RecordFormat for unparsed tuples that need to be written
 * in to hdfs. Plainly returns the byte array field from the tuple
 * DelimitedRecordFormat implementation does not work for byte arrays
 */

public class IdentityHdfsRecordFormat implements RecordFormat {
    @Override
    public byte[] format(Tuple tuple) {
        byte[] data = tuple.getBinaryByField(ParserBolt.BYTES_FIELD);
        byte[] recordDelimiter = "\n".getBytes();
        byte[] result = new byte[data.length + recordDelimiter.length];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(recordDelimiter, 0, result, data.length,
                recordDelimiter.length);
        return result;
    }
}
