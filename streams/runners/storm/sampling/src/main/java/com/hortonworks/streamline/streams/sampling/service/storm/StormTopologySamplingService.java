package com.hortonworks.streamline.streams.sampling.service.storm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.sampling.service.TopologySampling;
import com.hortonworks.streamline.streams.storm.common.StormRestAPIClient;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hortonworks.streamline.streams.common.StreamlineEventImpl.TO_STRING_PREFIX;

public class StormTopologySamplingService implements TopologySampling {
    private static final Logger LOG = LoggerFactory.getLogger(StormTopologySamplingService.class);

    private static final String LOG_CONTENT = "logContent";
    private static final Pattern START_REGEX = Pattern.compile("start=(\\d+)", Pattern.DOTALL);
    public static final int BYTES_TO_FETCH = 51200;

    private StormRestAPIClient client;

    public StormTopologySamplingService() {
    }

    @Override
    public void init(Map<String, Object> conf) {
        String stormApiRootUrl = null;
        Subject subject = null;
        if (conf != null) {
            stormApiRootUrl = (String) conf.get(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY);
            subject = (Subject) conf.get(TopologyLayoutConstants.SUBJECT_OBJECT);
        }
        Client restClient = ClientBuilder.newClient(new ClientConfig());
        this.client = new StormRestAPIClient(restClient, stormApiRootUrl, subject);
    }

    @Override
    public boolean enableSampling(Topology topology, int pct, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        return client.enableSampling(topologyId, pct, asUser);
    }

    @Override
    public boolean enableSampling(Topology topology, TopologyComponent component, int pct, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        return client.enableSampling(topologyId, component.getId() + "-" + component.getName(), pct, asUser);
    }

    @Override
    public boolean disableSampling(Topology topology, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        return client.disableSampling(topologyId, asUser);
    }

    @Override
    public boolean disableSampling(Topology topology, TopologyComponent component, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        return client.disableSampling(topologyId, component.getId() + "-" + component.getName(), asUser);
    }

    @Override
    public SamplingStatus getSamplingStatus(Topology topology, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        return buildSamplingStatus(client.getSamplingStatus(topologyId, asUser));
    }

    @Override
    public SamplingStatus getSamplingStatus(Topology topology, TopologyComponent component, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        return buildSamplingStatus(client.getSamplingStatus(topologyId, asUser));
    }

    @Override
    public SampledEvents getSampledEvents(Topology topology, TopologyComponent component, EventQueryParams qps, String asUser) {
        List<SampledEvent> events = new ArrayList<>(qps.count());
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        String componentId = component.getId() + "-" + component.getName();
        Long cur = null;
        Long next;
        if (qps.desc()) {
            next = qps.start();
        } else {
            next = qps.start() == null ? Long.valueOf(0) : qps.start();
        }
        Long nextOffset = null;
        Long prevOffset = null;
        while (events.size() < qps.count()) {
            cur = next;
            String res = client.getSampledEvents(topologyId, componentId, asUser, cur, qps.length());
            Document doc = Jsoup.parse(res);
            Element logContent = doc.getElementById(LOG_CONTENT);
            if (logContent == null) {
                break;
            }
            String lines = logContent.text();
            if (!StringUtils.isEmpty(lines)) {
                List<SampledEvent> curEvents = new ArrayList<>();
                addSampledEvents(lines, componentId, curEvents);
                if (qps.desc()) {
                    Collections.reverse(curEvents);
                }
                events.addAll(curEvents);
            }
            Element prevElement = getFirstMatch(doc.getElementsByClass("btn btn-default enabled"), "Prev");
            if (prevElement != null) {
                Matcher match = START_REGEX.matcher(prevElement.attributes().get("href"));
                if (match.find()) {
                    prevOffset = Long.parseLong(match.group(1));
                }
            }
            Element nextElement = getFirstMatch(doc.getElementsByClass("btn btn-default enabled"), "Next");
            if (nextElement != null) {
                Matcher match = START_REGEX.matcher(nextElement.attributes().get("href"));
                if (match.find()) {
                    nextOffset = Long.parseLong(match.group(1));
                }
            }
            if (qps.desc()) {
                if (prevOffset == null) {
                    break;
                } else {
                    next = prevOffset;
                }
            } else {
                if (nextOffset == null) {
                    break;
                } else {
                    next = nextOffset;
                }
            }
        }

        long nextStart = 0;
        Integer length = null;
        if (!events.isEmpty()) {
            if (events.size() > qps.count()) {
                events = events.subList(0, qps.count());
            }
            SampledEvent lastEvent = events.get(events.size() - 1);
            if (qps.desc()) {
                if (cur != null) {
                    nextStart = cur + lastEvent.getStartOffset() - qps.length() + 1;
                } else if (prevOffset != null) {
                    nextStart = prevOffset + lastEvent.getStartOffset() + 1;
                }
                if (nextStart >= 0) {
                    length = qps.length();
                } else if (cur != null && cur > 0 && cur < qps.length()){
                    length = lastEvent.getStartOffset() + cur.intValue() + 1;
                } else {
                    length = lastEvent.getStartOffset() + 1;
                }
                nextStart = Math.max(nextStart, 0);
            } else {
                nextStart = (cur == null ? 0 : cur) + lastEvent.getStartOffset() + lastEvent.getLength() - 1;
                length = qps.length() == null ? Integer.valueOf(BYTES_TO_FETCH) : qps.length();
            }
        }
        return buildSampledEvents(nextStart, length, events);
    }

