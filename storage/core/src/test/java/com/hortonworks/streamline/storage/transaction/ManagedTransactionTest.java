package com.hortonworks.streamline.storage.transaction;

import com.hortonworks.streamline.common.transaction.TransactionIsolation;
import com.hortonworks.streamline.storage.TransactionManager;
import com.hortonworks.streamline.storage.exception.IgnoreTransactionRollbackException;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMockit.class)
public class ManagedTransactionTest {
    @Injectable
    private TransactionManager mockedTransactionManager;

    @Injectable
    private TransactionIsolation transactionIsolation;

    @Tested
    private ManagedTransaction managedTransaction;

    private ManagedTransactionTestHelper testHelper;

    private final String testParam1 = "testParam1";
    private final String testParam2 = "testParam2";
    private final String testParam3 = "testParam3";
    private final String testParam4 = "testParam4";
    private final String testParam5 = "testParam5";

    @Before
    public void setUp() throws Exception {
        testHelper = new ManagedTransactionTestHelper();
    }

    @Test
    public void testExecuteFunctionArg0SuccessCase() throws Exception {
        Integer result = managedTransaction.executeFunction(this::callHelperWithReturn);
        Assert.assertEquals(Integer.valueOf(1), result);
        verifyInteractionWithTransactionManagerSuccessCase();
    }

    @Test
    public void testExecuteFunctionArg1SuccessCase() throws Exception {
        Integer result = managedTransaction.executeFunction(this::callHelperWithReturn, testParam1);
        Assert.assertEquals(Integer.valueOf(1), result);
        verifyInteractionWithTransactionManagerSuccessCase(testParam1);
    }

    @Test
    public void testExecuteFunctionArg2SuccessCase() throws Exception {
        Integer result = managedTransaction.executeFunction(this::callHelperWithReturn, testParam1, testParam2);
        Assert.assertEquals(Integer.valueOf(1), result);
        verifyInteractionWithTransactionManagerSuccessCase(testParam1, testParam2);
    }

    @Test
    public void testExecuteFunctionArg3SuccessCase() throws Exception {
        Integer result = managedTransaction.executeFunction(this::callHelperWithReturn, testParam1, testParam2, testParam3);
        Assert.assertEquals(Integer.valueOf(1), result);
        verifyInteractionWithTransactionManagerSuccessCase(testParam1, testParam2, testParam3);
    }

    @Test
    public void testExecuteFunctionArg4SuccessCase() throws Exception {
        Integer result = managedTransaction.executeFunction(this::callHelperWithReturn, testParam1, testParam2, testParam3,
                testParam4);
        Assert.assertEquals(Integer.valueOf(1), result);
        verifyInteractionWithTransactionManagerSuccessCase(testParam1, testParam2, testParam3,
                testParam4);
    }

    @Test
    public void testExecuteFunctionArg5SuccessCase() throws Exception {
        Integer result = managedTransaction.executeFunction(this::callHelperWithReturn, testParam1, testParam2, testParam3,
                testParam4, testParam5);
        Assert.assertEquals(Integer.valueOf(1), result);
        verifyInteractionWithTransactionManagerSuccessCase(testParam1, testParam2, testParam3,
                testParam4, testParam5);
    }

