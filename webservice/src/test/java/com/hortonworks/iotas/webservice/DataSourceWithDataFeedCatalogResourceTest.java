package com.hortonworks.iotas.webservice;

import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.catalog.DataSourceWithDataFeedCatalogResource;
import com.hortonworks.iotas.webservice.catalog.dto.DataSourceDto;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 *
 */

@RunWith(JMockit.class)
public class DataSourceWithDataFeedCatalogResourceTest {

    DataSourceWithDataFeedCatalogResource dataSourceWithDataFeedCatalogResource;

    @Mocked
    CatalogService mockCatalogService;

    @Before
    public void setup() {
        dataSourceWithDataFeedCatalogResource = new DataSourceWithDataFeedCatalogResource(mockCatalogService);
    }

    @Test
    public void testAddDataSourceWithDataFeed() throws Exception {
        final DataSource dataSource = createDataSource();
        final DataFeed dataFeed = createDataFeed(dataSource.getDataSourceId());
        final DataSourceDto dataSourceDto = new DataSourceDto(dataSource, dataFeed);

        new Expectations() {
            {
                mockCatalogService.addDataSource(dataSource);times=1;
                result = dataSource;

                mockCatalogService.addDataFeed(dataFeed); times=1;
                result=dataFeed;
            }
        };

        CatalogResponse catalogResponse = (CatalogResponse) dataSourceWithDataFeedCatalogResource.addDataSourceWithDataFeed(dataSourceDto).getEntity();
        assertEquals(CatalogResponse.ResponseMessage.SUCCESS.getCode(), catalogResponse.getResponseCode());
        assertEquals(dataSourceDto, catalogResponse.getEntity());
    }

    @Test
    public void testAddInvalidDataSourceWithDataFeed() throws Exception {
        final DataSource dataSource = new DataSource();
        final DataFeed dataFeed = new DataFeed();
        final DataSourceDto dataSourceDto = new DataSourceDto(dataSource, dataFeed);

        CatalogResponse catalogResponse = (CatalogResponse) dataSourceWithDataFeedCatalogResource.addDataSourceWithDataFeed(dataSourceDto).getEntity();
        assertEquals(CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING.getCode(), catalogResponse.getResponseCode());
    }

    @Test
    public void testAddDataSourceWithDataFeedWithException() throws Exception {
        final DataSource dataSource = createDataSource();
        final DataFeed dataFeed = createDataFeed(dataSource.getDataSourceId());
        final DataSourceDto dataSourceDto = new DataSourceDto(dataSource, dataFeed);
        new Expectations() {
            {
                mockCatalogService.addDataSource(dataSource);times=1;
                result = dataSource;

                mockCatalogService.addDataFeed(dataFeed); times=1;
                result= new Exception();
            }
        };

        CatalogResponse catalogResponse = (CatalogResponse) dataSourceWithDataFeedCatalogResource.addDataSourceWithDataFeed(dataSourceDto).getEntity();
        assertEquals(CatalogResponse.ResponseMessage.EXCEPTION.getCode(), catalogResponse.getResponseCode());
    }


    private DataSource createDataSource() {
        DataSource dataSource = new DataSource();
        dataSource.setDataSourceId(new Random().nextLong());
        dataSource.setDataSourceName("datasource-1");
        dataSource.setType(DataSource.Type.DEVICE);
        dataSource.setTypeConfig("device-datasource-typeconfig");
        dataSource.setTags("tag-1");
        dataSource.setDescription("test device data source");
        return dataSource;
    }

    private DataFeed createDataFeed(long dataSourceId) {
        DataFeed dataFeed = new DataFeed();
        dataFeed.setDataSourceId(dataSourceId);
        dataFeed.setDataFeedName("test data feed");
        dataFeed.setEndpoint("test-endpoint");
        dataFeed.setParserId(1l);
        return dataFeed;
    }
}
