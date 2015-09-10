package com.hortonworks.util;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by aiyer on 8/31/15.
 */
public class HdfsJarStorageTest {
    HdfsJarStorage jarStorage;

    @Before
    public void setUp() throws Exception {
        jarStorage = new HdfsJarStorage();
    }

    @Test(expected = RuntimeException.class)
    public void testInitWithoutFsUrl() throws Exception {
        jarStorage.init(new HashMap<String, String>());
    }

    @Test
    public void testUploadJar() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put(HdfsJarStorage.CONFIG_FSURL, "file:///");
        jarStorage.init(config);

        File file = File.createTempFile("test", ".tmp");
        file.deleteOnExit();

        List<String> lines = Arrays.asList("test-line-1", "test-line-2");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));
        String jarFileName = "test.jar";
        jarStorage.uploadJar(new FileInputStream(file), jarFileName);

        InputStream inputStream = jarStorage.downloadJar(jarFileName);
        List<String> actual = IOUtils.readLines(inputStream);
        Assert.assertEquals(lines, actual);
    }

    @Test
    public void testUploadJarWithDir() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put(HdfsJarStorage.CONFIG_FSURL, "file:///");
        config.put(HdfsJarStorage.CONFIG_DIRECTORY, "/tmp/test-hdfs");
        jarStorage.init(config);

        File file = File.createTempFile("test", ".tmp");
        file.deleteOnExit();

        List<String> lines = Arrays.asList("test-line-1", "test-line-2");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));
        String jarFileName = "test.jar";
        jarStorage.uploadJar(new FileInputStream(file), jarFileName);

        InputStream inputStream = jarStorage.downloadJar(jarFileName);
        List<String> actual = IOUtils.readLines(inputStream);
        Assert.assertEquals(lines, actual);
    }

}