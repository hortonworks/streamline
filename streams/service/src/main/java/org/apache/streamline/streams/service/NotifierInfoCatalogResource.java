package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.catalog.CatalogResponse;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.NotifierInfo;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST endpoint for bundled or user uploaded notifiers
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class NotifierInfoCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(NotifierInfoCatalogResource.class);

    private final StreamCatalogService catalogService;
    private final FileStorage fileStorage;


    public NotifierInfoCatalogResource(StreamCatalogService catalogService, FileStorage fileStorage) {
        this.catalogService = catalogService;
        this.fileStorage = fileStorage;
    }

    /**
     * List ALL notifiers or the ones matching specific query params.
     */
    @GET
    @Path("/notifiers")
    @Timed
    public Response listNotifiers(@Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<NotifierInfo> notifierInfos;
            if (params.isEmpty()) {
                notifierInfos = catalogService.listNotifierInfos();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                notifierInfos = catalogService.listNotifierInfos(queryParams);
            }
            if (notifierInfos != null) {
                return WSUtils.respond(notifierInfos, OK, SUCCESS);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/notifiers/{id}")
    @Timed
    public Response getNotifierById(@PathParam("id") Long id) {
        try {
            NotifierInfo result = catalogService.getNotifierInfo(id);
            if (result != null) {
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    /**
     * Sample request :
     * <pre>
     * curl -X POST 'http://localhost:8080/api/v1/catalog/notifiers' -F notifierJarFile=/tmp/email-notifier.jar
     * -F notifierConfig='{
     *  "name":"email_notifier",
     *  "description": "testing",
     * "className":"org.apache.streamline.streams.notifiers.EmailNotifier",
     *   "properties": {
     *     "username": "hwemailtest@gmail.com",
     *     "password": "testing12",
     *     "host": "smtp.gmail.com",
     *     "port": "587",
     *     "starttls": "true",
     *     "debug": "true"
     *     },
     *   "fieldValues": {
     *     "from": "hwemailtest@gmail.com",
     *     "to": "hwemailtest@gmail.com",
     *     "subject": "Testing email notifications",
     *     "contentType": "text/plain",
     *     "body": "default body"
     *     }
     * };type=application/json'
     * </pre>
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/notifiers")
    @Timed
    public Response addNotifier(@FormDataParam("notifierJarFile") final InputStream inputStream,
                                @FormDataParam("notifierJarFile") final FormDataContentDisposition contentDispositionHeader,
                                @FormDataParam("notifierConfig") final FormDataBodyPart notifierConfig) {
        try {
            LOG.debug("Media type {}", notifierConfig.getMediaType());
            if (!notifierConfig.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return WSUtils.respond(UNSUPPORTED_MEDIA_TYPE, CatalogResponse.ResponseMessage.UNSUPPORTED_MEDIA_TYPE);
            }
            NotifierInfo notifierInfo = notifierConfig.getValueAs(NotifierInfo.class);
            String jarFileName = uploadJar(inputStream, notifierInfo.getName());
            notifierInfo.setJarFileName(jarFileName);
            NotifierInfo createdNotifierInfo = catalogService.addNotifierInfo(notifierInfo);
            return WSUtils.respond(createdNotifierInfo, CREATED, SUCCESS);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/notifiers/{id}")
    @Timed
    public Response removeNotifierInfo(@PathParam("id") Long id) {
        try {
            NotifierInfo removedNotifierInfo = catalogService.removeNotifierInfo(id);
            if (removedNotifierInfo != null) {
                return WSUtils.respond(removedNotifierInfo, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/notifiers/{id}")
    @Timed
    public Response addOrUpdateNotifierInfo(@PathParam("id") Long id,
                                            @FormDataParam("notifierJarFile") final InputStream inputStream,
                                            @FormDataParam("notifierJarFile") final FormDataContentDisposition contentDispositionHeader,
                                            @FormDataParam("notifierConfig") final FormDataBodyPart notifierConfig) {
        try {
            LOG.debug("Media type {}", notifierConfig.getMediaType());
            if (!notifierConfig.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return WSUtils.respond(UNSUPPORTED_MEDIA_TYPE, CatalogResponse.ResponseMessage.UNSUPPORTED_MEDIA_TYPE);
            }
            NotifierInfo notifierInfo = notifierConfig.getValueAs(NotifierInfo.class);
            String jarFileName = uploadJar(inputStream, notifierInfo.getName());
            notifierInfo.setJarFileName(jarFileName);
            NotifierInfo newNotifierInfo = catalogService.addOrUpdateNotifierInfo(id, notifierInfo);
            return WSUtils.respond(newNotifierInfo, OK, SUCCESS);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * Download the jar corresponding to a specific Notifier.
     * <p>
     * E.g. curl http://localhost:8080/api/v1/catalog/notifiers/download/34 -o /tmp/notifier.jar
     * </p>
     */
    @Timed
    @GET
    @Produces({"application/java-archive", "application/json"})
    @Path("/notifiers/download/{notifierId}")
    public Response downloadUdf(@PathParam("notifierId") Long notifierId) {
        try {
            NotifierInfo notifierInfo = catalogService.getNotifierInfo(notifierId);
            if (notifierInfo != null) {
                StreamingOutput streamOutput = WSUtils.wrapWithStreamingOutput(
                        catalogService.downloadFileFromStorage(notifierInfo.getJarFileName()));
                return Response.ok(streamOutput).build();
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, notifierId.toString());
    }

    private String uploadJar(InputStream is, String notifierName) throws IOException {
        String jarFileName;
        if (is != null) {
            jarFileName = UUID.randomUUID().toString() + ".jar";
            String uploadedPath = this.fileStorage.uploadFile(is, jarFileName);
            LOG.debug("Jar uploaded to {}", uploadedPath);
        } else {
            String message = String.format("Notifier %s jar content is missing.", notifierName);
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
        return jarFileName;
    }


}