    @Test
    public void testExecuteFunctionRollbackCase() {
        try {
            managedTransaction.executeFunction(this::callHelperWithThrowException);
            Assert.fail("It should propagate Exception!");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ManagedTransactionTestHelper.IntendedException);
            verifyInteractionWithTransactionManagerWithExceptionCase();
        }
    }

    @Test
    public void testExecuteFunctionIgnoreRollbackCase() {
        try {
            managedTransaction.executeFunction(this::callHelperWithThrowIgnoreRollbackException);
            Assert.fail("It should propagate Exception!");
        } catch (Exception e) {
            // it should propagate cause, not exception itself
            Assert.assertFalse(e instanceof IgnoreTransactionRollbackException);
            Assert.assertTrue(e instanceof ManagedTransactionTestHelper.IntendedException);
            verifyInteractionWithTransactionManagerWithIgnoreRollbackCase();
        }
    }

    @Test
    public void testExecuteConsumerArg0SuccessCase() throws Exception {
        managedTransaction.executeConsumer(this::callHelperWithReturn);
        verifyInteractionWithTransactionManagerSuccessCase();
    }

    @Test
    public void testExecuteConsumerArg1SuccessCase() throws Exception {
        managedTransaction.executeConsumer(this::callHelperWithReturn, testParam1);
        verifyInteractionWithTransactionManagerSuccessCase(testParam1);
    }

    @Test
    public void testExecuteConsumerArg2SuccessCase() throws Exception {
        managedTransaction.executeConsumer(this::callHelperWithReturn, testParam1, testParam2);
        verifyInteractionWithTransactionManagerSuccessCase(testParam1, testParam2);
    }

    @Test
    public void testExecuteConsumerArg3SuccessCase() throws Exception {
        managedTransaction.executeConsumer(this::callHelperWithReturn, testParam1, testParam2, testParam3);
        verifyInteractionWithTransactionManagerSuccessCase(testParam1, testParam2, testParam3);
    }

    @Test
    public void testExecuteConsumerArg4SuccessCase() throws Exception {
        managedTransaction.executeConsumer(this::callHelperWithReturn, testParam1, testParam2, testParam3, testParam4);
        verifyInteractionWithTransactionManagerSuccessCase(testParam1, testParam2, testParam3, testParam4);
    }

    @Test
    public void testExecuteConsumerArg5SuccessCase() throws Exception {
        managedTransaction.executeConsumer(this::callHelperWithReturn, testParam1, testParam2, testParam3, testParam4,
                testParam5);
        verifyInteractionWithTransactionManagerSuccessCase(testParam1, testParam2, testParam3, testParam4, testParam5);
    }

    @Test
    public void testExecuteConsumerRollbackCase() {
        try {
            managedTransaction.executeConsumer(this::callHelperWithThrowException);
            Assert.fail("It should propagate Exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ManagedTransactionTestHelper.IntendedException);
            verifyInteractionWithTransactionManagerWithExceptionCase();
        }
    }

    @Test
    public void testExecuteConsumerIgnoreRollbackCase() {
        try {
            managedTransaction.executeConsumer(this::callHelperWithThrowIgnoreRollbackException);
            Assert.fail("It should propagate Exception");
        } catch (Exception e) {
            // it should propagate cause, not exception itself
            Assert.assertFalse(e instanceof IgnoreTransactionRollbackException);
            Assert.assertTrue(e instanceof ManagedTransactionTestHelper.IntendedException);
            verifyInteractionWithTransactionManagerWithIgnoreRollbackCase();
        }
    }

    @Test
    public void testCaseCommitTransactionThrowsException() {
        final Exception commitException = new Exception("Commit exception");
        new Expectations() {{
            mockedTransactionManager.commitTransaction();
            result = commitException;
        }};

        try {
            managedTransaction.executeConsumer(this::callHelperWithReturn);
            Assert.fail("It should propagate Exception");
        } catch (Exception e) {
            Assert.assertEquals(commitException, e);
            Assert.assertTrue(testHelper.isCalled());

            new VerificationsInOrder() {{
                mockedTransactionManager.beginTransaction(transactionIsolation);
                times = 1;

                mockedTransactionManager.commitTransaction();
                times = 1;

                mockedTransactionManager.rollbackTransaction();
                times = 1;
            }};
        }
    }

    @Test
    public void testCaseCommitTransactionThrowsExceptionAfterCaseIgnoreRollbackException() {
        final Exception commitException = new Exception("Commit exception");
        new Expectations() {{
            mockedTransactionManager.commitTransaction();
            result = commitException;
        }};

        try {
            managedTransaction.executeConsumer(this::callHelperWithThrowIgnoreRollbackException);
            Assert.fail("It should propagate Exception");
        } catch (Exception e) {
            Assert.assertEquals(commitException, e);
            Assert.assertTrue(testHelper.isCalled());

            new VerificationsInOrder() {{
                mockedTransactionManager.beginTransaction(transactionIsolation);
                times = 1;

                mockedTransactionManager.commitTransaction();
                times = 1;

                mockedTransactionManager.rollbackTransaction();
                times = 1;
            }};
        }
    }

    @Test
    public void testCaseRollbackTransactionThrowsException() {
        final Exception rollbackException = new Exception("Rollback exception");
        new Expectations() {{
            mockedTransactionManager.rollbackTransaction();
            result = rollbackException;
        }};

        try {
            managedTransaction.executeConsumer(this::callHelperWithThrowException);
            Assert.fail("It should propagate Exception");
        } catch (Exception e) {
            Assert.assertEquals(rollbackException, e);
            Assert.assertTrue(testHelper.isCalled());

            new VerificationsInOrder() {{
                mockedTransactionManager.beginTransaction(transactionIsolation);
                times = 1;

                mockedTransactionManager.commitTransaction();
                times = 0;

                mockedTransactionManager.rollbackTransaction();
                times = 1;
            }};
        }
    }

    private Integer callHelperWithReturn(Object...args) {
        testHelper.call(args);
        return 1;
    }

    private Integer callHelperWithThrowException(Object...args) throws Exception {
        testHelper.throwException(args);
        return 1;
    }

    private Integer callHelperWithThrowIgnoreRollbackException(Object...args) throws Exception {
        testHelper.throwIgnoreRollbackException(args);
        return 1;
    }

    private void verifyInteractionWithTransactionManagerSuccessCase(Object...args) {
        Assert.assertTrue(testHelper.isCalled());

        Object[] actualArgs = testHelper.getCalledArgs();
        Assert.assertArrayEquals(args, actualArgs);

        new VerificationsInOrder() {{
            mockedTransactionManager.beginTransaction(transactionIsolation);
            times = 1;

            mockedTransactionManager.commitTransaction();
            times = 1;

            mockedTransactionManager.rollbackTransaction();
            times = 0;
        }};
    }

    private void verifyInteractionWithTransactionManagerWithExceptionCase(Object...args) {
        Assert.assertTrue(testHelper.isCalled());

        Object[] actualArgs = testHelper.getCalledArgs();
        Assert.assertArrayEquals(args, actualArgs);

        new VerificationsInOrder() {{
            mockedTransactionManager.beginTransaction(transactionIsolation);
            times = 1;

            mockedTransactionManager.commitTransaction();
            times = 0;

            mockedTransactionManager.rollbackTransaction();
            times = 1;
        }};
    }

    private void verifyInteractionWithTransactionManagerWithIgnoreRollbackCase(Object...args) {
        Assert.assertTrue(testHelper.isCalled());

        Object[] actualArgs = testHelper.getCalledArgs();
        Assert.assertArrayEquals(args, actualArgs);

        new VerificationsInOrder() {{
            mockedTransactionManager.beginTransaction(transactionIsolation);
            times = 1;

            mockedTransactionManager.commitTransaction();
            times = 1;

            mockedTransactionManager.rollbackTransaction();
            times = 0;
        }};
    }
}
