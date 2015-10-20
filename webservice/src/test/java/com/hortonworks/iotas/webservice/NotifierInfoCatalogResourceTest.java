package com.hortonworks.iotas.webservice;

import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.catalog.NotifierInfo;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.catalog.NotifierInfoCatalogResource;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by aiyer on 9/28/15.
 */
@RunWith(JMockit.class)
public class NotifierInfoCatalogResourceTest {

    NotifierInfoCatalogResource resource;

    @Mocked
    CatalogService mockCatalogService;

    @Mocked
    UriInfo mockUriInfo;

    MultivaluedMap<String, String> multiValuedMap;

    @Before
    public void setUp() throws Exception {
        resource = new NotifierInfoCatalogResource(mockCatalogService);
        multiValuedMap = new MultivaluedHashMap<>();
    }

    @Test
    public void testListNotifiers() throws Exception {
        final NotifierInfo notifierInfo = new NotifierInfo();
        final CatalogService.QueryParam expectedQp = new CatalogService.QueryParam("notifierName", "email_notifier_1");
        multiValuedMap.putSingle("notifierName", "email_notifier_1");
        new Expectations() {
            {
                mockUriInfo.getQueryParameters(); times = 1;
                result = multiValuedMap;

                mockCatalogService.listNotifierInfos(Arrays.asList(expectedQp));times=1;
                result = Arrays.asList(notifierInfo);

            }
        };

        CatalogResponse catalogResponse = (CatalogResponse) resource.listNotifiers(mockUriInfo).getEntity();

        assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), catalogResponse.getResponseCode());
        assertEquals(1, catalogResponse.getEntities().size());
        assertEquals(notifierInfo, catalogResponse.getEntities().iterator().next());
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

        CatalogResponse catalogResponse = (CatalogResponse) resource.getNotifierById(1L).getEntity();

        assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), catalogResponse.getResponseCode());
        assertEquals(notifierInfo, catalogResponse.getEntity());
    }

    @Test
    public void testAddNotifier() throws Exception {
        final NotifierInfo notifierInfo = new NotifierInfo();
        new Expectations() {
            {
                mockCatalogService.addNotifierInfo(notifierInfo);times=1;
                result = notifierInfo;
            }
        };

        CatalogResponse catalogResponse = (CatalogResponse) resource.addNotifier(notifierInfo).getEntity();

        assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), catalogResponse.getResponseCode());
        assertEquals(notifierInfo, catalogResponse.getEntity());
    }

}