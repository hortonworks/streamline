package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.Device;
import com.hortonworks.iotas.storage.StorageManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class DeviceCatalogResource {
    private StorageManager dao;
    // TODO should probably make namespace static
    private static final String DEVICE_NAMESPACE = new Device().getNameSpace();

    public DeviceCatalogResource(StorageManager manager) {
        this.dao = manager;
    }

    @GET
    @Path("/devices")
    @Timed
    // TODO add a way to query/filter and/or page results
    public Collection<Device> listParsers() {
        return this.dao.<Device>list(DEVICE_NAMESPACE);
    }

    //TODO: This isn't working because "deviceId" param is null, need to debug.
    @GET
    @Path("/devices/{id}/{version}")
    @Timed
    public Device getDeviceById(@PathParam("id") String deviceId, @PathParam("version") Long version) {
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setVersion(version);
        return this.dao.<Device>get(DEVICE_NAMESPACE, device.getPrimaryKey());
    }

    @POST
    @Path("/devices")
    @Timed
    public Device addDevice(Device device) {
        if (device.getTimestamp() == null) {
            device.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(device);
        return device;
    }

    @DELETE
    @Path("/devices/{id}/{version}")
    @Timed
    public Device removeParser(@PathParam("id") String deviceId,  @PathParam("version") Long version) {
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setVersion(version);
        return this.dao.remove(DEVICE_NAMESPACE, device.getPrimaryKey());
    }

    @PUT
    @Path("/devices")
    @Timed
    public Device addOrUpdateDevice(Device device) {
        if (device.getTimestamp() == null) {
            device.setTimestamp(System.currentTimeMillis());
        }
        this.dao.addOrUpdate(device);
        return device;
    }
}
