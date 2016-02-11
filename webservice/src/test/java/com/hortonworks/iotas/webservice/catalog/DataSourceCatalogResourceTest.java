package com.hortonworks.iotas.webservice.catalog;

import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.service.CatalogService;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by aiyer on 10/26/15.
 */
@RunWith(JMockit.class)
public class DataSourceCatalogResourceTest {
    @Mocked
    CatalogService mockCatalogService;

    @Test
    public void testGetDataSourceSchemaNull() throws Exception {
        DataSourceCatalogResource resource = new DataSourceCatalogResource(mockCatalogService);
        new Expectations() {
            {
                mockCatalogService.listDataFeeds(withAny(new ArrayList<CatalogService.QueryParam>()));
                times = 1;
                result = null;

            }
        };
        Response rsp = resource.getDataSourceSchema(1L);
        assertEquals(404, rsp.getStatus());
        assertNull(((CatalogResponse) rsp.getEntity()).getEntity());
    }

    @Test
    public void testGetDataSourceSchemaEmpty() throws Exception {
        DataSourceCatalogResource resource = new DataSourceCatalogResource(mockCatalogService);
        new Expectations() {
            {
                mockCatalogService.listDataFeeds(withAny(new ArrayList<CatalogService.QueryParam>()));
                times = 1;
                result = new ArrayList<DataFeed>();

            }
        };
        Response rsp = resource.getDataSourceSchema(1L);
        assertEquals(404, rsp.getStatus());
        assertNull(((CatalogResponse) rsp.getEntity()).getEntity());
    }


    @Test
    public void testGetDataSourceSchemaOnetoOne() throws Exception {
        DataSourceCatalogResource resource = new DataSourceCatalogResource(mockCatalogService);
        final DataFeed dataFeed = new DataFeed();
        dataFeed.setId(1L);
        dataFeed.setDataSourceId(2L);
        dataFeed.setParserId(3L);
        final ParserInfo parserInfo = new ParserInfo();
        Schema parserSchema = Schema.of(new Schema.Field("foo", Schema.Type.STRING),
                                        new Schema.Field("bar", Schema.Type.INTEGER));
        parserInfo.setParserSchema(parserSchema);
        final List<CatalogService.QueryParam> queryParams = Arrays.asList(new CatalogService.QueryParam("dataSourceId", "2"));
        new Expectations() {
            {
                mockCatalogService.listDataFeeds(queryParams);
                times = 1;
                result = Arrays.asList(dataFeed);
                mockCatalogService.getParserInfo(3L);
                times = 1;
                result = parserInfo;
            }
        };
        Response rsp = resource.getDataSourceSchema(2L);
        assertEquals(200, rsp.getStatus());
        CatalogResponse catalogResponse = (CatalogResponse) rsp.getEntity();
        assertEquals(parserSchema, catalogResponse.getEntity());
    }

    @Test
    public void testGetDataSourceSchemaManytoOne() throws Exception {
        DataSourceCatalogResource resource = new DataSourceCatalogResource(mockCatalogService);
        final DataFeed dataFeed1 = new DataFeed();
        dataFeed1.setId(1L);
        dataFeed1.setDataSourceId(2L);
        dataFeed1.setParserId(3L);
        final DataFeed dataFeed2 = new DataFeed();
        dataFeed2.setId(2L);
        dataFeed2.setDataSourceId(2L);
        dataFeed2.setParserId(3L);
        final ParserInfo parserInfo = new ParserInfo();
        Schema parserSchema = Schema.of(new Schema.Field("foo", Schema.Type.STRING),
                                        new Schema.Field("bar", Schema.Type.INTEGER));
        parserInfo.setParserSchema(parserSchema);
        final List<CatalogService.QueryParam> queryParams = Arrays.asList(new CatalogService.QueryParam("dataSourceId", "2"));
        new Expectations() {
            {
                mockCatalogService.listDataFeeds(queryParams);
                times = 1;
                result = Arrays.asList(dataFeed1, dataFeed2);
            }
        };
        Response rsp = resource.getDataSourceSchema(2L);
        new Verifications() {
            {
                mockCatalogService.getParserInfo(3L);
                times = 0;
            }
        };
        assertEquals(404, rsp.getStatus());
        CatalogResponse catalogResponse = (CatalogResponse) rsp.getEntity();
        assertEquals(CatalogResponse.ResponseMessage.DATASOURCE_SCHEMA_NOT_UNIQUE.getCode(), catalogResponse.getResponseCode());
        assertNull(catalogResponse.getEntity());
    }
}