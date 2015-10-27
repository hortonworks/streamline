package com.hortonworks.iotas.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.DataStream;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.impl.memory.InMemoryStorageManager;
import com.hortonworks.iotas.util.exception.BadDataStreamLayoutException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by pshah on 10/5/15.
 */
public class DataStreamLayoutValidatorTest {
    StorageManager dao;
    ObjectMapper mapper;
    DataSource dataSource,dataSource2;
    String[] goodLayouts = {"datastreams/GoodLayout.json"};
    // if an element is added to the array below then corresponding error
    // message also needs to be added to badLayoutMessages array below
    String[] badLayouts = {"datastreams/BadDataSourceLayout.json",
        "datastreams/BadDataSourceConfigTypeLayout.json",
        "datastreams/MissingDataSourceConfigLayout.json",
        "datastreams/DuplicateUinameDataSinkLayout.json",
        "datastreams/BadDataSinkConfigTypeLayout.json",
        "datastreams/MissingDataSinkConfigLayout.json",
        "datastreams/BadLinkFromLayout.json",
        "datastreams/BadLinkToLayout.json",
        "datastreams/DisconnectedDataSourceLayout.json",
        "datastreams/DisconnectedDataSinkLayout.json",
        "datastreams/DisconnectedProcessorInLayout.json",
        "datastreams/DisconnectedProcessorOutLayout.json"
    };
    // the size of the array below should be same as size of the array
    // badLayouts above
    String[] badLayoutMessages = {String.format(DataStreamLayoutValidator
            .ERR_MSG_DATA_SOURCE_NOT_FOUND, "3"), String.format
            (DataStreamLayoutValidator.ERR_MSG_DATA_SOURCE_INVALID_TYPE,
                    "unknown"), String.format(DataStreamLayoutValidator
            .ERR_MSG_DATA_SOURCE_MISSING_CONFIG, "KAFKA"),
            String.format(DataStreamLayoutValidator.ERR_MSG_UINAME_DUP,
                    "kafkaDataSource"), String.format
            (DataStreamLayoutValidator.ERR_MSG_DATA_SINK_INVALID_TYPE,
                    "BASE"), String.format(DataStreamLayoutValidator
            .ERR_MSG_DATA_SINK_MISSING_CONFIG, "HBASE"), String.format
            (DataStreamLayoutValidator.ERR_MSG_LINK_FROM,
                    "afkaDataSource"), String.format
            (DataStreamLayoutValidator.ERR_MSG_LINK_TO, "basesink"),
            String.format(DataStreamLayoutValidator
                    .ERR_MSG_DISCONNETED_DATA_SOURCE, "kafkaDataSource2")
            , String.format(DataStreamLayoutValidator
            .ERR_MSG_DISCONNETED_DATA_SINK, "hbasesink0"), String.format
            (DataStreamLayoutValidator.ERR_MSG_DISCONNETED_PROCESSOR_IN,
                    "disconnectedProcessor"), String.format
            (DataStreamLayoutValidator.ERR_MSG_DISCONNETED_PROCESSOR_OUT,
                    "dontCareRule")};
    @Before
    public void setup () {
        dao = new InMemoryStorageManager();
        mapper = new ObjectMapper();
        dataSource = new DataSource();
        dataSource.setDataSourceId(1l);
        dao.add(dataSource);
        dataSource2 = new DataSource();
        dataSource.setDataSourceId(2l);
        dao.add(dataSource);
        String prefixString = BadDataStreamLayoutException.class.getName() +
                ": ";
        for (int i = 0; i < badLayoutMessages.length; ++i) {
            badLayoutMessages[i] = prefixString + badLayoutMessages[i];
        }
    }
    @After
    public void cleanup () {
        dao.remove(dataSource.getStorableKey());
        dao.remove(dataSource2.getStorableKey());
    }
    @Test
    public void testDataStreamLayoutGood () throws IOException, BadDataStreamLayoutException {
        // Test for a valid data stream layout json
        URL datastream = Thread.currentThread().getContextClassLoader()
                .getResource("datastreams/goodlayout.json");
        DataStream ds = mapper.readValue(datastream, DataStream.class);
        DataStreamLayoutValidator.validateDataStreamLayout(ds.getJson(), dao);
    }

    @Test
    public void testDataStreamLayoutBad () throws IOException {
        for (int i = 0; i < this.badLayouts.length; ++i) {
            URL datastream = Thread.currentThread().getContextClassLoader()
                    .getResource(badLayouts[i]);
            DataStream ds = mapper.readValue(datastream, DataStream.class);
            try {
                DataStreamLayoutValidator.validateDataStreamLayout(ds.getJson(), dao);
                Assert.fail("DataStream Layou validation test failed for" +
                        " " + this.badLayouts[i]);
            } catch (BadDataStreamLayoutException ex) {
                Assert.assertEquals("Exception getMessage does not match " +
                        "expected", badLayoutMessages[i], ex.getMessage());
            }
        }
    }
}
