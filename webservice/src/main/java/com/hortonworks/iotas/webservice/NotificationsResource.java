/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.webservice;

/**
 * Created by aiyer on 10/4/15.
 */

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.streams.notification.common.Notification;
import com.hortonworks.iotas.streams.notification.service.NotificationService;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST endpoint for notifications
 */
@Path("/api/v1/notification")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationsResource {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationsResource.class);

    private NotificationService notificationService;

    NotificationsResource(NotificationService service) {
        this.notificationService = service;
    }

    @GET
    @Path("/notifications/{id}")
    @Timed
    public Response getNotificationById(@PathParam("id") String id) {
        try {
            Notification result = notificationService.getNotification(id);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    @GET
    @Path("/notifications/")
    @Timed
    public Response listNotifications(@Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            MultivaluedMap<String, String> uriInfoParams = uriInfo.getQueryParameters();
            Collection<Notification> notifications = null;
            if (!uriInfoParams.isEmpty()) {
                queryParams = WSUtils.buildQueryParameters(uriInfoParams);
            } else {
                LOG.info("Query params empty, will use default criteria to return notifications.");
            }
            notifications = notificationService.findNotifications(queryParams);
            if (notifications != null && !notifications.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, notifications);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @PUT
    @Path("/notifications/{id}/{status}")
    @Timed
    public Response updateNotificationStatus(@PathParam("id") String notificationId,
                                             @PathParam("status") Notification.Status status) {
        try {
            Notification updateNotification = notificationService.updateNotificationStatus(notificationId, status);
            return WSUtils.respond(OK, SUCCESS, updateNotification);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/events/{id}")
    @Timed
    public Response getEventById(@PathParam("id") String id) {
        try {
            IotasEvent result = notificationService.getEvent(id);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

}
