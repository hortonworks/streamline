package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.Device;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.common.Schema;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractStoreManagerTest {

    //NOTE: If you are adding a new entity, create a list of 4 items where the 1st and 2nd item has same value for primary key.
    //and then add this list to storables variable defined below.

    List<Storable> parsers = new ArrayList<Storable>() {{
        add(createParserInfo(1l, "parser-1"));
        add(createParserInfo(1l, "parser-2"));
        add(createParserInfo(2l, "parser-3"));
        add(createParserInfo(3l, "parser-4"));
    }};

    List<Storable> datafeeds = new ArrayList<Storable>() {{
        add(createDataFeed(1l, "feed-1"));
        add(createDataFeed(1l, "feed-2"));
        add(createDataFeed(2l, "feed-3"));
        add(createDataFeed(3l, "feed-4"));
    }};

    List<Storable> datasources = new ArrayList<Storable>() {{
        add(createDataSource(1l, "datasource-1"));
        add(createDataSource(1l, "datasource-2"));
        add(createDataSource(2l, "datasource-3"));
        add(createDataSource(3l, "datasource-4"));
    }};

    List<Storable> devices = new ArrayList<Storable>() {{
        add(createDevice("device-1", 0l, 0l));
        add(createDevice("device-1", 0l, 1l));//deviceId and version is composite key.
        add(createDevice("device-2", 2l, 2l));
        add(createDevice("device-3", 3l, 3l));
    }};

    List<List<Storable>> storables = new ArrayList<List<Storable>>() {{
        add(parsers);
        add(datafeeds);
        add(datasources);
        add(devices);
    }};

    /**
     * @return When we add a new implementation for StorageManager interface we will also add a corresponding test implementation
     * which will extends this class and implement this method.
     *
     * Essentially we are going to run the same test defined in this class for every single implementation of StorageManager.
     */
    public abstract StorageManager getStorageManager();

    /**
     *
     * Each of the storable entities has its own list and the 0th and 1st index items in that list has same id so
     * test will use that to test the update operation. the 3rd item is inserted at storage layer and 4th i
     */
    @Test
    public void testCrudForAllEntities(){
        for(List<Storable> storableList : this.storables) {
            Storable storable1 = storableList.get(0);
            Storable storable2 = storableList.get(1);
            Storable storable3 = storableList.get(2);
            Storable storable4 = storableList.get(3);

            //test add by inserting the first item in list.
            getStorageManager().add(storable1);
            Assert.assertEquals(storable1, getStorageManager().get(storable1.getNameSpace(), storable1.getPrimaryKey()));

            //test update by calling addOrUpdate on second item which should have the same primary key value as first item.
            getStorageManager().addOrUpdate(storable2);
            Assert.assertEquals(storable2, getStorageManager().get(storable1.getNameSpace(), storable1.getPrimaryKey()));

            //add 3rd item, only added so list operation will return more then one item.
            getStorageManager().addOrUpdate(storable3);
            Assert.assertEquals(storable3, getStorageManager().get(storable3.getNameSpace(), storable3.getPrimaryKey()));

            //test remove by adding 4th item and removin it.
            getStorageManager().addOrUpdate(storable4);
            Assert.assertEquals(storable4, getStorageManager().get(storable4.getNameSpace(), storable4.getPrimaryKey()));
            getStorageManager().remove(storable4.getNameSpace(), storable4.getPrimaryKey());
            Assert.assertNull(getStorageManager().get(storable4.getNameSpace(), storable4.getPrimaryKey()));

            //The final state of storage layer should only have 2nd item (updated version of 1st item) and 3rd Item.
            Set<Storable> storableSet = new HashSet<Storable>();
            storableSet.add(storable2);
            storableSet.add(storable3);
            Assert.assertEquals(storableSet, new HashSet(getStorageManager().list(storable2.getNameSpace())));
        }
    }

    public static ParserInfo createParserInfo(Long id, String name) {
        ParserInfo pi = new ParserInfo();
        pi.setParserId(id);
        pi.setParserName(name);
        pi.setClassName("com.org.apache.TestParser");
        pi.setJarStoragePath("/tmp/parser.jar");
        pi.setSchema(new Schema(new Schema.Field("deviceId", Schema.Type.LONG), new Schema.Field("deviceName", Schema.Type.STRING)));
        pi.setVersion(0l);
        pi.setTimestamp(System.currentTimeMillis());
        return pi;
    }

    public static DataFeed createDataFeed(Long id, String name) {
        DataFeed df = new DataFeed();
        df.setDatafeedId(id);
        df.setDatafeedName(name);
        df.setDescription("desc");
        df.setEndpoint("kafka://host:port/topic");
        df.setParserId(id);
        df.setTags("a,b,c");
        df.setTimestamp(System.currentTimeMillis());
        return df;
    }

    public static DataSource createDataSource(Long id, String name) {
        DataSource ds = new DataSource();
        ds.setDatafeedId(id);
        ds.setDataSourceId(id);
        ds.setDataSourceName(name);
        ds.setDescription("desc");
        ds.setTags("t1, t2, t3");
        ds.setTimestamp(System.currentTimeMillis());
        return ds;
    }

    public static Device createDevice(String id, Long version, Long datafeedId) {
        Device d = new Device();
        d.setDeviceId(id);
        d.setVersion(version);
        d.setDataSourceId(datafeedId);
        d.setTimestamp(System.currentTimeMillis());
        return d;
    }
}
