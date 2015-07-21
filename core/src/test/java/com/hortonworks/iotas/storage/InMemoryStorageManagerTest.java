package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.catalog.ParserInfo;
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
                .setParserId(1l)
                .setParserName("test-parser")
                .setClassName("com.org.apache.TestParser")
                .setJarStoragePath("/tmp/parser.jar")
                .setSchema(new Schema(new Schema.Field("deviceId", Schema.Type.LONG), new Schema.Field("deviceName", Schema.Type.STRING)))
                .setVersion(0)
                .setTimeStamp(System.currentTimeMillis()).createParserInfo();

        storageManager.add(parserInfo1);
        ParserInfo parserInfo2 = storageManager.get(parserInfo1.getNameSpace(), parserInfo1.getPrimaryKey(), ParserInfo.class);
        Assert.assertEquals(parserInfo1, parserInfo2);

        ParserInfo parserInfo3 = new ParserInfo.ParserInfoBuilder()
                .setParserId(3l)
                .setParserName("test-parser")
                .setClassName("com.org.apache.TestParser")
                .setJarStoragePath("/tmp/parser.jar")
                .setSchema(new Schema(new Schema.Field("deviceId", Schema.Type.LONG), new Schema.Field("deviceName", Schema.Type.STRING)))
                .setVersion(0)
                .setTimeStamp(System.currentTimeMillis()).createParserInfo();
        storageManager.addOrUpdate(parserInfo3);
        parserInfo2 = storageManager.get(parserInfo3.getNameSpace(), parserInfo3.getPrimaryKey(), ParserInfo.class);
        Assert.assertEquals(parserInfo3, parserInfo2);

        ParserInfo parserInfo4 = new ParserInfo.ParserInfoBuilder()
                .setParserId(1l)
                .setParserName("test-parser-4")
                .setClassName("com.org.apache.TestParser-4")
                .setJarStoragePath("/tmp/parser-4.jar")
                .setSchema(new Schema(new Schema.Field("deviceId", Schema.Type.LONG), new Schema.Field("deviceName", Schema.Type.STRING)))
                .setVersion(0)
                .setTimeStamp(System.currentTimeMillis()).createParserInfo();
        storageManager.addOrUpdate(parserInfo4);
        parserInfo2 = storageManager.get(parserInfo4.getNameSpace(), parserInfo4.getPrimaryKey(), ParserInfo.class);
        Assert.assertEquals(parserInfo4, parserInfo2);

        storageManager.remove(parserInfo1.getNameSpace(), parserInfo1.getPrimaryKey());
        Assert.assertNull(storageManager.get(parserInfo1.getNameSpace(), parserInfo1.getPrimaryKey(), ParserInfo.class));

        ParserInfo parserInfo5 = new ParserInfo.ParserInfoBuilder()
                .setParserId(5l)
                .setParserName("test-parser-5")
                .setClassName("com.org.apache.TestParser-5")
                .setJarStoragePath("/tmp/parser-5.jar")
                .setSchema(new Schema(new Schema.Field("deviceId", Schema.Type.LONG), new Schema.Field("deviceName", Schema.Type.STRING)))
                .setVersion(0)
                .setTimeStamp(System.currentTimeMillis()).createParserInfo();
        storageManager.add(parserInfo5);

        Set<ParserInfo> parserInfoSet = new HashSet<ParserInfo>();
        parserInfoSet.add(parserInfo3);
        parserInfoSet.add(parserInfo5);
        Assert.assertEquals(parserInfoSet, new HashSet<ParserInfo>(storageManager.list(parserInfo1.getNameSpace(), ParserInfo.class)));

    }
}
