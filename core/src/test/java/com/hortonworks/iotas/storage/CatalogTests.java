package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.catalog.*;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.registries.tag.Tag;

import java.util.ArrayList;
import java.util.Arrays;

public class CatalogTests {


    public ArrayList<StorableTest> getAllInMemoryTests() {
        return new ArrayList<StorableTest>() {{
            add(new DataSourceTest());
            add(new ParsersTest());
            add(new DataFeedsTest());
            add(new FilesTest());
        }};
    }

    public ArrayList<StorableTest> getAllTests() {
        return new ArrayList<StorableTest>() {{
            add(new DataSourceTest());
            add(new DeviceJdbcTest());
            add(new ParsersTest());
            add(new DataFeedsJdbcTest());
            add(new FilesTest());
        }};
    }

    public class DataFeedsTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                add(createDataFeed(1l, "feed-1"));
                add(createDataFeed(1l, "feed-2"));
                add(createDataFeed(2l, "feed-3"));
                add(createDataFeed(3l, "feed-4"));
            }};
        }

        protected DataFeed createDataFeed(Long id, String name) {
            DataFeed df = new DataFeed();
            df.setId(id);
            df.setDataSourceId(1L);
            df.setName(name);
            df.setName(name);
            df.setType("KAFKA");
            df.setParserId(id);
            return df;
        }
    }

    public class DataSourceTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                add(createDataSource(1l, "datasource-1"));
                add(createDataSource(1l, "datasource-2"));
                add(createDataSource(2l, "datasource-3"));
                add(createDataSource(3l, "datasource-4"));
            }};
        }

        protected DataSource createDataSource(Long id, String name) {
            DataSource ds = new DataSource();
            ds.setId(id);
            ds.setName(name);
            ds.setDescription("desc");
            Tag tag = new Tag();
            tag.setId(1L);
            tag.setName("test-tag");
            tag.setDescription("test");
            ds.setTags(Arrays.asList(tag));
            ds.setTimestamp(System.currentTimeMillis());
            ds.setType(DataSource.Type.DEVICE);
            ds.setTypeConfig("device_type_config");
            return ds;
        }
    }

    public class DeviceTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                add(createDevice("device-1", "0", 1l));
                add(createDevice("device-1", "1", 1l));
                add(createDevice("device-2", "2", 2l));
                add(createDevice("device-3", "3", 3l));
            }};
        }

        protected Device createDevice(String make, String model, Long datafeedId) {
            Device d = new Device();
            d.setMake(make);
            d.setModel(model);
            d.setDataSourceId(datafeedId);
            return d;
        }
    }

    public class FilesTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                add(createFiles(1l, "file-1"));
                add(createFiles(1l, "file-2"));
                add(createFiles(2l, "file-3"));
                add(createFiles(3l, "file-4"));
            }};
        }

        protected FileInfo createFiles(Long id, String name) {
            FileInfo file = new FileInfo();
            file.setId(id);
            file.setName(name);
            file.setStoredFileName("/tmp/parser.jar");
            file.setVersion(0l);
            file.setTimestamp(System.currentTimeMillis());
            return file;
        }
    }


    public class ParsersTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                add(createParserInfo(1l, "parser-1"));
                add(createParserInfo(1l, "parser-2"));
                add(createParserInfo(2l, "parser-3"));
                add(createParserInfo(3l, "parser-4"));
            }};
        }

        protected ParserInfo createParserInfo(Long id, String name) {
            ParserInfo pi = new ParserInfo();
            pi.setId(id);
            pi.setName(name);
            pi.setClassName("com.org.apache.TestParser");
            pi.setJarStoragePath("/tmp/parser.jar");
            pi.setParserSchema(new Schema.SchemaBuilder().fields(new Schema.Field("deviceId", Schema.Type.LONG), new Schema.Field("deviceName", Schema.Type.STRING)).build());
            pi.setVersion(0l);
            pi.setTimestamp(System.currentTimeMillis());
            return pi;
        }
    }


    //Device has foreign key in DataSource table, which has to be initialized before we can insert data in the Device table
    class DeviceJdbcTest extends DeviceTest {
        @Override
        public void init() {
            DataSourceTest dataSourceTest  = new DataSourceTest();
            dataSourceTest.setStorageManager(getStorageManager());
            dataSourceTest.addAllToStorage();
        }
    }

    class DataFeedsJdbcTest extends DataFeedsTest {
        // DataFeed has foreign keys in ParserInfo and DataSource tables, which have to be
        // initialized before we can insert data in the DataFeed table
        @Override
        public void init() {
            ParsersTest parserTest  = new ParsersTest();
            parserTest.setStorageManager(getStorageManager());
            parserTest.addAllToStorage();

            DataSourceTest dataSourceTest  = new DataSourceTest();
            dataSourceTest.setStorageManager(getStorageManager());
            dataSourceTest.addAllToStorage();
        }
    }
}
