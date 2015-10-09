package com.hortonworks.iotas.webservice;

/**
 * Created by aiyer on 10/4/15.
 */

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.service.NotificationService;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.*;
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

    Map<String, Integer> knownParams = new HashMap<String, Integer>() {
        {
            put("datasourceid", 1);
            put("ruleid", 1);
            put("notifiername", 1);
            put("status", 2);
            put("startTs", 3);
            put("endTs", 3);
            put("numRows", 4);
        }
    };

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

    /**
     * Returns the slot (position) where a known param is
     * supposed to be present in a list of query params.
     */
    private int paramSlot(String param) {
        Integer val = knownParams.get(param.toLowerCase());
        if (val == null) {
            val = Integer.MAX_VALUE;
        }
        return val;
    }

    @GET
    @Path("/notifications/")
    @Timed
    public Response listNotifications(@Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            // put the query params in order.
            List<String> keys = new ArrayList<>();
            MultivaluedMap<String, String> uriInfoParams = uriInfo.getQueryParameters();
            keys.addAll(uriInfoParams.keySet());
            Collections.sort(keys, new Comparator<String>() {
                @Override
                public int compare(String key1, String key2) {
                    return paramSlot(key1) - paramSlot(key2);
                }
            });
            Collection<Notification> notifications = null;
            if (!keys.isEmpty()) {
                for (String key : keys) {
                    queryParams.add(new CatalogService.QueryParam(key, uriInfoParams.get(key).get(0)));
                }
            } else {
                LOG.info("Query params empty, will use default criteria to return latest notifications.");
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

}
