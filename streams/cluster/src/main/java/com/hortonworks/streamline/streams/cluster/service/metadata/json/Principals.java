package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.security.SecurityUtil;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class Principals {
    private Map<String, String> principals;     //Key is the name of the service component of the principal. Val is principal

    public Principals(Map<String, String> principals) {
        this.principals = principals;
    }

    public static Principals newInstance(ServiceConfiguration serviceConfig) throws IOException {
        return newInstance(serviceConfig.getConfigurationMap());
    }

    public static Principals newInstance(Map<String, String> principals) throws IOException {
        final Map<String, String> princs = principals.entrySet()
                .stream()
                .filter((e) -> e.getKey().contains("principal"))
                .collect(Collectors.toMap(
                        (e) -> {
                            String key = e.getKey().split("principal")[0];
                            return key.substring(0, key.length()-1);
                        },
                        (e) -> SecurityUtil.getUserName(e.getValue())));

        return new Principals(princs);
    }

    public Map<String, String> toMap() {
        return principals;
    }

    @Override
    public String toString() {
        return "{" +
                "Principals=" + principals +
                '}';
    }
}
