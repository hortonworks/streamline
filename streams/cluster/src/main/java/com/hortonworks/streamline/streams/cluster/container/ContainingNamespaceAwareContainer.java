package com.hortonworks.streamline.streams.cluster.container;

public interface ContainingNamespaceAwareContainer {
    void invalidateInstance(Long namespaceId);
}
