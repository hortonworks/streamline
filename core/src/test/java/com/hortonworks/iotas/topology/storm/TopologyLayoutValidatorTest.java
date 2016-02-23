package com.hortonworks.iotas.topology.storm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.Topology;
import com.hortonworks.iotas.client.CatalogRestClient;
import com.hortonworks.iotas.processor.CustomProcessor;
import com.hortonworks.iotas.processor.examples.ConsoleCustomProcessor;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.impl.memory.InMemoryStorageManager;
import com.hortonworks.iotas.topology.TopologyActions;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.topology.TopologyLayoutValidator;
import com.hortonworks.iotas.util.ProxyUtil;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@RunWith(JMockit.class)
public class TopologyLayoutValidatorTest {
    private @Mocked
    ProxyUtil<CustomProcessor> customProcessorProxyUtil;
    private @Mocked
    CatalogRestClient catalogRestClient;
    private CustomProcessor customProcessor = new ConsoleCustomProcessor();
    StorageManager dao;
    ObjectMapper mapper;
    TopologyActions topologyActions = new StormTopologyActionsImpl();

    String[] goodLayouts = {"topology/goodlayout.json", "topology/goodlayoutnotificationbolt.json", "topology/goodlayoutcustomprocessor.json"};
    // if an element is added to the array below then corresponding error
    // message also needs to be added to badLayoutMessages array below
    String[] badLayouts = {
            "topology/duplicateuinamelayout.json",
            "topology/linklooplayout.json",
            "topology/invalidlinkfromlayout.json",
            "topology/invalidlinktolayout.json",
            "topology/disconnecteddatasourcelayout.json",
            "topology/disconnecteddatasinklayout.json",
            "topology/disconnectedprocessorinlayout.json",
            "topology/disconnectedprocessoroutlayout.json",
            "topology/kafkamissingrequiredfieldlayout.json",
            "topology/kafkabadoptionalfieldlayout.json",
            "topology/hbasemissingrequiredfieldlayout.json",
            "topology/hbasebadoptionalfieldlayout.json",
            "topology/hdfsmissingrequiredfieldlayout.json",
            "topology/hdfsbadoptionalfieldlayout.json",
            "topology/notificationmissingrequiredfieldlayout.json",
            "topology/notificationbadoptionalfieldlayout.json",
            "topology/parsermissingrequiredfieldlayout.json",
            "topology/parserbadoptionalfieldlayout.json",
            "topology/rulemissingrequiredfieldlayout.json",
            "topology/rulebadoptionalfieldlayout.json",
            "topology/linkbadoptionalfieldlayout.json",
            "topology/invalidfieldsgroupinglinklayout.json",
            "topology/noparserprocessorlayout.json",
            "topology/invalidparserstreamidlayout.json",
            // Uncomment this with https://hwxiot.atlassian.net/browse/IOT-126
            //"topology/emptystreamidfromrulelinklayout.json",
            //"topology/invalidstreamidfromrulelinklayout.json",
            //"topology/invalidfieldsfromrulelinklayout.json",
            "topology/cpboltmissingrequiredfieldlayout.json",
            "topology/cpboltinvalidinputschemalayout.json",
            "topology/cpboltcustomconfigexception.json",
            "topology/cpboltemptystreamidlayout.json",
            "topology/cpboltinvalidstreamidlayout.json",
            "topology/cpboltinvalidfieldsgroupinglayout.json"
    };
    // the size of the array below should be same as size of the array
    // badLayouts above
    String[] badLayoutMessages = {
        String.format(TopologyLayoutConstants.ERR_MSG_UINAME_DUP, "kafkaDataSource"),
        String.format(TopologyLayoutConstants.ERR_MSG_LOOP, "kafkaDataSource", "kafkaDataSource"),
        String.format(TopologyLayoutConstants.ERR_MSG_LINK_FROM, "hbasesink"),
        String.format(TopologyLayoutConstants.ERR_MSG_LINK_TO, "kafkaDataSource"),
        String.format(TopologyLayoutConstants.ERR_MSG_DISCONNETED_DATA_SOURCE, "kafkaDataSource1"),
        String.format(TopologyLayoutConstants.ERR_MSG_DISCONNETED_DATA_SINK, "hbasesink1"),
        String.format(TopologyLayoutConstants.ERR_MSG_DISCONNETED_PROCESSOR_IN, "parserProcessor1"),
        String.format(TopologyLayoutConstants.ERR_MSG_DISCONNETED_PROCESSOR_OUT, "parserProcessor1"),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_ZK_URL),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_IGNORE_ZK_OFFSETS),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_TABLE),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_PARALLELISM),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_FS_URL),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_ROTATION_INTERVAL),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_NOTIFIER_NAME),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_NOTIFIER_PROTOCOL),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_PARSED_TUPLES_STREAM),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_FAILED_TUPLES_STREAM),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_RULES_PROCESSOR_CONFIG),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_PARALLELISM),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_STREAM_ID),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_GROUPING_FIELDS),
        TopologyLayoutConstants.ERR_MSG_NO_PARSER_PROCESSOR,
        String.format(TopologyLayoutConstants.ERR_MSG_INVALID_STREAM_ID, "parserProcessor-parsedTuples->ruleProcessor"),
        //String.format(TopologyLayoutConstants.ERR_MSG_INVALID_STREAM_ID, "ruleProcessor-rule1->hbasesink"),
        //String.format(TopologyLayoutConstants.ERR_MSG_INVALID_STREAM_ID, "ruleProcessor-rule1->hbasesink"),
        //String.format(TopologyLayoutConstants.ERR_MSG_INVALID_GROUPING_FIELDS, "ruleProcessor-rule1->hbasesink"),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_IMPL),
        String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, TopologyLayoutConstants.JSON_KEY_INPUT_SCHEMA),
        String.format(TopologyLayoutConstants.ERR_MSG_CP_CONFIG_EXCEPTION, "com.hortonworks.iotas.processor.examples.ConsoleCustomProcessor") + " Message from " +
                "implementation is: Missing config field: configField",
        String.format(TopologyLayoutConstants.ERR_MSG_INVALID_STREAM_ID, "consoleCustomProcessor->hbasesink"),
        String.format(TopologyLayoutConstants.ERR_MSG_INVALID_STREAM_ID, "consoleCustomProcessor->hbasesink"),
        String.format(TopologyLayoutConstants.ERR_MSG_INVALID_GROUPING_FIELDS, "consoleCustomProcessor->hbasesink")
    };

    @Before
    public void setup () throws ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
        dao = new InMemoryStorageManager();
        mapper = new ObjectMapper();
        Map conf = new HashMap<>();
        conf.put(TopologyLayoutConstants.YAML_KEY_CATALOG_ROOT_URL, "http://localhost:8080/api/v1/catalog");
        topologyActions.init(conf);
        new Expectations() {{
            catalogRestClient.getCustomProcessorJar(withAny("")); result = new ByteArrayInputStream("some-stream".getBytes());
            customProcessorProxyUtil.loadClassFromJar(withAny(""), ConsoleCustomProcessor.class.getCanonicalName()); result = customProcessor;
        }};
    }

    @After
    public void cleanup () {
    }

    @Test
    public void testTopologyLayoutGood () throws IOException {
        // Test for a valid topology layout json
        for (int i = 0; i < this.goodLayouts.length; ++i) {
            URL topologyJson = Thread.currentThread().getContextClassLoader()
                    .getResource(goodLayouts[i]);
            Topology topology = mapper.readValue(topologyJson, Topology.class);
            dao.addOrUpdate(topology);
            try {
                TopologyLayoutValidator validator = new TopologyLayoutValidator
                        (topology.getConfig());
                validator.validate();
                topologyActions.validate(topology);
            } catch (Exception ex) {
                Assert.fail("Good topology should not throw an exception." + ex.getMessage());
            }
            dao.remove(topology.getStorableKey());
        }
    }

    @Test
    public void testTopologyLayoutBad () throws IOException {
        for (int i = 0; i < this.badLayouts.length; ++i) {
            final int j = i;
            URL topologyJson = Thread.currentThread().getContextClassLoader()
                    .getResource(badLayouts[i]);
            Topology topology = mapper.readValue(topologyJson, Topology.class);
            dao.addOrUpdate(topology);
            try {
                TopologyLayoutValidator validator = new TopologyLayoutValidator
                        (topology.getConfig());
                validator.validate();
                topologyActions.validate(topology);
                Assert.fail("Topology Layout validation test failed for" +
                        " " + this.badLayouts[i]);
            } catch (Exception ex) {
                Assert.assertThat(ex.getMessage(), new Matcher<String>() {
                    @Override
                    public boolean matches(Object o) {
                        return ((String) o).endsWith(badLayoutMessages[j]);
                    }

                    @Override
                    public void describeMismatch(Object o, Description description) {

                    }

                    @Override
                    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {

                    }

                    @Override
                    public void describeTo(Description description) {

                    }
                });
            }
            dao.remove(topology.getStorableKey());
        }
    }
}
