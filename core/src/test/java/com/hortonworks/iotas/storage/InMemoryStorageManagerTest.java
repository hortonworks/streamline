package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.parser.ParserInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class InMemoryStorageManagerTest {

    private StorageManager storageManager;

    @Before
    public void setup() {
        this.storageManager = new InMemoryStorageManager();
    }

    @Test
    public void testCrud() {
        ParserInfo parserInfo1 = new ParserInfo.ParserInfoBuilder()
                .setParserId("Id-1")
                .setParserName("test-parser")
                .setClassName("com.org.apache.TestParser")
                .setJarStoragePath("/tmp/parser.jar")
                .setTimeStamp(System.currentTimeMillis()).createParserInfo();

        storageManager.add(parserInfo1);
        ParserInfo parserInfo2 = storageManager.get(parserInfo1.getNameSpace(), parserInfo1.getId(), ParserInfo.class);
        Assert.assertEquals(parserInfo1, parserInfo2);

        ParserInfo parserInfo3 = new ParserInfo.ParserInfoBuilder()
                .setParserId("Id-3")
                .setParserName("test-parser")
                .setClassName("com.org.apache.TestParser")
                .setJarStoragePath("/tmp/parser.jar")
                .setTimeStamp(System.currentTimeMillis()).createParserInfo();
        storageManager.addOrUpdate(parserInfo3);
        parserInfo2 = storageManager.get(parserInfo3.getNameSpace(), parserInfo3.getId(), ParserInfo.class);
        Assert.assertEquals(parserInfo3, parserInfo2);

        ParserInfo parserInfo4 = new ParserInfo.ParserInfoBuilder()
                .setParserId("Id-1")
                .setParserName("test-parser-4")
                .setClassName("com.org.apache.TestParser-4")
                .setJarStoragePath("/tmp/parser-4.jar")
                .setTimeStamp(System.currentTimeMillis()).createParserInfo();
        storageManager.addOrUpdate(parserInfo4);
        parserInfo2 = storageManager.get(parserInfo4.getNameSpace(), parserInfo4.getId(), ParserInfo.class);
        Assert.assertEquals(parserInfo4, parserInfo2);

        storageManager.remove(parserInfo1.getNameSpace(), parserInfo1.getId());
        Assert.assertNull(storageManager.get(parserInfo1.getNameSpace(), parserInfo1.getId(), ParserInfo.class));

        ParserInfo parserInfo5 = new ParserInfo.ParserInfoBuilder()
                .setParserId("Id-5")
                .setParserName("test-parser-5")
                .setClassName("com.org.apache.TestParser-5")
                .setJarStoragePath("/tmp/parser-5.jar")
                .setTimeStamp(System.currentTimeMillis()).createParserInfo();
        storageManager.add(parserInfo5);

        Set<ParserInfo> parserInfoSet = new HashSet<ParserInfo>();
        parserInfoSet.add(parserInfo3);
        parserInfoSet.add(parserInfo5);
        Assert.assertEquals(parserInfoSet, new HashSet<ParserInfo>(storageManager.list(parserInfo1.getNameSpace(), ParserInfo.class)));

    }
}
