package com.hortonworks.streamline.streams.logsearch.storm.ambari;

import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.ValuePattern;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Lists;
import com.hortonworks.streamline.streams.logsearch.EventSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.EventSearchResult;
import com.hortonworks.streamline.streams.logsearch.LogSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.LogSearchResult;
import mockit.Deencapsulation;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_LOG_LEVEL;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_LOG_MESSAGE;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_LOG_TIME;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_STREAMLINE_COMPONENT_NAME;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_STREAMLINE_EVENT_AUX_KEYVALUES;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_STREAMLINE_EVENT_HEADERS;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_STREAMLINE_EVENT_ID;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_STREAMLINE_EVENT_KEYVALUES;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_STREAMLINE_EVENT_PARENT_ID_SET;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_STREAMLINE_EVENT_ROOT_ID_SET;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_STREAMLINE_TOPOLOGY_ID;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_TYPE;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_VALUE_TYPE_EVENT;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_VALUE_TYPE_WORKER_LOG;
import static org.junit.Assert.*;

public class AmbariInfraWithStormLogSearchTest {
    private final String TEST_SOLR_API_PATH = "/solr";
    private final String TEST_COLLECTION_NAME = "test_collection";
    private final String STUB_REQUEST_API_PATH = TEST_SOLR_API_PATH + "/" + TEST_COLLECTION_NAME + "/select";

    private AmbariInfraWithStormLogSearch logSearch;
    private String buildTestSolrApiUrl;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(18886);

    public static final String TEST_APP_ID = "1";
    public static final long TEST_FROM = System.currentTimeMillis() - (1000 * 60 * 30);
    public static final long TEST_TO = System.currentTimeMillis();

    @Before
    public void setUp() throws Exception {
        logSearch = new AmbariInfraWithStormLogSearch();

        Map<String, Object> conf = new HashMap<>();
        buildTestSolrApiUrl = "http://localhost:18886" + TEST_SOLR_API_PATH;
        conf.put(AmbariInfraWithStormLogSearch.SOLR_API_URL_KEY, buildTestSolrApiUrl);
        conf.put(AmbariInfraWithStormLogSearch.COLLECTION_NAME, TEST_COLLECTION_NAME);

        logSearch.init(conf);

        // we are doing some hack to change parser, since default wt (javabin) would be faster
        // but not good to construct custom result by ourselves
        HttpSolrClient solrClient = Deencapsulation.getField(logSearch, "solr");
        solrClient.setParser(new XMLResponseParser());
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testLogSearchWithMinimumParameters() throws Exception {
        stubSolrUrl();

        LogSearchCriteria logSearchCriteria = new LogSearchCriteria.Builder(TEST_APP_ID, TEST_FROM, TEST_TO).build();
        LogSearchResult result = logSearch.search(logSearchCriteria);
        verifyLogSearchResults(result);

        // please note that space should be escaped to '+' since Wiremock doesn't handle it when matching...
        String dateRangeValue = "%s:[%s+TO+%s]";

        Instant fromInstant = Instant.ofEpochMilli(TEST_FROM);
        Instant toInstant = Instant.ofEpochMilli(TEST_TO);

        dateRangeValue = String.format(dateRangeValue, COLUMN_NAME_LOG_TIME, fromInstant.toString(), toInstant.toString());

        String expectedLogLevels = "(" + String.join("+OR+", AmbariInfraWithStormLogSearch.DEFAULT_LOG_LEVELS) + ")";

        List<LoggedRequest> requests = wireMockRule.findAll(getRequestedFor(urlPathEqualTo(STUB_REQUEST_API_PATH)));
        assertEquals(1, requests.size());

        LoggedRequest request = requests.get(0);

        QueryParameter qParam = request.queryParameter("q");
        assertTrue(qParam.containsValue(COLUMN_NAME_LOG_MESSAGE + ":*"));

        QueryParameter fqParam = request.queryParameter("fq");
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID + ":" + TEST_APP_ID));
        assertTrue(fqParam.containsValue(COLUMN_NAME_TYPE + ":" + COLUMN_VALUE_TYPE_WORKER_LOG));
        assertTrue(fqParam.containsValue(dateRangeValue));
        assertTrue(fqParam.containsValue(COLUMN_NAME_LOG_LEVEL + ":" + expectedLogLevels));
        assertFalse(fqParam.hasValueMatching(ValuePattern.containing(COLUMN_NAME_STREAMLINE_COMPONENT_NAME)));

