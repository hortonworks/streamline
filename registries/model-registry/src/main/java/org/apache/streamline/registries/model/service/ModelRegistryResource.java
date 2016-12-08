package org.apache.streamline.registries.model.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.registries.model.data.ModelInfo;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import static javax.ws.rs.core.Response.Status.CREATED;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public final class ModelRegistryResource {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ModelRegistryResource.class);
    private final ModelRegistryService modelRegistryService;

    public ModelRegistryResource(ModelRegistryService modelRegistryService) {
        this.modelRegistryService = modelRegistryService;
    }

    @POST
    @Path("/models")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Timed
    public Response addModelInfo(@FormParam("modelInfo") final ModelInfo modelInfo, @FormParam("pmmlFile") final InputStream pmmlFile) {
        ModelInfo createdModelInfo = modelRegistryService.addModelInfo(modelInfo);
        modelRegistryService.uploadFile(pmmlFile, modelInfo.getPmmlFileName());
        return WSUtils.respondEntity(createdModelInfo, CREATED);
    }

    @GET
    @Path("/models/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    public Response getModelInfo(@PathParam("id") final Long modelId) {
        ModelInfo modelInfo = modelRegistryService.getModelInfo(modelId);
        return WSUtils.respondEntity(modelInfo, CREATED);
    }

    @GET
    @Produces({"application/octet-stream", "application/json"})
    @Path("/models/file/{modelId}")
    @Timed
    public Response downloadFile(@PathParam("modelId") Long modelId) {
        Response response = null;
        try {
            StreamingOutput streamOutput = WSUtils.wrapWithStreamingOutput(modelRegistryService.getModelFile(modelId));
            response = Response.ok(streamOutput).build();
            return response;
        } catch (FileNotFoundException e) {
            LOG.error("No file found for fileId [{}]", modelId, e);
        } catch (Exception ex) {
            LOG.error("Encountered error while downloading file [{}]", modelId, ex);
        }

        return response;
    }

}
