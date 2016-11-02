package org.apache.streamline.streams.catalog.service;

import org.apache.streamline.common.QueryParam;
import org.apache.streamline.streams.catalog.UDFInfo;
import org.apache.streamline.streams.layout.component.Edge;
import org.apache.streamline.streams.layout.component.StreamlineProcessor;
import org.apache.streamline.streams.layout.component.StreamlineSink;
import org.apache.streamline.streams.layout.component.StreamlineSource;
import org.apache.streamline.streams.layout.component.TopologyDagVisitor;
import org.apache.streamline.streams.layout.component.impl.RulesProcessor;
import org.apache.streamline.streams.layout.component.rule.Rule;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StormTopologyExtraJarsHandler extends TopologyDagVisitor {
    private final Set<String> extraJars = new HashSet<>();
    private final StreamCatalogService catalogService;

    StormTopologyExtraJarsHandler(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public void visit(RulesProcessor rulesProcessor) {
        Set<UDFInfo> udfsToShip = new HashSet<>();
        for (Rule rule : rulesProcessor.getRules()) {
            for (String udf : rule.getReferredUdfs()) {
                Collection<UDFInfo> udfInfos = catalogService.listUDFs(Collections.singletonList(new QueryParam(UDFInfo.NAME, udf)));
                if (udfInfos.size() > 1) {
                    throw new IllegalArgumentException("Multiple UDF definitions with name:" + udf);
                } else if (udfInfos.size() == 1) {
                    udfsToShip.add(udfInfos.iterator().next());
                }
            }
            for (UDFInfo udf : udfsToShip) {
                extraJars.add(udf.getJarStoragePath());
            }
        }
    }

    @Override
    public void visit(Edge edge) {
    }

    @Override
    public void visit(StreamlineSource source) {
        extraJars.addAll(source.getExtraJars());
    }

    @Override
    public void visit(StreamlineSink sink) {
        extraJars.addAll(sink.getExtraJars());
    }

    @Override
    public void visit(StreamlineProcessor processor) {
        extraJars.addAll(processor.getExtraJars());
    }

    public Set<String> getExtraJars() {
        return extraJars;
    }
}
