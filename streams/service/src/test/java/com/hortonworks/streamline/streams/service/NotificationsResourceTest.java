/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/


package com.hortonworks.streamline.streams.service;

import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.streams.notification.service.NotificationService;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.impl.NoopAuthorizer;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(JMockit.class)
public class NotificationsResourceTest {

    NotificationsResource resource;

    @Mocked
    UriInfo mockUriInfo;

    @Mocked
    NotificationService mockNotificationService;

    @Injectable
    StreamlineAuthorizer authorizer = new NoopAuthorizer();

    @Injectable
    SecurityContext securityContext;

    @Before
    public void setUp() {
        resource = new NotificationsResource(authorizer, mockNotificationService);
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

        try {
            resource.listNotifications(mockUriInfo, securityContext);

            fail("We don't mock the result so it should throw entity not found");
        } catch (EntityNotFoundException e) {
            // expected
            new Verifications() {
                {
                    List<QueryParam> qps;
                    mockNotificationService.findNotifications(qps = withCapture());
                    //System.out.println(qps);
                    assertEquals(4, qps.size());
                }
            };
        }
    }
}