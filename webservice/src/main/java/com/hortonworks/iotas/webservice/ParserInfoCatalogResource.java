package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.storage.StorageManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ParserInfoCatalogResource {
    private StorageManager dao;
    // TODO should probably make namespace static
    private static final String PARSER_INFO_NAMESPACE = new ParserInfo().getNameSpace();

    public ParserInfoCatalogResource(StorageManager manager) {
        this.dao = manager;
    }

    @GET
    @Path("/parsers")
    @Timed
    // TODO add a way to query/filter and/or page results
    public Collection<ParserInfo> listParsers() {
        return this.dao.<ParserInfo>list(PARSER_INFO_NAMESPACE);
    }

    @GET
    @Path("/parsers/{id}")
    @Timed
    public ParserInfo getParserInfoById(@PathParam("id") Long parserId) {
        ParserInfo parserInfo = new ParserInfo();
        parserInfo.setParserId(parserId);
        return this.dao.<ParserInfo>get(PARSER_INFO_NAMESPACE, parserInfo.getPrimaryKey());
    }

    @POST
    @Path("/parsers")
    @Timed
    public ParserInfo addParserInfo(ParserInfo parserInfo) {
        if (parserInfo.getParserId() == null) {
            parserInfo.setParserId(this.dao.nextId(PARSER_INFO_NAMESPACE));
        }
        if (parserInfo.getTimestamp() == null) {
            parserInfo.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(parserInfo);
        return parserInfo;
    }

    @DELETE
    @Path("/parsers/{id}")
    @Timed
    public ParserInfo removeParser(@PathParam("id") Long parserId) {
        ParserInfo parserInfo = new ParserInfo();
        parserInfo.setParserId(parserId);
        return this.dao.remove(PARSER_INFO_NAMESPACE, parserInfo.getPrimaryKey());
    }

    @PUT
    @Path("/parsers")
    @Timed
    public ParserInfo addOrUpdateParserInfo(ParserInfo parserInfo) {
        if (parserInfo.getParserId() == null) {
            parserInfo.setParserId(this.dao.nextId(PARSER_INFO_NAMESPACE));
        }
        if (parserInfo.getTimestamp() == null) {
            parserInfo.setTimestamp(System.currentTimeMillis());
        }
        this.dao.addOrUpdate(parserInfo);
        return parserInfo;
    }
}
