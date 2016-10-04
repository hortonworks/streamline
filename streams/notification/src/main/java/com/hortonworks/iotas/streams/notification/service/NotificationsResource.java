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

package com.hortonworks.iotas.streams.notification.service;

/**
 * Created by aiyer on 10/4/15.
 */

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.streams.IotasEvent;
import com.hortonworks.iotas.streams.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST endpoint for notifications
 */
@Path("/api/v1/notification")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationsResource {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationsResource.class);

    private NotificationService notificationService;

    public NotificationsResource(NotificationService service) {
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
        List<QueryParam> queryParams = new ArrayList<QueryParam>();
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
