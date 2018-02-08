package com.hortonworks.streamline.streams.logsearch.storm.ambari;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.logsearch.EventSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.EventSearchResult;
import com.hortonworks.streamline.streams.logsearch.LogSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.LogSearchResult;
import com.hortonworks.streamline.streams.logsearch.TopologyLogSearch;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of TopologyLogSearch for Ambari Infra (Solr) with Storm.
 * <p/>
 * This class assumes that worker logs are collected via LogFeeder with known configuration, and pushed to Ambari Infra (Solr).
 */
public class AmbariInfraWithStormLogSearch implements TopologyLogSearch {
    private static final Logger LOG = LoggerFactory.getLogger(AmbariInfraWithStormLogSearch.class);

    // the configuration keys
    static final String SOLR_API_URL_KEY = "solrApiUrl";
    static final String COLLECTION_NAME = "collection";
    static final String SECURED_CLUSTER = "secured";

    public static final String COLUMN_NAME_TYPE = "type";
    public static final String COLUMN_VALUE_TYPE_WORKER_LOG = "storm_worker";
    public static final String COLUMN_VALUE_TYPE_EVENT = "storm_worker_event";

    public static final String COLUMN_NAME_STREAMLINE_TOPOLOGY_ID = "sdi_streamline_topology_id";
    public static final String COLUMN_NAME_STREAMLINE_COMPONENT_NAME = "sdi_streamline_component_name";
    public static final String COLUMN_NAME_STORM_WORKER_PORT = "sdi_storm_worker_port";
    public static final String COLUMN_NAME_HOST = "host";
    public static final String COLUMN_NAME_LOG_TIME = "logtime";
    public static final String COLUMN_NAME_LOG_LEVEL = "level";
    public static final String COLUMN_NAME_LOG_MESSAGE = "log_message";

    public static final String COLUMN_NAME_STREAMLINE_EVENT_ID = "sdi_streamline_event_id";
    public static final String COLUMN_NAME_STREAMLINE_EVENT_ROOT_ID_SET = "sdi_streamline_root_ids";
    public static final String COLUMN_NAME_STREAMLINE_EVENT_PARENT_ID_SET = "sdi_streamline_parent_ids";
    public static final String COLUMN_NAME_STREAMLINE_EVENT_KEYVALUES = "sdi_streamline_event_fields_and_values";
    public static final String COLUMN_NAME_STREAMLINE_EVENT_HEADERS = "sdi_streamline_event_headers";
    public static final String COLUMN_NAME_STREAMLINE_EVENT_AUX_KEYVALUES = "sdi_streamline_event_aux_fields_and_values";

    public static final String DEFAULT_COLLECTION_NAME = "hadoop_logs";

    static final List<String> DEFAULT_LOG_LEVELS = Lists.newArrayList("INFO", "WARN", "ERROR");

    private HttpSolrClient solr;

