package com.hortonworks.iotas.notification.service;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.Notifier;
import com.hortonworks.iotas.notification.common.NotifierConfig;
import com.hortonworks.iotas.notification.store.Criteria;
import com.hortonworks.iotas.notification.store.NotificationStore;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.util.ReflectionHelper;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class NotificationServiceImplTest {

    @Mocked
    ReflectionHelper mockReflectionHelper;

    @Mocked
    NotificationStore mockNotificationStore;

    @Mocked
    NotificationContext mockCtx;

    @Mocked
    NotifierConfig mockNotifierConfig;

    @Mocked
    Notifier mockNotifier;

    @Mocked
    Notification mockNotification;

    NotificationServiceImpl notificationService;

    @Before
    public void setUp() throws Exception {
        notificationService = new NotificationServiceImpl(mockNotificationStore);
    }

    @Test
    public void testRegister() throws Exception {
        new Expectations() {
            {
                mockCtx.getConfig();
                times = 2;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                ReflectionHelper.isJarInClassPath(anyString);
                result = true;
                ReflectionHelper.newInstance(anyString);
                result = mockNotifier;
            }
        };
        Notifier result = notificationService.register("test_notifier", mockCtx);
        assertEquals(mockNotifier, result);
        new Verifications() {
            {
                mockNotifier.open(mockCtx);
                times = 1;
            }
        };
    }

    @Test
    public void testDuplicateRegister() throws Exception {
        new Expectations() {
            {
                mockCtx.getConfig();
                times = 2;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                ReflectionHelper.isJarInClassPath(anyString);
                result = true;
                ReflectionHelper.newInstance(anyString);
                result = mockNotifier;
            }
        };

        notificationService.register("test_notifier", mockCtx);

        new Expectations() {
            {
                mockCtx.getConfig();
                times = 2;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                ReflectionHelper.isJarInClassPath(anyString);
                result = true;
                ReflectionHelper.newInstance(anyString);
                result = mockNotifier;
            }
        };
        Notifier result = notificationService.register("test_notifier", mockCtx);
        assertEquals(mockNotifier, result);
        new Verifications() {
            {
                mockNotifier.open(mockCtx);
                times = 0;
            }
        };
    }

    @Test
    public void testRemove() throws Exception {
        new Expectations() {
            {
                mockCtx.getConfig();
                times = 2;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                ReflectionHelper.isJarInClassPath(anyString);
                result = true;
                ReflectionHelper.newInstance(anyString);
                result = mockNotifier;
            }
        };

        notificationService.register("test_notifier", mockCtx);
        Notifier result = notificationService.remove("test_notifier");
        assertEquals(mockNotifier, result);
        new Verifications() {
            {
                mockNotifier.close();
                times = 1;
            }
        };

        // removing again should return null
        result = notificationService.remove("test_notifier");
        assertEquals(null, result);

    }

    @Test
    public void testNotify() throws Exception {
        new Expectations() {
            {
                mockCtx.getConfig();
                times = 2;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                ReflectionHelper.isJarInClassPath(anyString);
                result = true;
                ReflectionHelper.newInstance(anyString);
                result = mockNotifier;
            }
        };

        notificationService.register("test_notifier", mockCtx);

        notificationService.notify("test_notifier", mockNotification);

        new Verifications() {
            {
                mockNotificationStore.store(mockNotification);
                times = 1;
                mockNotifier.notify(mockNotification);
                times = 1;
            }
        };
    }

    @Test(expected = NoSuchNotifierException.class)
    public void testNotifyWithoutNotifier() {
        notificationService.notify("foo_notifier", mockNotification);
    }

    @Test
    public void testFindNotifications() throws Exception {
        new Expectations() {
            {
                mockCtx.getConfig();
                times = 2;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                ReflectionHelper.isJarInClassPath(anyString);
                result = true;
                ReflectionHelper.newInstance(anyString);
                result = mockNotifier;
            }
        };
        notificationService.register("test_notifier", mockCtx);

        CatalogService.QueryParam qp1 = new CatalogService.QueryParam("one", "1");
        CatalogService.QueryParam qp2 = new CatalogService.QueryParam("two", "2");
        CatalogService.QueryParam qp3 = new CatalogService.QueryParam("numRows", "5");

        notificationService.findNotifications(Arrays.asList(qp1, qp2, qp3));

        new Verifications() {
            {
                Criteria criteria;
                mockNotificationStore.findEntities(criteria = withCapture());
                //System.out.println("criteria = " + criteria);
                assertEquals(criteria.clazz(), Notification.class);
                assertEquals(criteria.numRows(), 5);
                assertEquals(criteria.fieldRestrictions().size(), 2);
                assertEquals(criteria.fieldRestrictions().get("one"), "1");
                assertEquals(criteria.fieldRestrictions().get("two"), "2");
            }
        };

        notificationService.findNotifications(Arrays.asList(qp1, qp2));

        new Verifications() {
            {
                Criteria criteria;
                mockNotificationStore.findEntities(criteria = withCapture());
                //System.out.println("criteria = " + criteria);
                assertEquals(criteria.clazz(), Notification.class);
                assertEquals(criteria.numRows(), 10);
                assertEquals(criteria.fieldRestrictions().size(), 2);
                assertEquals(criteria.fieldRestrictions().get("one"), "1");
                assertEquals(criteria.fieldRestrictions().get("two"), "2");
            }
        };
    }
}