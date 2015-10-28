package com.hortonworks.hdfs;

import backtype.storm.tuple.Tuple;
import com.hortonworks.bolt.ParserBolt;
import org.apache.storm.hdfs.bolt.format.RecordFormat;

/**
 * Implementation of RecordFormat for unparsed tuples that need to be written
 * in to hdfs. Plainly returns the byte array field from the tuple
 * DelimtiedRecordFormat implementation does not work for byte arrays
 */

public class IdentityHdfsRecordFormat implements RecordFormat {
    @Override
    public byte[] format(Tuple tuple) {
        byte[] data = tuple.getBinaryByField(ParserBolt.BINARY_BYTES);
        byte[] recordDelimter = "\n".getBytes();
        byte[] result = new byte[data.length + recordDelimter.length];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(recordDelimter, 0, result, data.length,
                recordDelimter.length);
        return result;
    }
}
