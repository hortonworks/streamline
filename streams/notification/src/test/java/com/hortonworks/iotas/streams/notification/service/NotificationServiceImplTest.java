/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.hortonworks.iotas.streams.notification.service;

import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.util.ProxyUtil;
import com.hortonworks.iotas.streams.notification.Notification;
import com.hortonworks.iotas.streams.notification.NotificationContext;
import com.hortonworks.iotas.streams.notification.Notifier;
import com.hortonworks.iotas.streams.notification.NotifierConfig;
import com.hortonworks.iotas.streams.notification.store.Criteria;
import com.hortonworks.iotas.streams.notification.store.NotificationStore;
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
    ProxyUtil<Notifier> mockProxyUtil;

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
                times = 3;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                mockProxyUtil.loadClassFromJar("/tmp/test.jar", "Test");
                result = mockNotifier;
            }
        };
        Notifier result = notificationService.register("test_notifier", mockCtx);
        assertEquals(mockNotifier, result);
        new Verifications() {
            {
                NotificationContext ctx;
                mockNotifier.open(ctx = withCapture());
                times = 1;
                assertEquals(NotificationServiceContext.class, ctx.getClass());
            }
        };
    }

    @Test
    public void testDuplicateRegister() throws Exception {
        new Expectations() {
            {
                mockCtx.getConfig();
                times = 3;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                mockProxyUtil.loadClassFromJar("/tmp/test.jar", "Test");
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
                mockProxyUtil.loadClassFromJar("/tmp/test.jar", "Test");
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
                times = 3;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                mockProxyUtil.loadClassFromJar("/tmp/test.jar", "Test");
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
                times = 3;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                mockProxyUtil.loadClassFromJar("/tmp/test.jar", "Test");
                result = mockNotifier;
                mockNotification.getId(); times = 1;
                result = "123";
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
                times = 3;
                result = mockNotifierConfig;
                mockNotifierConfig.getClassName();
                times = 1;
                result = "Test";
                mockNotifierConfig.getJarPath();
                times = 1;
                result = "/tmp/test.jar";
                mockProxyUtil.loadClassFromJar("/tmp/test.jar", "Test");
                result = mockNotifier;
            }
        };
        notificationService.register("test_notifier", mockCtx);

        QueryParam qp1 = new QueryParam("one", "1");
        QueryParam qp2 = new QueryParam("two", "2");
        QueryParam qp3 = new QueryParam("numRows", "5");

        notificationService.findNotifications(Arrays.asList(qp1, qp2, qp3));

        new Verifications() {
            {
                Criteria<Notification> criteria;
                mockNotificationStore.findEntities(criteria = withCapture());
                //System.out.println("criteria = " + criteria);
                assertEquals(criteria.clazz(), Notification.class);
                assertEquals(criteria.numRows(), 5);
                assertEquals(criteria.fieldRestrictions().size(), 2);
                assertEquals(criteria.fieldRestrictions().get(0).getValue(), "1");
                assertEquals(criteria.fieldRestrictions().get(1).getValue(), "2");
            }
        };

        notificationService.findNotifications(Arrays.asList(qp1, qp2));

        new Verifications() {
            {
                Criteria<Notification> criteria;
                mockNotificationStore.findEntities(criteria = withCapture());
                //System.out.println("criteria = " + criteria);
                assertEquals(criteria.clazz(), Notification.class);
                assertEquals(0, criteria.numRows());
                assertEquals(criteria.fieldRestrictions().size(), 2);
                assertEquals(criteria.fieldRestrictions().get(0).getValue(), "1");
                assertEquals(criteria.fieldRestrictions().get(1).getValue(), "2");
            }
        };
    }
}