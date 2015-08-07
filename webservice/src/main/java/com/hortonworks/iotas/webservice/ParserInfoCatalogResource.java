package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.storage.StorageManager;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.file.FileSystems;
import java.util.Collection;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ParserInfoCatalogResource {
    // TODO should probably make namespace static
    private static final String PARSER_INFO_NAMESPACE = new ParserInfo().getNameSpace();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private StorageManager dao;
    private IotasConfiguration configuration;

    public ParserInfoCatalogResource(StorageManager manager, IotasConfiguration configuration) {
        this.dao = manager;
        this.configuration = configuration;
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

    @DELETE
    @Path("/parsers/{id}")
    @Timed
    public ParserInfo removeParser(@PathParam("id") Long parserId) {
        ParserInfo parserInfo = new ParserInfo();
        parserInfo.setParserId(parserId);
        return this.dao.remove(PARSER_INFO_NAMESPACE, parserInfo.getPrimaryKey());
    }

    //Test curl command curl -X POST -i -F parserJar=@original-webservice-0.1-SNAPSHOT.jar -F parserInfo='{"parserName":"TestParser","className":"some.test.parserClass","version":0}' http://localhost:8080/api/v1/catalog/parsers
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/parsers")
    public ParserInfo addParser(@FormDataParam("parserJar") final InputStream inputStream, @FormDataParam("parserJar") final FormDataContentDisposition contentDispositionHeader,
                             @FormDataParam("parserInfo") final String parserInfoStr) throws IOException {
        File file = null;
        if(contentDispositionHeader != null && contentDispositionHeader.getFileName() != null) {
            String name = contentDispositionHeader.getFileName();
            java.nio.file.Path path = FileSystems.getDefault().getPath(configuration.getCatalogHomeDir(), name);
            file = path.toFile();
            if (!file.exists()) {
                file.createNewFile();
            }
            ByteStreams.copy(inputStream, new FileOutputStream(file));
        }

        //TODO something special about multipart request so it wont let me pass just a ParserInfo json object, instead we must pass ParserInfo as a json string.
        ParserInfo parserInfo = objectMapper.readValue(new StringReader(parserInfoStr), ParserInfo.class);
        if (parserInfo.getParserId() == null) {
            parserInfo.setParserId(this.dao.nextId(PARSER_INFO_NAMESPACE));
        }
        if (parserInfo.getTimestamp() == null) {
            parserInfo.setTimestamp(System.currentTimeMillis());
        }
        parserInfo.setJarStoragePath(file.getAbsolutePath());
        this.dao.add(parserInfo);

        return parserInfo;
    }

    //TODO Still need to implement update/PUT

    //TODO, is it better to expect that clients will know the parserId or the jarStoragePath? I like parserId as it
    //hides the storage details from clients.
    @Timed
    @GET
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/parsers/download/{parserId}")
    public StreamingOutput downloadParserJar(@PathParam("parserId") Long parserId) throws IOException {
        ParserInfo parserInfo = getParserInfoById(parserId);
        final InputStream inputStream = new FileInputStream(parserInfo.getJarStoragePath());
        StreamingOutput streamOutput = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                ByteStreams.copy(inputStream, os);
            }
        };
        return streamOutput;
    }
}
