package com.hortonworks.iotas.webservice;

import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.topology.TopologyComponent;
import com.hortonworks.iotas.webservice.schema.MockEvolvingSchemaCatalogServiceAwareImpl;
import com.hortonworks.iotas.webservice.schema.MockEvolvingSchemaImpl;
import com.hortonworks.iotas.webservice.schema.SchemaAPI;
import mockit.Expectations;
import mockit.Injectable;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class SchemaAPITest {
    SchemaAPI resource;

    @Injectable
    CatalogService mockCatalogService;

    @Injectable
    UriInfo mockUriInfo;

    MultivaluedMap<String, String> multiValuedMap;
    Map<String, Object> topologyComponentMap;

    @Before
    public void setUp() throws Exception {
        resource = new SchemaAPI(mockCatalogService);
        multiValuedMap = createDummyQueryParameters();
        topologyComponentMap = createDummyRuleTopologyComponentMap();
    }

    private MultivaluedHashMap<String, String> createDummyQueryParameters() {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.putSingle("componentId", "1");
        queryParams.putSingle("configuration", "{}");
        queryParams.putSingle("stream", "{}");
        return queryParams;
    }

    @Test
    public void testEvolveForSupportedComponent() throws Exception {
        topologyComponentMap.put("schemaClass", "com.hortonworks.iotas.webservice.schema.MockEvolvingSchemaImpl");
        final TopologyComponent component = (TopologyComponent) new TopologyComponent().fromMap(topologyComponentMap);

        new Expectations() {
            {
                mockUriInfo.getQueryParameters(); times = 1;
                result = multiValuedMap;

                mockCatalogService.getTopologyComponent(1L);
                result = component;
            }
        };

        Response response = resource.simulateEvolution(mockUriInfo);
        assertEquals(OK.getStatusCode(), response.getStatus());

        CatalogResponse catalogResponse = (CatalogResponse) response.getEntity();
        assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), catalogResponse.getResponseCode());

        Collection<Stream> streams = (Collection<Stream>) catalogResponse.getEntities();
        assertEquals(new MockEvolvingSchemaImpl().getStreams(), streams);
    }

    @Test
    public void testEvolveForNotSupportedComponent() throws Exception {
        // assume schema class isn't defined to component definition
        topologyComponentMap.remove("schemaClass");

        final TopologyComponent component = (TopologyComponent) new TopologyComponent().fromMap(topologyComponentMap);

        new Expectations() {
            {
                mockUriInfo.getQueryParameters(); times = 1;
                result = multiValuedMap;

                mockCatalogService.getTopologyComponent(1L);
                result = component;
            }
        };

        Response response = resource.simulateEvolution(mockUriInfo);
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        CatalogResponse catalogResponse = (CatalogResponse) response.getEntity();
        assertEquals(CatalogResponse.ResponseMessage.EXCEPTION.getCode(), catalogResponse.getResponseCode());
    }

    @Test
    public void testEvolveComponentNotFound() {
        new Expectations() {
            {
                mockUriInfo.getQueryParameters(); times = 1;
                result = multiValuedMap;

                mockCatalogService.getTopologyComponent(1L);
                result = null;
            }
        };

        Response response = resource.simulateEvolution(mockUriInfo);
        assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());

        CatalogResponse catalogResponse = (CatalogResponse) response.getEntity();
        assertEquals(CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND.getCode(), catalogResponse.getResponseCode());
    }

    @Test
    public void testEvolvePlayWellWithCatalogServiceAware() {
        topologyComponentMap = createDummyParserTopologyComponentMap();
        topologyComponentMap.put("schemaClass", "com.hortonworks.iotas.webservice.schema.MockEvolvingSchemaCatalogServiceAwareImpl");

        final TopologyComponent component = (TopologyComponent) new TopologyComponent()
                .fromMap(topologyComponentMap);

        new Expectations() {
            {
                mockUriInfo.getQueryParameters(); times = 1;
                result = multiValuedMap;

                mockCatalogService.getTopologyComponent(1L);
                result = component;
            }
        };

        Response response = resource.simulateEvolution(mockUriInfo);
        assertEquals(OK.getStatusCode(), response.getStatus());

        CatalogResponse catalogResponse = (CatalogResponse) response.getEntity();
        assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), catalogResponse.getResponseCode());

        Collection<Stream> streams = (Collection<Stream>) catalogResponse.getEntities();
        assertEquals(new MockEvolvingSchemaCatalogServiceAwareImpl().getStreams(), streams);
    }

    private Map<String, Object> createDummyRuleTopologyComponentMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 1L);
        map.put("name", "dummy");
        map.put("type", "PROCESSOR");
        map.put("timestamp", 1L);
        map.put("streamingEngine", "STORM");
        map.put("subType", "RULE");
        map.put("config", "dummy");
        map.put("transformationClass", "dummy");
        return map;
    }

    private Map<String, Object> createDummyParserTopologyComponentMap() {
        Map<String, Object> map = createDummyRuleTopologyComponentMap();
        map.put("type", "PROCESSOR");
        map.put("subType", "PARSER");
        return map;
    }
}
