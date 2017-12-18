package com.hortonworks.streamline.streams.sampling.service;

import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.container.NamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.sampling.service.mapping.MappedTopologySamplingImpl;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;

public class TopologySamplingContainer extends NamespaceAwareContainer<TopologySampling> {
    private final Subject subject;

    public TopologySamplingContainer(EnvironmentService environmentService, Subject subject) {
        super(environmentService);
        this.subject = subject;
    }

    @Override
    protected TopologySampling initializeInstance(Namespace namespace) {
        try {
            String streamingEngine = namespace.getStreamingEngine();

            MappedTopologySamplingImpl samplingImpl;
            try {
                samplingImpl = MappedTopologySamplingImpl.valueOf(streamingEngine);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Unsupported streaming engine: " + streamingEngine, e);
            }

            Class<TopologySampling> clazz = (Class<TopologySampling>) Class.forName(samplingImpl.getClassName());
            TopologySampling samplingInstance = clazz.newInstance();
            samplingInstance.init(buildConfig(namespace, streamingEngine));
            return samplingInstance;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Map<String, Object> buildConfig(Namespace namespace, String streamingEngine) {
        Map<String, Object> conf = new HashMap<>();
        conf.put(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY, buildStormRestApiRootUrl(namespace, streamingEngine));
        conf.put(TopologyLayoutConstants.SUBJECT_OBJECT, subject);
        return conf;
    }

}