    public AmbariInfraWithStormLogSearch() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Map<String, Object> conf) throws ConfigException {
        String solrApiUrl = null;
        String collectionName = null;
        if (conf != null) {
            solrApiUrl = (String) conf.get(SOLR_API_URL_KEY);
            collectionName = (String) conf.get(COLLECTION_NAME);
            if (collectionName == null) {
                collectionName = DEFAULT_COLLECTION_NAME;
            }
        }

        if (solrApiUrl == null || collectionName == null) {
            throw new ConfigException("'solrApiUrl' must be presented in configuration.");
        }

        if ((boolean) conf.getOrDefault(SECURED_CLUSTER, false)) {
            HttpClientUtil.addConfigurer(new Krb5HttpClientConfigurer());
        }
        solr = new HttpSolrClient.Builder(solrApiUrl + "/" + collectionName).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogSearchResult search(LogSearchCriteria logSearchCriteria) {
        SolrQuery query = new SolrQuery();

        query.setQuery(buildColumnAndValue(COLUMN_NAME_LOG_MESSAGE, buildValue(logSearchCriteria.getSearchString())));
        query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_TYPE, COLUMN_VALUE_TYPE_WORKER_LOG));
        query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID, buildValue(logSearchCriteria.getAppId())));
        query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_LOG_TIME, buildDateRangeValue(logSearchCriteria.getFrom(), logSearchCriteria.getTo())));

        List<String> componentNames = logSearchCriteria.getComponentNames();
        if (componentNames != null && !componentNames.isEmpty()) {
            query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME, buildORValues(componentNames)));
        }

        List<String> logLevels = logSearchCriteria.getLogLevels();
        if (logLevels == null || logLevels.isEmpty()) {
            logLevels = DEFAULT_LOG_LEVELS;
        }
        query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_LOG_LEVEL, buildORValues(logLevels)));

        if (logSearchCriteria.getAscending() == null || logSearchCriteria.getAscending()) {
            query.addSort(COLUMN_NAME_LOG_TIME, SolrQuery.ORDER.asc);
        } else {
            query.addSort(COLUMN_NAME_LOG_TIME, SolrQuery.ORDER.desc);
        }

        if (logSearchCriteria.getStart() != null) {
            query.setStart(logSearchCriteria.getStart());
        }
        if (logSearchCriteria.getLimit() != null) {
            query.setRows(logSearchCriteria.getLimit());
        }

        LOG.debug("Querying to Solr: query => {}", query);

        long numFound;
        List<LogSearchResult.LogDocument> results = new ArrayList<>();
        try {
            QueryResponse response = solr.query(query);

            SolrDocumentList docList = response.getResults();
            numFound = docList.getNumFound();

            for (SolrDocument document : docList) {
                String appId = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID);
                String componentName = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME);
                String logLevel = (String) document.getFieldValue(COLUMN_NAME_LOG_LEVEL);
                String logMessage = (String) document.getFieldValue(COLUMN_NAME_LOG_MESSAGE);
                String host = (String) document.getFieldValue(COLUMN_NAME_HOST);
                String port = (String) document.getFieldValue(COLUMN_NAME_STORM_WORKER_PORT);
                Date logDate = (Date) document.getFieldValue(COLUMN_NAME_LOG_TIME);
                long timestamp = logDate.toInstant().toEpochMilli();

                LogSearchResult.LogDocument logDocument = new LogSearchResult.LogDocument(appId, componentName,
                        logLevel, logMessage, host, port != null ? Integer.parseInt(port) : null, timestamp);
                results.add(logDocument);
            }

        } catch (SolrServerException | IOException e) {
            // TODO: any fine-grained control needed?
            throw new RuntimeException(e);
        }

        return new LogSearchResult(numFound, results);
    }

    @Override
    public EventSearchResult searchEvent(EventSearchCriteria criteria) {
        SolrQuery query = new SolrQuery();

        String searchString = criteria.getSearchString();

        List<String> queryStrings = new ArrayList<>();
        addQueryStringToListOnlyIfAvailable(queryStrings, searchString, COLUMN_NAME_STREAMLINE_EVENT_KEYVALUES);
        addQueryStringToListOnlyIfAvailable(queryStrings, searchString, COLUMN_NAME_STREAMLINE_EVENT_HEADERS);
        addQueryStringToListOnlyIfAvailable(queryStrings, searchString, COLUMN_NAME_STREAMLINE_EVENT_AUX_KEYVALUES);

        // this is to get rid of non-streamline events
        String queryString = buildColumnAndValue(COLUMN_NAME_STREAMLINE_EVENT_ID, buildValue(null));

        if (!queryStrings.isEmpty()) {
            queryString += " AND (" + String.join(" OR ", queryStrings) + ")";
        }

        query.setQuery(queryString);

        query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_TYPE, COLUMN_VALUE_TYPE_EVENT));
        query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID, buildValue(criteria.getAppId())));
        query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_LOG_TIME, buildDateRangeValue(criteria.getFrom(), criteria.getTo())));

        List<String> componentNames = criteria.getComponentNames();
        if (componentNames != null && !componentNames.isEmpty()) {
            query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME, buildORValues(componentNames)));
        }

        String searchEventId = criteria.getSearchEventId();
        if (searchEventId != null) {
            // eventId OR rootId OR parentId
            String queryToEventId = buildColumnAndValue(COLUMN_NAME_STREAMLINE_EVENT_ID, buildValue(searchEventId));
            String queryToRootIds = buildColumnAndValue(COLUMN_NAME_STREAMLINE_EVENT_ROOT_ID_SET, buildValue("*" + searchEventId + "*"));
            String queryToParentIds = buildColumnAndValue(COLUMN_NAME_STREAMLINE_EVENT_PARENT_ID_SET, buildValue("*" + searchEventId + "*"));
            query.addFilterQuery(queryToEventId + " OR " + queryToRootIds + " OR " + queryToParentIds);
        }

        if (criteria.getAscending() == null || criteria.getAscending()) {
            query.addSort(COLUMN_NAME_LOG_TIME, SolrQuery.ORDER.asc);
        } else {
            query.addSort(COLUMN_NAME_LOG_TIME, SolrQuery.ORDER.desc);
        }

        if (criteria.getStart() != null) {
            query.setStart(criteria.getStart());
        }
        if (criteria.getLimit() != null) {
            query.setRows(criteria.getLimit());
        }

        LOG.debug("Querying to Solr: query => {}", query);

        long numFound;
        List<EventSearchResult.Event> results = new ArrayList<>();
        try {
            QueryResponse response = solr.query(query);

            SolrDocumentList docList = response.getResults();
            numFound = docList.getNumFound();

            for (SolrDocument document : docList) {
                String appId = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID);
                String componentName = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME);
                String eventId = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_EVENT_ID);
                String rootIds = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_EVENT_ROOT_ID_SET);
                String parentIds = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_EVENT_PARENT_ID_SET);
                String keyValues = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_EVENT_KEYVALUES);
                String headers = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_EVENT_HEADERS);
                String auxKeyValues = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_EVENT_AUX_KEYVALUES);

                Date logDate = (Date) document.getFieldValue(COLUMN_NAME_LOG_TIME);
                long timestamp = logDate.toInstant().toEpochMilli();

                EventSearchResult.Event event = new EventSearchResult.Event(appId, componentName,
                        eventId, rootIds, parentIds, keyValues, headers, auxKeyValues, timestamp);
                results.add(event);
            }

        } catch (SolrServerException | IOException e) {
            // TODO: any fine-grained control needed?
            throw new RuntimeException(e);
        }

        return new EventSearchResult(numFound, results);
    }

    private void addQueryStringToListOnlyIfAvailable(List<String> queryStrings, String searchString, String columnName) {
        if (searchString != null && !searchString.isEmpty()) {
            queryStrings.add(buildColumnAndValue(columnName, searchString));
        }
    }

    private String buildColumnAndValue(String column, String value) {
        return column + ":" + value;
    }

    private String buildORValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Values should not be null or empty.");
        }

        if (values.size() > 1) {
            return "(" + String.join(" OR ", values) + ")";
        } else {
            // values.size() == 1
            return values.get(0);
        }
    }

    private String buildValue(String value) {
        if (value == null || value.isEmpty()) {
            return "*";
        }

        return value;
    }

    private String buildDateRangeValue(long from, long to) {
        String value = "[%s TO %s]";

        Instant fromInstant = Instant.ofEpochMilli(from);
        Instant toInstant = Instant.ofEpochMilli(to);

        return String.format(value, fromInstant.toString(), toInstant.toString());
    }

}
