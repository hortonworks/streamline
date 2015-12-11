package com.hortonworks.util;

import com.google.common.collect.Lists;
import com.hortonworks.iotas.util.JarStorage;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

public abstract class AbstractJarStorageTest {


    public abstract JarStorage getJarStorage();

    @Test
    public void testJarStorage() throws IOException {
        JarStorage jarStorage = getJarStorage();
        File file = File.createTempFile("test", ".tmp");
        file.deleteOnExit();
        List<String> lines = Lists.newArrayList("test-line-1", "test-line-2");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));
        String name = "file.name";
        jarStorage.uploadJar(new FileInputStream(file), name);
        InputStream inputStream = jarStorage.downloadJar(name);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader
                (inputStream));
        String nextLine;
        List<String> actual = Lists.newArrayList();
        while((nextLine = bufferedReader.readLine()) != null) {
            actual.add(nextLine);
        }
        Assert.assertEquals(lines, actual);

    }
}
