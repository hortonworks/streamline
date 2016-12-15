package org.apache.streamline.streams.catalog.container;

public interface ContainingNamespaceAwareContainer {
    void invalidateInstance(Long namespaceId);
}
