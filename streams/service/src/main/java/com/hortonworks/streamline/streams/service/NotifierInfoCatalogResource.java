/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.registries.common.util.FileStorage;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Notifier;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.common.exception.service.exception.request.UnsupportedMediaTypeException;
import com.hortonworks.streamline.streams.security.Permission;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
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
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static com.hortonworks.streamline.streams.security.Permission.EXECUTE;
import static com.hortonworks.streamline.streams.security.Permission.READ;
import static com.hortonworks.streamline.streams.security.Permission.DELETE;
import static com.hortonworks.streamline.streams.security.Permission.WRITE;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST endpoint for bundled or user uploaded notifiers
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class NotifierInfoCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(NotifierInfoCatalogResource.class);

    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final FileStorage fileStorage;


    public NotifierInfoCatalogResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService, FileStorage fileStorage) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.fileStorage = fileStorage;
    }

    /**
     * List ALL notifiers or the ones matching specific query params.
     */
    @GET
    @Path("/notifiers")
    @Timed
    public Response listNotifiers(@Context UriInfo uriInfo, @Context SecurityContext securityContext) {
        List<QueryParam> queryParams = new ArrayList<>();
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        Collection<Notifier> notifiers;
        if (params.isEmpty()) {
            notifiers = catalogService.listNotifierInfos();
        } else {
            queryParams = WSUtils.buildQueryParameters(params);
            notifiers = catalogService.listNotifierInfos(queryParams);
        }
        if (notifiers != null) {
            boolean notifierUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_NOTIFIER_USER);
            if (notifierUser) {
                LOG.debug("Returning all Notifiers since user has role: {}", Roles.ROLE_NOTIFIER_USER);
            } else {
                notifiers = SecurityUtil.filter(authorizer, securityContext, Notifier.NAMESPACE, notifiers, READ);
            }
            return WSUtils.respondEntities(notifiers, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/notifiers/{id}")
    @Timed
    public Response getNotifierById(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        boolean notifierUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_NOTIFIER_USER);
        if (notifierUser) {
            LOG.debug("Allowing get Notifier, since user has role: {}", Roles.ROLE_NOTIFIER_USER);
        } else {
            SecurityUtil.checkPermissions(authorizer, securityContext, Notifier.NAMESPACE, id, READ);
        }
        Notifier result = catalogService.getNotifierInfo(id);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(id.toString());
    }

    /**
     * Sample request :
     * <pre>
     * curl -X POST 'http://localhost:8080/api/v1/catalog/notifiers' -F notifierJarFile=/tmp/email-notifier.jar
     * -F notifierConfig='{
     *  "name":"email_notifier",
     *  "description": "testing",
     * "className":"com.hortonworks.streamline.streams.notifiers.EmailNotifier",
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
                                @FormDataParam("notifierConfig") final FormDataBodyPart notifierConfig,
                                @Context SecurityContext securityContext) throws IOException {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_NOTIFIER_ADMIN);
        MediaType mediaType = notifierConfig.getMediaType();
        LOG.debug("Media type {}", mediaType);
        if (!mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
            throw new UnsupportedMediaTypeException(mediaType.toString());
        }
        Notifier notifier = notifierConfig.getValueAs(Notifier.class);
        Collection<Notifier> existing = null;
        existing = catalogService.listNotifierInfos(Collections.singletonList(new QueryParam(Notifier.NOTIFIER_NAME, notifier.getName())));
        if (existing != null && !existing.isEmpty()) {
            LOG.warn("Received a post request for an already registered notifier. Not creating entity for " + notifier);
            return WSUtils.respondEntity(notifier, CONFLICT);
        }
        String jarFileName = uploadJar(inputStream, notifier.getName());
        notifier.setJarFileName(jarFileName);
        Notifier createdNotifier = catalogService.addNotifierInfo(notifier);
        SecurityUtil.addAcl(authorizer, securityContext, Notifier.NAMESPACE, createdNotifier.getId(),
                EnumSet.allOf(Permission.class));
        return WSUtils.respondEntity(createdNotifier, CREATED);
    }

    @DELETE
    @Path("/notifiers/{id}")
    @Timed
    public Response removeNotifierInfo(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        SecurityUtil.checkPermissions(authorizer, securityContext, Notifier.NAMESPACE, id, DELETE);
        Notifier removedNotifier = catalogService.removeNotifierInfo(id);
        if (removedNotifier != null) {
            SecurityUtil.removeAcl(authorizer, securityContext, Notifier.NAMESPACE, id);
            return WSUtils.respondEntity(removedNotifier, OK);
        }

        throw EntityNotFoundException.byId(id.toString());
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/notifiers/{id}")
    @Timed
    public Response addOrUpdateNotifierInfo(@PathParam("id") Long id,
                                            @FormDataParam("notifierJarFile") final InputStream inputStream,
                                            @FormDataParam("notifierJarFile") final FormDataContentDisposition contentDispositionHeader,
                                            @FormDataParam("notifierConfig") final FormDataBodyPart notifierConfig,
                                            @Context SecurityContext securityContext) throws IOException {
        SecurityUtil.checkPermissions(authorizer, securityContext, Notifier.NAMESPACE, id, WRITE);
        MediaType mediaType = notifierConfig.getMediaType();
        LOG.debug("Media type {}", mediaType);
        if (!mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
            throw new UnsupportedMediaTypeException(mediaType.toString());
        }
        Notifier notifier = notifierConfig.getValueAs(Notifier.class);
        String jarFileName = uploadJar(inputStream, notifier.getName());
        notifier.setJarFileName(jarFileName);
        Notifier newNotifier = catalogService.addOrUpdateNotifierInfo(id, notifier);
        return WSUtils.respondEntity(newNotifier, CREATED);
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
    public Response downloadNotifier(@PathParam("notifierId") Long notifierId, @Context SecurityContext securityContext) throws IOException {
        SecurityUtil.checkPermissions(authorizer, securityContext, Notifier.NAMESPACE, notifierId, READ, EXECUTE);
        Notifier notifier = catalogService.getNotifierInfo(notifierId);
        if (notifier != null) {
            StreamingOutput streamOutput = WSUtils.wrapWithStreamingOutput(
                    catalogService.downloadFileFromStorage(notifier.getJarFileName()));
            return Response.ok(streamOutput).build();
        }

        throw EntityNotFoundException.byId(notifierId.toString());
    }

    private String uploadJar(InputStream is, String notifierName) throws IOException {
        String jarFileName;
        if (is != null) {
            jarFileName = String.format("notifiers-%s.jar", UUID.randomUUID().toString());
            String uploadedPath = this.fileStorage.upload(is, jarFileName);
            LOG.debug("Jar uploaded to {}", uploadedPath);
        } else {
            String message = String.format("Notifier %s jar content is missing.", notifierName);
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
        return jarFileName;
    }
}
