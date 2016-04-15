/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.util;

import com.google.common.io.ByteStreams;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;


/**
 * HDFS based implementation for storing files.
 *
 */
public class HdfsFileStorage implements FileStorage {

    // the configuration keys
    public static final String CONFIG_FSURL = "fsUrl";
    public static final String CONFIG_DIRECTORY = "directory";

    private String fsUrl;
    private String directory = DEFAULT_DIR;
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
            throw new RuntimeException("fsUrl must be specified for HdfsFileStorage.");
        }

        try {
            hdfsFileSystem = FileSystem.get(URI.create(fsUrl), hdfsConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String uploadFile(InputStream inputStream, String name) throws IOException {
        Path jarPath = new Path(directory, name);

        try(FSDataOutputStream outputStream = hdfsFileSystem.create(jarPath, false)) {
            ByteStreams.copy(inputStream, outputStream);
        }

        return jarPath.toString();
    }

    @Override
    public InputStream downloadFile(String name) throws IOException {
        Path filePath = new Path(directory, name);
        return hdfsFileSystem.open(filePath);
    }

    @Override
    public boolean deleteFile(String name) throws IOException {
        return hdfsFileSystem.delete(new Path(directory, name), true);
    }
}
