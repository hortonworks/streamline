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
package com.hortonworks.streamline.streams.runtime.storm.spout;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.storm.hdfs.spout.ParseException;
import org.apache.storm.hdfs.spout.TextFileReader;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Converts each JSON line in the Text file into a SreamlineEvent based tuple
 */

public class JsonFileReader extends TextFileReader {
    public JsonFileReader(FileSystem fs, Path file, Map conf) throws IOException {
        super(fs, file, conf);
    }

    public JsonFileReader(FileSystem fs, Path file, Map conf, String startOffset) throws IOException {
        super(fs, file, conf, startOffset);
    }

    public List<Object> next() throws IOException, ParseException {
        List<Object> lineTuple = super.next();
        String jsonLine = (String) lineTuple.get(0);
        if ( jsonLine==null )
            return null;
        if ( jsonLine.trim().isEmpty() )
            return next();

        try {
            //1- convert Json to Map<>
            HashMap<String, Object> jsonMap = new ObjectMapper().readValue(jsonLine, HashMap.class);

            //2- make StreamlineEvent from map
            StreamlineEventImpl slEvent = new StreamlineEventImpl(jsonMap, "HdfsSpout");

            //3- create tuple from StreamlineEvent
            return Collections.singletonList(slEvent);
        } catch (JsonProcessingException e) {
            throw new ParseException("Json parsing error at location : " + getFileOffset().toString(), e);
        }

    }
}
