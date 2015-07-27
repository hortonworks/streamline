package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.storage.StorageManager;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.file.FileSystems;
import java.util.Collection;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ParserInfoCatalogResource {
    // TODO should probably make namespace static
    private static final String PARSER_INFO_NAMESPACE = new ParserInfo().getNameSpace();

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

    //Test curl command curl -X POST -i  -F file=@original-webservice-0.1-SNAPSHOT.jar http://localhost:8080/api/v1/catalog/parsers/upload
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/parsers/upload")
    public String uploadFile(@FormDataParam("file") final InputStream inputStream, @FormDataParam("file") final FormDataContentDisposition contentDispositionHeader) throws IOException {
        String name = contentDispositionHeader.getFileName();
        java.nio.file.Path path = FileSystems.getDefault().getPath(configuration.getCatalogHomeDir(), name);
        File file = path.toFile();
        if(!file.exists()) {
            file.createNewFile();
        }
        ByteStreams.copy(inputStream, new FileOutputStream(file));
        return file.getAbsolutePath();
    }

    //TODO, is it better to expect that clients will know the parserId or the jarStoragePath? I like parserId as it
    //hides the storage details from clients.
    @Timed
    @GET
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/parsers/download/{parserId}")
    public StreamingOutput downloadFile(@PathParam("parserId") Long parserId) throws IOException {
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
