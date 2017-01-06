package com.hortonworks.streamline.streams.catalog.container;

public interface ContainingNamespaceAwareContainer {
    void invalidateInstance(Long namespaceId);
}
