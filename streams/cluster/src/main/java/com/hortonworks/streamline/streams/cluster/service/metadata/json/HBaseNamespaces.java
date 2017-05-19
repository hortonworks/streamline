package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.hadoop.hbase.NamespaceDescriptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.SecurityContext;

/**
 * Wrapper used to show proper JSON formatting
 */
@JsonPropertyOrder({"namespaces", "security"})
public class HBaseNamespaces {
    private final List<String> namespaces;
    private final Security security;

    public HBaseNamespaces(List<String> namespaces, Security security) {
        this.namespaces = namespaces;
        this.security = security;
    }

    public static HBaseNamespaces newInstance(NamespaceDescriptor[] namespaceDescriptors, SecurityContext securityContext,
                                              boolean isAuthorizerInvoked, Principals principals, Keytabs keytabs) {

        List<String> namespaces = Collections.emptyList();
        if (namespaceDescriptors != null) {
            namespaces = Arrays.stream(namespaceDescriptors).map(NamespaceDescriptor::getName).collect(Collectors.toList());
        }
        return new HBaseNamespaces(namespaces, new Security(securityContext, new Authorizer(isAuthorizerInvoked), principals, keytabs));
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public Security getSecurity() {
        return security;
    }

    @Override
    public String toString() {
        return "HBaseNamespaces{" +
                "namespaces=" + namespaces +
                ", security=" + security +
                '}';
    }
}
