package org.apache.streamline.streams.runtime.storm.bolt.solr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.storm.solr.mapper.SolrMapper;
import org.apache.storm.solr.mapper.SolrMapperException;
import org.apache.storm.tuple.ITuple;
import org.apache.streamline.streams.StreamlineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Extracts JSON
 */
public class StreamlineSolrJsonMapper implements SolrMapper {
    private static final String CONTENT_TYPE = "application/json";
    private static final Logger LOG = LoggerFactory.getLogger(StreamlineSolrJsonMapper.class);

    private final String jsonUpdateUrl;
    private final String collection;
    private final ObjectMapper objectMapper;

    public StreamlineSolrJsonMapper(String jsonUpdateUrl, String collection, ObjectMapper objectMapper) {
        this.jsonUpdateUrl = jsonUpdateUrl;
        this.collection = collection;
        this.objectMapper = objectMapper;
    }

    public StreamlineSolrJsonMapper(String collection) {
        this("/update/json/docs", collection, new ObjectMapper());
    }

    @Override
    public String getCollection() {
        return collection;
    }

    @Override
    public SolrRequest toSolrRequest(List<? extends ITuple> tupleList) throws SolrMapperException {
        return createSolrRequest(getJsonFromTuples(tupleList));
    }

    @Override
    public SolrRequest toSolrRequest(ITuple tuple) throws SolrMapperException {
        final String json = getJsonFromTuple(tuple);
        return createSolrRequest(json);
    }

    private String getJsonFromTuple(ITuple tuple) throws SolrMapperException {
        final StreamlineEvent event = (StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new SolrMapperException(e.getMessage());
        }
    }

    private String getJsonFromTuples(List<? extends ITuple> tuples) throws SolrMapperException {
        final StringBuilder jsonListBuilder = new StringBuilder("[");
        for (ITuple tuple : tuples) {
            final String json = getJsonFromTuple(tuple);
            jsonListBuilder.append(json).append(",");
        }
        jsonListBuilder.setCharAt(jsonListBuilder.length() - 1, ']');
        return jsonListBuilder.toString();
    }

    private SolrRequest createSolrRequest(String json) {
        final ContentStreamUpdateRequest request = new ContentStreamUpdateRequest(jsonUpdateUrl);
        final ContentStream cs = new ContentStreamBase.StringStream(json, CONTENT_TYPE);
        request.addContentStream(cs);
        LOG.debug("Request generated with JSON: {}", json);
        return request;
    }
}
