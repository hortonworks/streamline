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

package org.apache.streamline.streams.service;

/**
 * Created by aiyer on 10/4/15.
 */

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.notification.Notification;
import org.apache.streamline.streams.notification.service.NotificationService;
import org.apache.streamline.streams.service.exception.request.EntityNotFoundException;
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

import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST endpoint for notifications
 */
@Path("/v1/notification")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationsResource {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationsResource.class);

    private final NotificationService notificationService;

    public NotificationsResource(NotificationService service) {
        this.notificationService = service;
    }

    @GET
    @Path("/notifications/{id}")
    @Timed
    public Response getNotificationById(@PathParam("id") String id) {
        Notification result = notificationService.getNotification(id);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(id);
    }

    @GET
    @Path("/notifications/")
    @Timed
    public Response listNotifications(@Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        MultivaluedMap<String, String> uriInfoParams = uriInfo.getQueryParameters();
        Collection<Notification> notifications = null;
        if (!uriInfoParams.isEmpty()) {
            queryParams = WSUtils.buildQueryParameters(uriInfoParams);
        } else {
            LOG.info("Query params empty, will use default criteria to return notifications.");
        }
        notifications = notificationService.findNotifications(queryParams);
        if (notifications != null && !notifications.isEmpty()) {
            return WSUtils.respondEntities(notifications, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @PUT
    @Path("/notifications/{id}/{status}")
    @Timed
    public Response updateNotificationStatus(@PathParam("id") String notificationId,
                                             @PathParam("status") Notification.Status status) {
        Notification updateNotification = notificationService.updateNotificationStatus(notificationId, status);
        return WSUtils.respondEntity(updateNotification, OK);
    }

    @GET
    @Path("/events/{id}")
    @Timed
    public Response getEventById(@PathParam("id") String id) {
        StreamlineEvent result = notificationService.getEvent(id);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(id);
    }

}
