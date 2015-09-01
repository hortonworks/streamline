package com.hortonworks.topology;

import backtype.storm.tuple.Tuple;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by pshah on 8/26/15.
 * An implementation of UnparsedTupleHandler that will save all the unparsed
 * tuples to hdfs
 */
public class HdfsUnparsedTupleHandler implements UnparsedTupleHandler {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(HdfsUnparsedTupleHandler.class);
    protected String fsUrl;
    protected String path;
    protected String name;
    protected Long rotationInterval;
    protected long rotation = 0;
    protected transient FileSystem fileSystem;
    private transient FSDataOutputStream out;
    private String recordDelimiter = "\n";
    protected transient AtomicBoolean shouldRotate;

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

    /**
     *
     * @param rotationIntervalSeconds is the interval in seconds at which the files
     *                         should be rotated. Note that the largest
     *                         supported value is Math.floor(Long
     *                         .MAX_VALUE/1000)
     * @return
     */
    public HdfsUnparsedTupleHandler withRotationInterval (long rotationIntervalSeconds) {
        this.rotationInterval = (rotationIntervalSeconds * 1000);
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
        // Changing from lock to AtomicBoolean as per comment on code review.
        // Note that this might miss a rotation since if the timer thread
        // gets swapped in after rotateOutputFile is executed and before
        // shouldRotate.set(false) is called. However, this will not cause
        // any data loss. Just the number of output files will be one less
        // every time this happens.
        if (shouldRotate.get()) {
            rotateOutputFile();
            shouldRotate.set(false);
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
        shouldRotate = new AtomicBoolean();
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
        Path path =  new Path(this.path, this.name + "-" + rotation);
        this.out = this.fileSystem.create(path);
        if (this.rotationInterval != null) {
            Timer timer = new Timer(true);
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    shouldRotate.set(true);
                }
            };
            timer.scheduleAtFixedRate(timerTask, this.rotationInterval, this.rotationInterval);
        }

    }

    /**
     * cleanup underlying hdfs output stream
     */
    public void cleanup () throws IOException {
        if (this.out != null) {
            this.out.close();
        }
    }

    protected void rotateOutputFile () throws IOException {
        cleanup();
        rotation++;
        Path path =  new Path(this.path, this.name + "-" + rotation);
        this.out = this.fileSystem.create(path);
    }
}
