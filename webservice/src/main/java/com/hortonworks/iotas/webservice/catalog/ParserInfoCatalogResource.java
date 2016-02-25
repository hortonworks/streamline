package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.Parser;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.util.JarStorage;
import com.hortonworks.iotas.util.ProxyUtil;
import com.hortonworks.iotas.util.ReflectionHelper;
import com.hortonworks.iotas.util.SchemaNamespaceUtil;
import com.hortonworks.iotas.webservice.IotasConfiguration;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.UUID;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.PARSER_SCHEMA_FOR_ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class ParserInfoCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(ParserInfoCatalogResource.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private CatalogService catalogService;
    private IotasConfiguration configuration;
    private JarStorage jarStorage;
    private final ProxyUtil<Parser> parserProxyUtil;

    public ParserInfoCatalogResource(CatalogService service, IotasConfiguration configuration) {
        this.catalogService = service;
        this.configuration = configuration;
        try {
            this.jarStorage = ReflectionHelper.newInstance(this.configuration
                    .getJarStorageConfiguration().getClassName());
            this.jarStorage.init(configuration.getJarStorageConfiguration().getProperties());
            this.parserProxyUtil = new ProxyUtil<>(Parser.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @GET
    @Path("/parsers")
    @Timed
    // TODO add a way to query/filter and/or page results
    public Response listParsers(@Context UriInfo uriInfo) {
        try {
            Collection<ParserInfo> parserInfos = null;
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            if (params == null || params.isEmpty()) {
                parserInfos = catalogService.listParsers();
            } else {
                parserInfos = catalogService.listParsers(WSUtils.buildQueryParameters(params));
            }
            return WSUtils.respond(OK, SUCCESS, parserInfos);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/parsers/{id}/schema")
    @Timed
    public Response getParserSchema(@PathParam("id") Long parserId) {
        try {
            ParserInfo parserInfo = doGetParserInfoById(parserId);
            if (parserInfo != null) {
                Schema result = parserInfo.getParserSchema();
                if (result != null) {
                    return WSUtils.respond(OK, SUCCESS, result);
                }
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, PARSER_SCHEMA_FOR_ENTITY_NOT_FOUND, parserId.toString());
    }

    @GET
    @Path("/parsers/{id}/schema/namespace/{namespace}")
    public Response getNamespaceAppliedParserSchema(@PathParam("id") Long parserId, @PathParam("namespace") String namespace) {
        try {
            ParserInfo parserInfo = doGetParserInfoById(parserId);
            if (parserInfo != null) {
                Schema result = parserInfo.getParserSchema();
                if (result != null) {
                    Schema applied = SchemaNamespaceUtil.applyNamespace(namespace, result);
                    return WSUtils.respond(OK, SUCCESS, applied);
                }
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, PARSER_SCHEMA_FOR_ENTITY_NOT_FOUND, parserId.toString());
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
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * Loads the parser jar and invoke the {@link Parser#schema} method
     * to get the schema.
     */
    private Schema loadSchemaFromParserJar(String jarName, String className) {
        OutputStream os = null;
        InputStream is = null;
        Schema result = null;
        try {
            File tmpFile = File.createTempFile(UUID.randomUUID().toString(), ".jar");
            tmpFile.deleteOnExit();
            os = new FileOutputStream(tmpFile);
            is = this.jarStorage.downloadJar(jarName);
            ByteStreams.copy(is, os);
            Parser parser = parserProxyUtil.loadClassFromJar(tmpFile.getAbsolutePath(), className);
            result = parser.schema();
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                LOG.error("Got exception", ex);
            }
        }
        return result;
    }

    //Test curl command curl -X POST -i -F parserJar=@original-webservice-0.1.0-SNAPSHOT.jar -F parserInfo='{"parserName":"TestParser","className":"some.test.parserClass","version":0}' http://localhost:8080/api/v1/catalog/parsers
    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/parsers")
    public Response addParser(@FormDataParam("parserJar") final InputStream inputStream, @FormDataParam("parserJar") final FormDataContentDisposition contentDispositionHeader,
                              @FormDataParam("parserInfo") final String parserInfoStr, @FormDataParam("schemaFromParserJar") boolean schemaFromParserJar) {

        LOG.debug("schemaFromParser {}", schemaFromParserJar);

        File file = null;
        try {
            String name = "";
            if (contentDispositionHeader != null && contentDispositionHeader.getFileName() != null) {
                name = contentDispositionHeader.getFileName();
                this.jarStorage.uploadJar(inputStream, name);
                inputStream.close();
            }


            //TODO something special about multipart request so it wont let me pass just a ParserInfo json object, instead we must pass ParserInfo as a json string.
            ParserInfo parserInfo = objectMapper.readValue(parserInfoStr, ParserInfo.class);
            parserInfo.setJarStoragePath(name);

            // if schema is not set in json, try to load it from the jar just uploaded.
            if (parserInfo.getParserSchema() == null && schemaFromParserJar) {
                parserInfo.setParserSchema(loadSchemaFromParserJar(name, parserInfo.getClassName()));
            }

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
            if (parserInfo != null) {
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