        QueryParameter sortParam = request.queryParameter("sort");
        assertTrue(sortParam.containsValue(COLUMN_NAME_LOG_TIME + "+asc"));
    }

    @Test
    public void testLogSearchWithFullParameters() throws Exception {
        stubSolrUrl();

        int testStart = 100;
        int testLimit = 2000;
        List<String> testLogLevels = Lists.newArrayList("INFO", "DEBUG");
        String testSearchString = "helloworld";
        List<String> testComponentNames = Lists.newArrayList("testComponent", "testComponent2");

        LogSearchCriteria logSearchCriteria = new LogSearchCriteria.Builder(TEST_APP_ID, TEST_FROM, TEST_TO)
            .setLogLevels(testLogLevels)
            .setSearchString(testSearchString)
            .setComponentNames(testComponentNames)
            .setStart(testStart)
            .setLimit(testLimit)
            .setAscending(false)
            .build();

        LogSearchResult result = logSearch.search(logSearchCriteria);

        // note that the result doesn't change given that we just provide same result from file
        verifyLogSearchResults(result);

        // please note that space should be escaped to '+' since Wiremock doesn't handle it when matching...
        String dateRangeValue = "%s:[%s+TO+%s]";

        Instant fromInstant = Instant.ofEpochMilli(TEST_FROM);
        Instant toInstant = Instant.ofEpochMilli(TEST_TO);

        dateRangeValue = String.format(dateRangeValue, COLUMN_NAME_LOG_TIME, fromInstant.toString(), toInstant.toString());

        String expectedComponentNames = "(" + String.join("+OR+", testComponentNames) + ")";
        String expectedLogLevels = "(" + String.join("+OR+", testLogLevels) + ")";

        List<LoggedRequest> requests = wireMockRule.findAll(getRequestedFor(urlPathEqualTo(STUB_REQUEST_API_PATH)));
        assertEquals(1, requests.size());

        LoggedRequest request = requests.get(0);

        QueryParameter qParam = request.queryParameter("q");
        assertTrue(qParam.containsValue(COLUMN_NAME_LOG_MESSAGE + ":" + testSearchString));

        QueryParameter fqParam = request.queryParameter("fq");
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID + ":" + TEST_APP_ID));
        assertTrue(fqParam.containsValue(COLUMN_NAME_TYPE + ":" + COLUMN_VALUE_TYPE_WORKER_LOG));
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME + ":" + expectedComponentNames));
        assertTrue(fqParam.containsValue(COLUMN_NAME_LOG_LEVEL + ":" + expectedLogLevels));
        assertTrue(fqParam.containsValue(dateRangeValue));

        QueryParameter sortParam = request.queryParameter("sort");
        assertTrue(sortParam.containsValue(COLUMN_NAME_LOG_TIME + "+desc"));

        QueryParameter startParam = request.queryParameter("start");
        assertTrue(startParam.containsValue(String.valueOf(testStart)));

        QueryParameter rowsParam = request.queryParameter("rows");
        assertTrue(rowsParam.containsValue(String.valueOf(testLimit)));
    }

    @Test
    public void testLogSearchWithSingleComponentNameAndLogLevelParameters() throws Exception {
        stubSolrUrl();

        int testStart = 100;
        int testLimit = 2000;
        List<String> testLogLevels = Collections.singletonList("INFO");
        String testSearchString = "helloworld";
        List<String> testComponentNames = Collections.singletonList("testComponent");

        LogSearchCriteria logSearchCriteria = new LogSearchCriteria.Builder(TEST_APP_ID, TEST_FROM, TEST_TO)
            .setLogLevels(testLogLevels)
            .setSearchString(testSearchString)
            .setComponentNames(testComponentNames)
            .setStart(testStart)
            .setLimit(testLimit)
            .build();

        LogSearchResult result = logSearch.search(logSearchCriteria);

        // note that the result doesn't change given that we just provide same result from file
        verifyLogSearchResults(result);

        // others are covered from testLogSearchWithFullParameters()

        List<LoggedRequest> requests = wireMockRule.findAll(getRequestedFor(urlPathEqualTo(STUB_REQUEST_API_PATH)));
        assertEquals(1, requests.size());

        LoggedRequest request = requests.get(0);

        QueryParameter fqParam = request.queryParameter("fq");
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME + ":" + testComponentNames.get(0)));
        assertTrue(fqParam.containsValue(COLUMN_NAME_TYPE + ":" + COLUMN_VALUE_TYPE_WORKER_LOG));
        assertTrue(fqParam.containsValue(COLUMN_NAME_LOG_LEVEL + ":" + testLogLevels.get(0)));
    }

    @Test
    public void testEventSearchWithMinimumParameters() throws Exception {
        stubSolrUrl();

        EventSearchCriteria eventSearchCriteria = new EventSearchCriteria.Builder(TEST_APP_ID, TEST_FROM, TEST_TO).build();
        EventSearchResult result = logSearch.searchEvent(eventSearchCriteria);
        verifyEventSearchResults(result);

        // please note that space should be escaped to '+' since Wiremock doesn't handle it when matching...
        String dateRangeValue = "%s:[%s+TO+%s]";

        Instant fromInstant = Instant.ofEpochMilli(TEST_FROM);
        Instant toInstant = Instant.ofEpochMilli(TEST_TO);

        dateRangeValue = String.format(dateRangeValue, COLUMN_NAME_LOG_TIME, fromInstant.toString(), toInstant.toString());

        List<LoggedRequest> requests = wireMockRule.findAll(getRequestedFor(urlPathEqualTo(STUB_REQUEST_API_PATH)));
        assertEquals(1, requests.size());

        LoggedRequest request = requests.get(0);

        QueryParameter qParam = request.queryParameter("q");
        assertTrue(qParam.containsValue(COLUMN_NAME_STREAMLINE_EVENT_ID + ":*"));

        QueryParameter fqParam = request.queryParameter("fq");
        assertTrue(fqParam.containsValue(COLUMN_NAME_TYPE + ":" + COLUMN_VALUE_TYPE_EVENT));
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID + ":" + TEST_APP_ID));
        assertTrue(fqParam.containsValue(dateRangeValue));
        assertFalse(fqParam.hasValueMatching(ValuePattern.containing(COLUMN_NAME_STREAMLINE_COMPONENT_NAME)));
        assertFalse(fqParam.hasValueMatching(ValuePattern.containing(COLUMN_NAME_STREAMLINE_EVENT_ID)));
        assertFalse(fqParam.hasValueMatching(ValuePattern.containing(COLUMN_NAME_STREAMLINE_EVENT_ROOT_ID_SET)));
        assertFalse(fqParam.hasValueMatching(ValuePattern.containing(COLUMN_NAME_STREAMLINE_EVENT_PARENT_ID_SET)));

        QueryParameter sortParam = request.queryParameter("sort");
        assertTrue(sortParam.containsValue(COLUMN_NAME_LOG_TIME + "+asc"));
    }

    @Test
    public void testEventSearchWithComponentNamesAndStartAndLimitAndDescendingParameters() throws Exception {
        stubSolrUrl();

        int testStart = 100;
        int testLimit = 2000;
        List<String> testComponentNames = Collections.singletonList("SOURCE");

        EventSearchCriteria eventSearchCriteria = new EventSearchCriteria.Builder(TEST_APP_ID, TEST_FROM, TEST_TO)
                .setComponentNames(testComponentNames).setAscending(false).setStart(testStart).setLimit(testLimit).build();
        EventSearchResult result = logSearch.searchEvent(eventSearchCriteria);
        verifyEventSearchResults(result);

        // please note that space should be escaped to '+' since Wiremock doesn't handle it when matching...
        String dateRangeValue = "%s:[%s+TO+%s]";

        Instant fromInstant = Instant.ofEpochMilli(TEST_FROM);
        Instant toInstant = Instant.ofEpochMilli(TEST_TO);

        dateRangeValue = String.format(dateRangeValue, COLUMN_NAME_LOG_TIME, fromInstant.toString(), toInstant.toString());

        List<LoggedRequest> requests = wireMockRule.findAll(getRequestedFor(urlPathEqualTo(STUB_REQUEST_API_PATH)));
        assertEquals(1, requests.size());

        LoggedRequest request = requests.get(0);

        QueryParameter qParam = request.queryParameter("q");
        assertTrue(qParam.containsValue(COLUMN_NAME_STREAMLINE_EVENT_ID + ":*"));

        QueryParameter fqParam = request.queryParameter("fq");
        assertTrue(fqParam.containsValue(COLUMN_NAME_TYPE + ":" + COLUMN_VALUE_TYPE_EVENT));
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID + ":" + TEST_APP_ID));
        assertTrue(fqParam.containsValue(dateRangeValue));
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME + ":" + testComponentNames.get(0)));
        assertFalse(fqParam.hasValueMatching(ValuePattern.containing(COLUMN_NAME_STREAMLINE_EVENT_ID)));
        assertFalse(fqParam.hasValueMatching(ValuePattern.containing(COLUMN_NAME_STREAMLINE_EVENT_ROOT_ID_SET)));
        assertFalse(fqParam.hasValueMatching(ValuePattern.containing(COLUMN_NAME_STREAMLINE_EVENT_PARENT_ID_SET)));

        QueryParameter sortParam = request.queryParameter("sort");
        assertTrue(sortParam.containsValue(COLUMN_NAME_LOG_TIME + "+desc"));

        QueryParameter startParam = request.queryParameter("start");
        assertTrue(startParam.containsValue(String.valueOf(testStart)));

        QueryParameter rowsParam = request.queryParameter("rows");
        assertTrue(rowsParam.containsValue(String.valueOf(testLimit)));
    }

    @Test
    public void testEventSearchWithEventId() throws Exception {
        stubSolrUrl();

        String testEventId = "b7715c60-74ad-43dd-814a-8a40403a31bc";

        EventSearchCriteria eventSearchCriteria = new EventSearchCriteria.Builder(TEST_APP_ID, TEST_FROM, TEST_TO)
                .setSearchEventId(testEventId).build();
        EventSearchResult result = logSearch.searchEvent(eventSearchCriteria);
        verifyEventSearchResults(result);

        // please note that space should be escaped to '+' since Wiremock doesn't handle it when matching...
        String dateRangeValue = "%s:[%s+TO+%s]";

        Instant fromInstant = Instant.ofEpochMilli(TEST_FROM);
        Instant toInstant = Instant.ofEpochMilli(TEST_TO);

        dateRangeValue = String.format(dateRangeValue, COLUMN_NAME_LOG_TIME, fromInstant.toString(), toInstant.toString());

        List<LoggedRequest> requests = wireMockRule.findAll(getRequestedFor(urlPathEqualTo(STUB_REQUEST_API_PATH)));
        assertEquals(1, requests.size());

        LoggedRequest request = requests.get(0);

        QueryParameter qParam = request.queryParameter("q");
        assertTrue(qParam.containsValue(COLUMN_NAME_STREAMLINE_EVENT_ID + ":*"));

        QueryParameter fqParam = request.queryParameter("fq");
        assertTrue(fqParam.containsValue(COLUMN_NAME_TYPE + ":" + COLUMN_VALUE_TYPE_EVENT));
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID + ":" + TEST_APP_ID));
        assertTrue(fqParam.containsValue(dateRangeValue));
        assertFalse(fqParam.hasValueMatching(ValuePattern.containing(COLUMN_NAME_STREAMLINE_COMPONENT_NAME)));

        String expectedEventIdQuery = COLUMN_NAME_STREAMLINE_EVENT_ID + ":" + testEventId;
        expectedEventIdQuery += "+OR+" + COLUMN_NAME_STREAMLINE_EVENT_ROOT_ID_SET + ":*" + testEventId + "*";
        expectedEventIdQuery += "+OR+" + COLUMN_NAME_STREAMLINE_EVENT_PARENT_ID_SET + ":*" + testEventId + "*";
        assertTrue(fqParam.containsValue(expectedEventIdQuery));

        QueryParameter sortParam = request.queryParameter("sort");
        assertTrue(sortParam.containsValue(COLUMN_NAME_LOG_TIME + "+asc"));
    }

    @Test
    public void testEventSearchWithKeyValuesQueryAndHeadersQuery() throws Exception {
        stubSolrUrl();

        String searchQuery = "hello=world";

        EventSearchCriteria eventSearchCriteria = new EventSearchCriteria.Builder(TEST_APP_ID, TEST_FROM, TEST_TO)
                .setSearchString(searchQuery).build();
        EventSearchResult result = logSearch.searchEvent(eventSearchCriteria);
        verifyEventSearchResults(result);

        // please note that space should be escaped to '+' since Wiremock doesn't handle it when matching...
        String dateRangeValue = "%s:[%s+TO+%s]";

        Instant fromInstant = Instant.ofEpochMilli(TEST_FROM);
        Instant toInstant = Instant.ofEpochMilli(TEST_TO);

        dateRangeValue = String.format(dateRangeValue, COLUMN_NAME_LOG_TIME, fromInstant.toString(), toInstant.toString());

        List<LoggedRequest> requests = wireMockRule.findAll(getRequestedFor(urlPathEqualTo(STUB_REQUEST_API_PATH)));
        assertEquals(1, requests.size());

        LoggedRequest request = requests.get(0);

        QueryParameter qParam = request.queryParameter("q");
        String expectedQuery = COLUMN_NAME_STREAMLINE_EVENT_ID + ":*";
        expectedQuery += "+AND+(";
        expectedQuery += COLUMN_NAME_STREAMLINE_EVENT_KEYVALUES + ":" + searchQuery;
        expectedQuery += "+OR+" + COLUMN_NAME_STREAMLINE_EVENT_HEADERS + ":" + searchQuery;
        expectedQuery += "+OR+" + COLUMN_NAME_STREAMLINE_EVENT_AUX_KEYVALUES + ":" + searchQuery;
        expectedQuery += ")";

        assertTrue(qParam.containsValue(expectedQuery));

        QueryParameter fqParam = request.queryParameter("fq");
        assertTrue(fqParam.containsValue(COLUMN_NAME_TYPE + ":" + COLUMN_VALUE_TYPE_EVENT));
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID + ":" + TEST_APP_ID));
        assertTrue(fqParam.containsValue(dateRangeValue));
        assertFalse(fqParam.hasValueMatching(ValuePattern.containing(COLUMN_NAME_STREAMLINE_COMPONENT_NAME)));

        QueryParameter sortParam = request.queryParameter("sort");
        assertTrue(sortParam.containsValue(COLUMN_NAME_LOG_TIME + "+asc"));
    }

    private void verifyLogSearchResults(LogSearchResult results) {
        assertEquals(Long.valueOf(1434L), results.getMatchedDocs());

        // 5 static rows are presented in pre-stored result for making just test simpler...
        // please refer 'ambari-infra-log-search-output.xml'
        List<LogSearchResult.LogDocument> documents = results.getDocuments();
        assertEquals(5, documents.size());

        LogSearchResult.LogDocument document = documents.get(0);
        assertEquals("1", document.getAppId());
        assertEquals("SOURCE", document.getComponentName());
        assertEquals("DEBUG", document.getLogLevel());
        assertEquals("host1", document.getHost());
        assertEquals(Integer.valueOf(6700), document.getPort());
        assertTrue(document.getLogMessage().startsWith("Polled [0] records from Kafka."));
        assertEquals(Instant.parse("2017-11-24T07:48:58.347Z"), Instant.ofEpochMilli(document.getTimestamp()));

        document = documents.get(1);
        assertEquals("1", document.getAppId());
        assertEquals("SOURCE", document.getComponentName());
        assertEquals("DEBUG", document.getLogLevel());
        assertEquals("host1", document.getHost());
        assertEquals(Integer.valueOf(6700), document.getPort());
        assertTrue(document.getLogMessage().startsWith("Topic partitions with entries"));
        assertEquals(Instant.parse("2017-11-24T07:48:58.348Z"), Instant.ofEpochMilli(document.getTimestamp()));

        document = documents.get(2);
        assertEquals("1", document.getAppId());
        assertEquals("SOURCE", document.getComponentName());
        assertEquals("DEBUG", document.getLogLevel());
        assertEquals("host1", document.getHost());
        assertEquals(Integer.valueOf(6700), document.getPort());
        assertTrue(document.getLogMessage().startsWith("Polled [0] records from Kafka."));
        assertEquals(Instant.parse("2017-11-24T07:48:58.549Z"), Instant.ofEpochMilli(document.getTimestamp()));

        document = documents.get(3);
        assertEquals("1", document.getAppId());
        assertEquals("SOURCE", document.getComponentName());
        assertEquals("DEBUG", document.getLogLevel());
        assertEquals("host1", document.getHost());
        assertEquals(Integer.valueOf(6700), document.getPort());
        assertTrue(document.getLogMessage().startsWith("Topic partitions with entries"));
        assertEquals(Instant.parse("2017-11-24T07:48:58.55Z"), Instant.ofEpochMilli(document.getTimestamp()));

        document = documents.get(4);
        assertEquals("1", document.getAppId());
        assertEquals("SOURCE", document.getComponentName());
        assertEquals("INFO", document.getLogLevel());
        assertEquals("host1", document.getHost());
        assertEquals(Integer.valueOf(6700), document.getPort());
        assertTrue(document.getLogMessage().startsWith("Trying to load entry for cache"));
        assertEquals(Instant.parse("2017-11-24T15:43:37.902Z"), Instant.ofEpochMilli(document.getTimestamp()));

    }

    private void verifyEventSearchResults(EventSearchResult results) {
        assertEquals(Long.valueOf(1450L), results.getMatchedEvents());

        // 1 static rows are presented in pre-stored result for making just test simpler...
        // please refer 'ambari-infra-event-search-output.xml'
        List<EventSearchResult.Event> events = results.getEvents();
        assertEquals(1, events.size());

        EventSearchResult.Event event = events.get(0);
        assertEquals("11", event.getAppId());
        assertEquals("RULE", event.getComponentName());
        assertEquals("b7715c60-74ad-43dd-814a-8a40403a31bc", event.getEventId());
        assertTrue(event.getRootIds().contains("8e1bd283-1146-43c9-b7d0-16884beb2011"));
        assertTrue(event.getParentIds().contains("8e1bd283-1146-43c9-b7d0-16884beb2011"));
        assertTrue(event.getKeyValues().contains("user_id=Sjb5e5-gKoLXueFDMc2R8Q"));
        assertTrue(event.getKeyValues().contains("review_id=2_Ru_ASf75kU303rdQjFfQ"));
        assertTrue(event.getHeaders().contains("sourceComponentName=RULE"));
        assertNull(event.getAuxKeyValues());

        assertEquals(Instant.parse("2017-12-15T16:46:35.229Z"), Instant.ofEpochMilli(event.getTimestamp()));
    }

    private void stubSolrUrl() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/ambari-infra-log-search-output.xml")) {
            String body = IOUtils.toString(is, Charset.forName("UTF-8"));

            UrlMatchingStrategy strategyForWorkerLog = urlPathMatching(STUB_REQUEST_API_PATH + ".+" +
                    AmbariInfraWithStormLogSearch.COLUMN_NAME_TYPE + "%3A" +
                    AmbariInfraWithStormLogSearch.COLUMN_VALUE_TYPE_WORKER_LOG + ".+");
            wireMockRule.stubFor(get(strategyForWorkerLog)
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/xml; charset=UTF-8")
                            .withHeader("Transfer-Encoding", "chunked")
                            .withBody(body)));
        }

        try (InputStream is = getClass().getResourceAsStream("/ambari-infra-event-search-output.xml")) {
            String body = IOUtils.toString(is, Charset.forName("UTF-8"));

            UrlMatchingStrategy strategyForWorkerLog = urlPathMatching(STUB_REQUEST_API_PATH + ".+" +
                    AmbariInfraWithStormLogSearch.COLUMN_NAME_TYPE + "%3A" +
                    AmbariInfraWithStormLogSearch.COLUMN_VALUE_TYPE_EVENT + ".+");
            wireMockRule.stubFor(get(strategyForWorkerLog)
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/xml; charset=UTF-8")
                            .withHeader("Transfer-Encoding", "chunked")
                            .withBody(body)));
        }
    }

}
