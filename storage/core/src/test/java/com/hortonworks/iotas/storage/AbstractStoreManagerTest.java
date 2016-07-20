package com.hortonworks.iotas.storage;

import com.google.common.collect.Lists;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.Schema;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
        for (StorableTest test : storableTests ){
            test.setStorageManager(getStorageManager());
        }
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
    //@Test(expected = AlreadyExistsException.class)
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
    //@Test
    public void testAdd_EqualExistingStorable_NoOperation() {
        for (StorableTest test : storableTests) {
            Storable storable1 = test.getStorableList().get(0);
            getStorageManager().add(storable1);
            getStorageManager().add(storable1);     // should throw exception
            Assert.assertEquals(storable1, getStorageManager().get(storable1.getStorableKey()));
        }
    }

    //@Test
    public void testRemove_NonExistentStorable_null() {
        for (StorableTest test : storableTests) {
            Storable removed = getStorageManager().remove(test.getStorableList().get(0).getStorableKey());
            Assert.assertNull(removed);
        }
    }

    //@Test(expected = StorageException.class)
    public void testList_NonexistentNameSpace_StorageException() {
        Collection<Storable> found = getStorageManager().list("NONEXISTENT_NAME_SPACE");
    }

    //@Test
    public void testFind_NullQueryParams_AllEntries() {
        for (StorableTest test : storableTests) {
            test.addAllToStorage();
            Collection<Storable> allExisting = getStorageManager().list(test.getNameSpace());
            Collection<Storable> allMatchingQueryParamsFilter = getStorageManager().find(test.getNameSpace(), null);
            Assert.assertEquals(allExisting, allMatchingQueryParamsFilter);
        }
    }

    //@Test
    public void testFind_NonExistentQueryParams_EmptyList() {
        for (StorableTest test : storableTests) {
            test.addAllToStorage();
            List<QueryParam> queryParams = new ArrayList<QueryParam>() {
                {
                    add(new QueryParam("NON_EXISTING_FIELD_1", "NON_EXISTING_VAL_1"));
                    add(new QueryParam("NON_EXISTING_FIELD_2", "NON_EXISTING_VAL_2"));
                }
            };

            final Collection<Storable> allMatchingQueryParamsFilter = getStorageManager().find(test.getNameSpace(), queryParams);
            Assert.assertEquals(Collections.EMPTY_LIST, allMatchingQueryParamsFilter);
        }
    }

    //@Test
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
}
