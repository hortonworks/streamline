package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.util.JarStorage;
import com.hortonworks.iotas.util.ReflectionHelper;
import com.hortonworks.iotas.webservice.IotasConfiguration;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ParserInfoCatalogResource {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private CatalogService catalogService;
    private IotasConfiguration configuration;
    private JarStorage jarStorage;

    public ParserInfoCatalogResource(CatalogService service, IotasConfiguration configuration) {
        this.catalogService = service;
        this.configuration = configuration;
        try {
            this.jarStorage = ReflectionHelper.newInstance(this.configuration
                    .getJarStorageConfiguration().getClassName());
            this.jarStorage.init(configuration.getJarStorageConfiguration().getProperties());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @GET
    @Path("/parsers")
    @Timed
    // TODO add a way to query/filter and/or page results
    public Response listParsers() {
        try {
            Collection<ParserInfo> parserInfos = catalogService.listParsers();
            return WSUtils.respond(OK, SUCCESS, parserInfos);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/parsers/{id}")
    @Timed
    public Response getParserInfoById(@PathParam("id") Long parserId) {
        try {
            ParserInfo result = doGetParserInfoById(parserId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, parserId.toString());
    }

    @DELETE
    @Path("/parsers/{id}")
    @Timed
    public Response removeParser(@PathParam("id") Long parserId) {
        try {
            ParserInfo removedParser = catalogService.removeParser(parserId);
            if (removedParser != null) {
                return WSUtils.respond(OK, SUCCESS, removedParser);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, parserId.toString());
            }
        }  catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    //Test curl command curl -X POST -i -F parserJar=@original-webservice-0.1-SNAPSHOT.jar -F parserInfo='{"parserName":"TestParser","className":"some.test.parserClass","version":0}' http://localhost:8080/api/v1/catalog/parsers
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/parsers")
    public Response addParser(@FormDataParam("parserJar") final InputStream inputStream, @FormDataParam("parserJar") final FormDataContentDisposition contentDispositionHeader,
                             @FormDataParam("parserInfo") final String parserInfoStr) {
        File file = null;
        try {
          String name = "";
          if(contentDispositionHeader != null && contentDispositionHeader.getFileName() != null) {
            name = contentDispositionHeader.getFileName();
            this.jarStorage.uploadJar(inputStream, name);
            inputStream.close();
          }

            //TODO something special about multipart request so it wont let me pass just a ParserInfo json object, instead we must pass ParserInfo as a json string.
            ParserInfo parserInfo = objectMapper.readValue(parserInfoStr, ParserInfo.class);
            parserInfo.setJarStoragePath(name);
            ParserInfo result = catalogService.addParserInfo(parserInfo);
            return WSUtils.respond(CREATED, SUCCESS, parserInfo);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    //TODO Still need to implement update/PUT

    //TODO, is it better to expect that clients will know the parserId or the jarStoragePath? I like parserId as it
    //hides the storage details from clients.
    @Timed
    @GET
    @Produces({"application/java-archive", "application/json"})
    @Path("/parsers/download/{parserId}")
    public Response downloadParserJar(@PathParam("parserId") Long parserId) {
        try {
            ParserInfo parserInfo = doGetParserInfoById(parserId);
            if(parserInfo != null) {
                final InputStream inputStream = this.jarStorage.downloadJar(parserInfo.getJarStoragePath());
                StreamingOutput streamOutput = new StreamingOutput() {
                    public void write(OutputStream os) throws IOException, WebApplicationException {
                        try {
                            ByteStreams.copy(inputStream, os);
                        } finally {
                            os.close();
                        }
                    }
                };
                return Response.ok(streamOutput).build();
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, parserId.toString());
    }


    private ParserInfo doGetParserInfoById(Long parserId) {
        return catalogService.getParserInfo(parserId);
    }
}
