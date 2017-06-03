/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.hortonworks.streamline.streams.runtime.storm.hdfs;

import com.hortonworks.streamline.streams.runtime.storm.spout.JsonFileReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TestJsonFileReader {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testRead() throws Exception {
        String file = tmp.newFile().getAbsolutePath();

        JsonFileReader reader = new JsonFileReader(LocalFileSystem.get(new Configuration()), new Path(file), null);
        String jsonStr  = "{ \"truckId\" : 10, \"driverId\" : 20 }\n" +
                          "{ \"truckId\" : 11, \"driverId\" : 21 }\n" +
                          "{ \"truckId\" : 12, \"driverId\" : 22 }\n" +
                          "{ \"truckId\" : 13, \"driverId\" : 23 }\n" +
                          "{ \"truckId\" : 14, \"driverId\" : 24 }\n" +
                          "{ \"truckId\" : 15, \"driverId\" : 25 }\n" +
                          "{ \"truckId\" : 16, \"driverId\" : 26 }";


        dumpToFile(jsonStr, file);

        int lineCount = 0;
        List<Object> x = reader.next();
        while( x!=null ) {
            System.err.println(x.get(0));
            x = reader.next();
            ++lineCount;
        }
        Assert.assertEquals(7, lineCount);
    }

    private static void dumpToFile(String jsonStr, String file) throws IOException {
        FileWriter fw = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fw);
        out.write(jsonStr);
        out.close();
        fw.close();
    }
}