    private SampledEvents buildSampledEvents(Long next, Integer length, List<SampledEvent> events) {
        return new SampledEvents() {
            @Override
            public Collection<SampledEvent> getEvents() {
                return events;
            }

            @Override
            public Long getNext() {
                return next;
            }

            @Override
            public Integer getLength() {
                return length;
            }
        };
    }

    private void addSampledEvents(String lines, String queriedComponentId, List<SampledEvent> events) {
        // Timestamp, Component name, Component task-id, MessageId (incase of anchoring), List of emitted values
        int offset = 0;
        for (String line : lines.split("\n")) {
            line = StringEscapeUtils.unescapeHtml(line);
            int componentStart = line.indexOf(',') + 1;
            if (componentStart != 0) {
                int componentEnd = line.indexOf(',', componentStart);
                if (componentEnd != -1) {
                    String componentName = line.substring(componentStart, componentEnd);
                    try {
                        if (componentName.equals(queriedComponentId)) {
                            long time = Date.parse(line.substring(0, componentStart - 1));
                            int start = line.indexOf(TO_STRING_PREFIX) + TO_STRING_PREFIX.length();
                            if (start >= TO_STRING_PREFIX.length()) {
                                int end = line.lastIndexOf("]");
                                if (end > start) {
                                    String eventStr = line.substring(start, end);
                                    events.add(buildSampledEvent(time, eventStr, offset, line.length()));
                                }
                            }
                        } else {
                            LOG.trace("Skipping sampled event for component {}", componentName);
                        }
                    } catch (RuntimeException ex) {
                        LOG.error("Not able to parse date {}", line.substring(0, componentStart - 1));
                    }
                }
            }
            offset += line.length() + 1;
        }
    }

    private SamplingStatus buildSamplingStatus(Map result) {
        return result == null ? null : new SamplingStatus() {
            @Override
            public Boolean getEnabled() {
                Object debug = result.get("debug");
                return debug != null && debug instanceof Boolean ? (Boolean) debug : false;
            }

            @Override
            public Integer getPct() {
                Object samplingPct = result.get("samplingPct");
                return samplingPct != null && samplingPct instanceof Number ? ((Number) samplingPct).intValue() : 0;
            }
        };
    }

    private Element getFirstMatch(Elements elements, String match) {
        for (Element element : elements) {
            if (element.text().equals(match)) {
                return element;
            }
        }
        return null;
    }

    private SampledEvent buildSampledEvent(long ts, String event, int startOffset, int length) {
        return new SimpleSampledEvent(ts, event, startOffset, length);
    }

    private static class SimpleSampledEvent implements SampledEvent {
        private final long time;
        private final String event;
        private final int startOffset;
        private final int length;

        public SimpleSampledEvent(long time, String event, int startOffset, int length) {
            this.time = time;
            this.event = event;
            this.startOffset = startOffset;
            this.length = length;
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        @JsonRawValue
        public String getEvent() {
            return event;
        }

        @Override
        @JsonIgnore
        public int getStartOffset() {
            return startOffset;
        }

        @Override
        @JsonIgnore
        public int getLength() {
            return length;
        }

    }
}
