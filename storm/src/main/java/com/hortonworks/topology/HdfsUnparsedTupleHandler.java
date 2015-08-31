package com.hortonworks.topology;

import backtype.storm.tuple.Tuple;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream;

import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

/**
 * Created by pshah on 8/26/15.
 * An implementation of UnparsedTupleHandler that will save all the unparsed
 * tuples to hdfs
 */
public class HdfsUnparsedTupleHandler implements UnparsedTupleHandler {
    protected String fsUrl;
    protected String path;
    protected String name;
    protected transient FileSystem fileSystem;
    private transient FSDataOutputStream out;
    private String recordDelimiter = "\n";

    public HdfsUnparsedTupleHandler withFsUrl (String fsUrl) {
        this.fsUrl = fsUrl;
        return this;
    }

    public HdfsUnparsedTupleHandler withPath (String path) {
        this.path = path;
        return this;
    }

    public HdfsUnparsedTupleHandler withName (String name) {
        this.name = name;
        return this;
    }

    public HdfsUnparsedTupleHandler withRecordDelimiter (String recordDelimiter) {
        this.recordDelimiter = recordDelimiter;
        return this;
    }

    /**
     * save the unparsed tuple to hdfs
     * @param data raw unparsed data
     */
    public void save(byte[] data) throws IOException {
        if ((data == null) || (data.length == 0)) {
            return;
        }
        this.out.write(data);
        this.out.write(this.recordDelimiter.getBytes());
        // calling hsynch here since this is going to be a one-off error
        // scenario and hence unlikely to affect performance
        if (this.out instanceof HdfsDataOutputStream) {
            ((HdfsDataOutputStream) this.out).hsync(EnumSet.of(HdfsDataOutputStream.SyncFlag.UPDATE_LENGTH));
        } else {
            this.out.hsync();
        }
    }

    /**
     * one time initialization for hdfs related stuff
     * @param conf map containing values that will be used in initializing hdfs
     */
    public void prepare (Map conf) throws IOException {
        if (this.fsUrl == null) {
            throw new IllegalStateException("File system URL must be specified.");
        }
        if (this.path == null) {
            throw new IllegalStateException("Path must be specified");
        }
        if (this.name == null) {
            throw new IllegalStateException("File name must be specified");
        }
        // create a unique filename to this instance of the object to avoid
        // records being intermingled from different threads
        this.name = this.name + UUID.randomUUID();
        Configuration hadoopConf = new Configuration();
        this.fileSystem = FileSystem.get(URI.create(this.fsUrl), hadoopConf);
        Path path =  new Path(this.path, this.name);
        this.out = this.fileSystem.create(path);
    }

    /**
     * cleanup underlying hdfs output stream
     */
    public void cleanup () throws IOException {
        if (this.out != null) {
            this.out.close();
        }
    }
}
