package com.hortonworks.iotas.webservice;

import com.hortonworks.iotas.notification.service.NotificationService;
import com.hortonworks.iotas.service.CatalogService;
import mockit.Expectations;
import mockit.Mock;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JMockit.class)
public class NotificationsResourceTest {

    NotificationsResource resource;

    @Mocked
    UriInfo mockUriInfo;

    @Mocked
    NotificationService mockNotificationService;

    @Before
    public void setUp() {
        resource = new NotificationsResource(mockNotificationService);
    }

    @Test
    public void testListNotifications() throws Exception {
        final MultivaluedMap<String, String> qp = new MultivaluedHashMap<String, String>() {
            {
                putSingle("status", "DELIVERED");
                putSingle("notifierName", "console_notifier");
                putSingle("startTs", "1444625166800");
                putSingle("endTs", "1444625166810");
            }
        };

        new Expectations() {
            {
                mockUriInfo.getQueryParameters(); times = 1;
                result = qp;
            }
        };

        resource.listNotifications(mockUriInfo);

        new Verifications() {
            {
                List<CatalogService.QueryParam> qps;
                mockNotificationService.findNotifications(qps = withCapture());
                //System.out.println(qps);
                assertEquals(4, qps.size());
                assertEquals("notifierName", qps.get(0).getName());
                assertEquals("console_notifier", qps.get(0).getValue());
                assertEquals("status", qps.get(1).getName());
                assertEquals("DELIVERED", qps.get(1).getValue());
            }
        };
    }
}