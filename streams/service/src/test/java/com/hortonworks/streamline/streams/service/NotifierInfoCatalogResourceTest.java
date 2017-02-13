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
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.util.FileStorage;
import com.hortonworks.streamline.streams.catalog.NotifierInfo;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
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
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.Arrays;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;

/**
 * Created by aiyer on 9/28/15.
 */
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

    @Before
    public void setUp() throws Exception {
        resource = new NotifierInfoCatalogResource(mockCatalogService, mockFileStorage);
        multiValuedMap = new MultivaluedHashMap<>();
    }

    @Test
    public void testListNotifiers() {
        final NotifierInfo notifierInfo = new NotifierInfo();
        final QueryParam expectedQp = new QueryParam("notifierName", "email_notifier_1");
        multiValuedMap.putSingle("notifierName", "email_notifier_1");
        new Expectations() {
            {
                mockUriInfo.getQueryParameters(); times = 1;
                result = multiValuedMap;

                mockCatalogService.listNotifierInfos(Arrays.asList(expectedQp));times=1;
                result = Arrays.asList(notifierInfo);

            }
        };

        CollectionResponse collectionResponse = (CollectionResponse) resource.listNotifiers(mockUriInfo).getEntity();

        assertEquals(1, collectionResponse .getEntities().size());
        assertEquals(notifierInfo, collectionResponse.getEntities().iterator().next());
    }

    @Test
    public void testGetNotifierById() throws Exception {
        final NotifierInfo notifierInfo = new NotifierInfo();
        new Expectations() {
            {
                mockCatalogService.getNotifierInfo(anyLong);times=1;
                result = notifierInfo;
            }
        };

        NotifierInfo result = (NotifierInfo) resource.getNotifierById(1L).getEntity();
        assertEquals(notifierInfo, result);
    }

    @Test
    public void testAddNotifier() throws Exception {
        final NotifierInfo notifierInfo = new NotifierInfo();
        notifierInfo.setName("test");
        new Expectations() {
            {
                mockCatalogService.addNotifierInfo(notifierInfo);times=1;
                result = notifierInfo;
                mockFormDataBodyPart.getMediaType();
                result = APPLICATION_JSON_TYPE;
                mockFormDataBodyPart.getValueAs(NotifierInfo.class);
                result = notifierInfo;
                mockFileStorage.uploadFile(mockInputStream, anyString);
                result = "uploadedPath";

            }
        };

        NotifierInfo result = (NotifierInfo) resource.addNotifier(
                mockInputStream,
                mockFormDataContentDisposition,
                mockFormDataBodyPart).getEntity();

        assertEquals(notifierInfo, result);
    }

}