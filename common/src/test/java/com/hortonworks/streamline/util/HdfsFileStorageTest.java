/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/

package com.hortonworks.streamline.util;

import com.hortonworks.streamline.common.util.HdfsFileStorage;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class HdfsFileStorageTest {
    private static final String HDFS_DIR = "/tmp/test-hdfs";
    private HdfsFileStorage fileStorage;

    @Before
    public void setUp() throws Exception {
        fileStorage = new HdfsFileStorage();
    }

    @Test(expected = RuntimeException.class)
    public void testInitWithoutFsUrl() throws Exception {
        fileStorage.init(new HashMap<String, String>());
    }

    @Test
    public void testUploadJar() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put(HdfsFileStorage.CONFIG_FSURL, "file:///");
        fileStorage.init(config);

        File file = File.createTempFile("test", ".tmp");
        file.deleteOnExit();

        List<String> lines = Arrays.asList("test-line-1", "test-line-2");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));
        String jarFileName = "test.jar";

        fileStorage.delete(jarFileName);

        fileStorage.upload(new FileInputStream(file), jarFileName);

        InputStream inputStream = fileStorage.download(jarFileName);
        List<String> actual = IOUtils.readLines(inputStream);
        Assert.assertEquals(lines, actual);
    }

    @Test
    public void testUploadJarWithDir() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put(HdfsFileStorage.CONFIG_FSURL, "file:///");
        config.put(HdfsFileStorage.CONFIG_DIRECTORY, HDFS_DIR);
        fileStorage.init(config);

        File file = File.createTempFile("test", ".tmp");
        file.deleteOnExit();

        List<String> lines = Arrays.asList("test-line-1", "test-line-2");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));
        String jarFileName = "test.jar";

        fileStorage.delete(jarFileName);

        fileStorage.upload(new FileInputStream(file), jarFileName);

        InputStream inputStream = fileStorage.download(jarFileName);
        List<String> actual = IOUtils.readLines(inputStream);
        Assert.assertEquals(lines, actual);
    }

}
