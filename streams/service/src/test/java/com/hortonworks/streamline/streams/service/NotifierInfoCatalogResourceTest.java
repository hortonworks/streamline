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

import com.hortonworks.streamline.common.CollectionResponse;
import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.registries.common.util.FileStorage;
import com.hortonworks.streamline.streams.catalog.Notifier;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.impl.NoopAuthorizer;
import mockit.Expectations;
import mockit.Injectable;
import mockit.integration.junit4.JMockit;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.Arrays;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class NotifierInfoCatalogResourceTest {

    NotifierInfoCatalogResource resource;

    @Injectable
    StreamCatalogService mockCatalogService;

    @Injectable
    UriInfo mockUriInfo;

    @Injectable
    FileStorage mockFileStorage;

    @Injectable
    InputStream mockInputStream;

    @Injectable
    FormDataContentDisposition mockFormDataContentDisposition;

    @Injectable
    FormDataBodyPart mockFormDataBodyPart;

    MultivaluedMap<String, String> multiValuedMap;

    @Injectable
    StreamlineAuthorizer authorizer = new NoopAuthorizer();

    @Injectable
    SecurityContext securityContext;


    @Before
    public void setUp() throws Exception {
        resource = new NotifierInfoCatalogResource(authorizer, mockCatalogService, mockFileStorage);
        multiValuedMap = new MultivaluedHashMap<>();
    }

    @Test
    public void testListNotifiers() {
        final Notifier notifier = new Notifier();
        final QueryParam expectedQp = new QueryParam("notifierName", "email_notifier_1");
        multiValuedMap.putSingle("notifierName", "email_notifier_1");
        new Expectations() {
            {
                mockUriInfo.getQueryParameters(); times = 1;
                result = multiValuedMap;

                mockCatalogService.listNotifierInfos(Arrays.asList(expectedQp));times=1;
                result = Arrays.asList(notifier);

            }
        };

        CollectionResponse collectionResponse = (CollectionResponse) resource.listNotifiers(mockUriInfo, securityContext).getEntity();

        assertEquals(1, collectionResponse .getEntities().size());
        assertEquals(notifier, collectionResponse.getEntities().iterator().next());
    }

    @Test
    public void testGetNotifierById() throws Exception {
        final Notifier notifier = new Notifier();
        new Expectations() {
            {
                mockCatalogService.getNotifierInfo(anyLong);times=1;
                result = notifier;
            }
        };

        Notifier result = (Notifier) resource.getNotifierById(1L, securityContext).getEntity();
        assertEquals(notifier, result);
    }

    @Test
    public void testAddNotifier() throws Exception {
        final Notifier notifier = new Notifier();
        notifier.setName("test");
        new Expectations() {
            {
                mockCatalogService.addNotifierInfo(notifier);times=1;
                result = notifier;
                mockFormDataBodyPart.getMediaType();
                result = APPLICATION_JSON_TYPE;
                mockFormDataBodyPart.getValueAs(Notifier.class);
                result = notifier;
                mockFileStorage.upload(mockInputStream, anyString);
                result = "uploadedPath";

            }
        };

        Notifier result = (Notifier) resource.addNotifier(
                mockInputStream,
                mockFormDataContentDisposition,
                mockFormDataBodyPart,
                securityContext).getEntity();

        assertEquals(notifier, result);
    }

}