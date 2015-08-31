package com.hortonworks.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import com.google.common.io.ByteStreams;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;


/**
 * HDFS based implementation for storing jar files.
 *
 */
public class HdfsJarStorage implements JarStorage {

    // the configuration keys
    public static final String CONFIG_FSURL = "fsUrl";
    public static final String CONFIG_DIRECTORY = "directory";

    private String fsUrl;
    // default to /tmp
    private String directory = "/tmp";
    private FileSystem hdfsFileSystem;

    @Override
    public void init(Map<String, String> config) {
        Configuration hdfsConfig = new Configuration();
        for(Map.Entry<String, String> entry: config.entrySet()) {
            if(entry.getKey().equals(CONFIG_FSURL)) {
                this.fsUrl = config.get(CONFIG_FSURL);
            } else if(entry.getKey().equals(CONFIG_DIRECTORY)) {
                this.directory = config.get(CONFIG_DIRECTORY);
            } else {
                hdfsConfig.set(entry.getKey(), entry.getValue());
            }
        }

        // make sure fsUrl is set
        if(fsUrl == null) {
            throw new RuntimeException("fsUrl must be specified for HdfsJarStorage.");
        }

        try {
            hdfsFileSystem = FileSystem.get(URI.create(fsUrl), hdfsConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void uploadJar(InputStream inputStream, String name) throws IOException {
        Path jarPath = new Path(directory, name);
        FSDataOutputStream outputStream = null;
        try {
            outputStream = hdfsFileSystem.create(jarPath);
            ByteStreams.copy(inputStream, outputStream);
        } finally {
            if(outputStream != null) {
                outputStream.close();
            }
        }
    }

    @Override
    public InputStream downloadJar(String name) throws IOException {
        Path jarPath = new Path(directory, name);
        return hdfsFileSystem.open(jarPath);
    }
}
