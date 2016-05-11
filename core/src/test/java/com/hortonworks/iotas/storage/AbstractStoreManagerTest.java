package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.Device;
import com.hortonworks.iotas.catalog.File;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.catalog.Tag;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.storage.exception.AlreadyExistsException;
import com.hortonworks.iotas.storage.exception.StorageException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;

public abstract class AbstractStoreManagerTest {
    protected static final Logger log = LoggerFactory.getLogger(AbstractStoreManagerTest.class);

    @Rule
    public TestName testName = new TestName();

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        public void starting(final Description method) {
            log.info("RUNNING TEST [{}] ", method.getMethodName());
        }
    };

    // To test a new Storable entity type, add it to this list in the implementation of the method setStorableTests
    protected List<StorableTest> storableTests;

    @Before
    public void setup() {
        setStorableTests();
    }

    // Method that sets the list of CRUD tests to be run
    protected abstract void setStorableTests();

    /**
     * @return When we add a new implementation for StorageManager interface we will also add a corresponding test implementation
     * which will extends this class and implement this method.
     * <p/>
     * Essentially we are going to run the same test defined in this class for every single implementation of StorageManager.
     */
    protected abstract StorageManager getStorageManager();

    // ================ TEST METHODS ================
    // Test methods use the widely accepted naming convention  [UnitOfWork_StateUnderTest_ExpectedBehavior]

    @Test
    public void testCrud_AllStorableEntities_NoExceptions() {
        for (StorableTest test : storableTests) {
            try {
                test.init();
                test.test();
            } finally {
                test.close();
            }
        }
    }

    // UnequalExistingStorable => Storable that has the same StorableKey but does NOT verify .equals()
    @Test(expected = AlreadyExistsException.class)
    public void testAdd_UnequalExistingStorable_AlreadyExistsException() {
        for (StorableTest test : storableTests) {
            Storable storable1 = test.getStorableList().get(0);
            Storable storable2 = test.getStorableList().get(1);
            Assert.assertEquals(storable1.getStorableKey(), storable2.getStorableKey());
            Assert.assertNotEquals(storable1, storable2);
            getStorageManager().add(storable1);
            getStorageManager().add(storable2);     // should throw exception
        }
    }

    // EqualExistingStorable => Storable that has the same StorableKey and verifies .equals()
    @Test
    public void testAdd_EqualExistingStorable_NoOperation() {
        for (StorableTest test : storableTests) {
            Storable storable1 = test.getStorableList().get(0);
            getStorageManager().add(storable1);
            getStorageManager().add(storable1);     // should throw exception
            Assert.assertEquals(storable1, getStorageManager().get(storable1.getStorableKey()));
        }
    }

    @Test
    public void testRemove_NonExistentStorable_null() {
        for (StorableTest test : storableTests) {
            Storable removed = getStorageManager().remove(test.getStorableList().get(0).getStorableKey());
            Assert.assertNull(removed);
        }
    }

    @Test(expected = StorageException.class)
    public void testList_NonexistentNameSpace_StorageException() {
        Collection<Storable> found = getStorageManager().list("NONEXISTENT_NAME_SPACE");
    }

    @Test
    public void testFind_NullQueryParams_AllEntries() {
        for (StorableTest test : storableTests) {
            test.addAllToStorage();
            Collection<Storable> allExisting = getStorageManager().list(test.getNameSpace());
            Collection<Storable> allMatchingQueryParamsFilter = getStorageManager().find(test.getNameSpace(), null);
            Assert.assertEquals(allExisting, allMatchingQueryParamsFilter);
        }
    }

    @Test
    public void testFind_NonExistentQueryParams_EmptyList() {
        for (StorableTest test : storableTests) {
            test.addAllToStorage();
            List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>() {
                {
                    add(new CatalogService.QueryParam("NON_EXISTING_FIELD_1", "NON_EXISTING_VAL_1"));
                    add(new CatalogService.QueryParam("NON_EXISTING_FIELD_2", "NON_EXISTING_VAL_2"));
                }
            };

            final Collection<Storable> allMatchingQueryParamsFilter = getStorageManager().find(test.getNameSpace(), queryParams);
            Assert.assertEquals(Collections.EMPTY_LIST, allMatchingQueryParamsFilter);
        }
    }

    @Test
    public void testNextId_AutoincrementColumn_IdPlusOne() throws Exception {
        for (StorableTest test : storableTests) {
            // Device does not have auto_increment, and therefore there is no concept of nextId and should throw exception
            doTestNextId_AutoincrementColumn_IdPlusOne(test);
        }
    }

    protected void doTestNextId_AutoincrementColumn_IdPlusOne(StorableTest test) throws SQLException {
        Long actualNextId = getStorageManager().nextId(test.getNameSpace());
        Long expectedNextId = actualNextId;
        Assert.assertEquals(expectedNextId, actualNextId);
        addAndAssertNextId(test, 0, ++expectedNextId);
        addAndAssertNextId(test, 2, ++expectedNextId);
        addAndAssertNextId(test, 2, expectedNextId);
        addAndAssertNextId(test, 3, ++expectedNextId);
    }

    protected void addAndAssertNextId(StorableTest test, int idx, Long expectedId) throws SQLException {
        getStorageManager().addOrUpdate(test.getStorableList().get(idx));
        Long nextId = getStorageManager().nextId(test.getNameSpace());
        Assert.assertEquals(expectedId, nextId);
    }

    // ============= Inner classes that represent the list of Storable entities to be tested =================

    protected class StorableTest {
        protected List<Storable> storableList;

        /**
         * Performs any initialization steps that are required to test this storable instance, for example,
         * initialize the a parent table that a child table refers to (e.g. DataSource and Device)
         */
        public void init() { }

        /**
         * Each of the storable entities has its own list and the 0th and 1st index items in that list has same id so
         * test will use that to test the update operation. the 3rd item is inserted at storage layer and 4th i
         */
        public void test() {
            final Storable storable1 = storableList.get(0);
            final Storable storable2 = storableList.get(1);
            final Storable storable3 = storableList.get(2);
            final Storable storable4 = storableList.get(3);
            String namespace = storable1.getNameSpace();

            // test get nonexistent key
            Assert.assertNull(getStorageManager().get(storable1.getStorableKey()));

            //test add, get by inserting the first item in list.
            getStorageManager().add(storable1);
            Assert.assertEquals(storable1, getStorageManager().get(storable1.getStorableKey()));

            //test update by calling addOrUpdate on second item which should have the same primary key value as the first item.
            Assert.assertEquals(storable1.getPrimaryKey(), storable2.getPrimaryKey());
            getStorageManager().addOrUpdate(storable2);
            Assert.assertEquals(storable2, getStorageManager().get(storable2.getStorableKey()));

            //add 3rd item, only added so that list operation will return more then one item.
            getStorageManager().addOrUpdate(storable3);
            Assert.assertEquals(storable3, getStorageManager().get(storable3.getStorableKey()));

            //test remove by adding 4th item and removing it.
            getStorageManager().addOrUpdate(storable4);
            Assert.assertEquals(storable4, getStorageManager().get(storable4.getStorableKey()));
            Storable removed = getStorageManager().remove(storable4.getStorableKey());
            Assert.assertNull(getStorageManager().get(storable4.getStorableKey()));
            // check that the correct removed item gets returned
            Assert.assertEquals(storable4, removed);

            //Test list method. The storage layer should have the 2nd item (updated version of 1st item) and 3rd Item.
            final Set<Storable> expected = new HashSet<Storable>() {{
                add(storable2);
                add(storable3);
            }};
            final HashSet allExisting = new HashSet(getStorageManager().list(getNameSpace()));
            Assert.assertEquals(expected, allExisting);

            //Test method with query parameters(filter) matching only the item storable3
            final Collection<Storable> found = getStorageManager().find(namespace, buildQueryParamsForPrimaryKey(storable3));
            Assert.assertEquals(1, found.size());
            Assert.assertTrue(found.contains(storable3));
        }

        public void close() {
            getStorageManager().cleanup();
        }

        public List<Storable> getStorableList() {
            return storableList;
        }

        public void addAllToStorage() {
            for (Storable storable : storableList) {
                getStorageManager().addOrUpdate(storable);
            }
        }

        protected List<CatalogService.QueryParam> buildQueryParamsForPrimaryKey(Storable storable) {
            final Map<Schema.Field, Object> fieldsToVal = storable.getPrimaryKey().getFieldsToVal();
            final List<CatalogService.QueryParam> queryParams = new ArrayList<>(fieldsToVal.size());

            for (Schema.Field field : fieldsToVal.keySet()) {
                CatalogService.QueryParam qp = new CatalogService.QueryParam(field.getName(), fieldsToVal.get(field).toString());
                queryParams.add(qp);
            }

            return queryParams;
        }

        public String getNameSpace() {
            return storableList.get(0).getStorableKey().getNameSpace();
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

    public class FilesTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                add(createFiles(1l, "file-1"));
                add(createFiles(1l, "file-2"));
                add(createFiles(2l, "file-3"));
                add(createFiles(3l, "file-4"));
            }};
        }

        protected File createFiles(Long id, String name) {
            File file = new File();
            file.setId(id);
            file.setName(name);
            file.setStoredFileName("/tmp/parser.jar");
            file.setVersion(0l);
            file.setTimestamp(System.currentTimeMillis());
            return file;
        }
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
}
