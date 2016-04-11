package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.Tag;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.exception.StorageException;
import com.hortonworks.iotas.webservice.catalog.dto.DataSourceDto;
import com.hortonworks.iotas.webservice.catalog.dto.TagDto;
import com.hortonworks.iotas.webservice.util.WSUtils;

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
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static javax.ws.rs.core.Response.Status.*;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TagCatalogResource {
    private CatalogService catalogService;
    private DataSourceFacade dataSourceFacade;

    public TagCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
        this.dataSourceFacade = new DataSourceFacade(catalogService);
    }

    /**
     * List ALL tags or the ones matching specific query params.
     */
    @GET
    @Path("/tags")
    @Timed
    public Response listTags(@Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<Tag> tags;
            if (params.isEmpty()) {
                tags = catalogService.listTags();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                tags = catalogService.listTags(queryParams);
            }
            if (tags != null && !tags.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, makeTagDto(tags));
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/tags/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagById(@PathParam("id") Long tagId) {
        try {
            Tag result = catalogService.getTag(tagId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, makeTagDto(result));
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, tagId.toString());
    }

    @POST
    @Path("/tags")
    @Timed
    public Response addTag(TagDto tagDto) {
        try {
            Tag createdTag = catalogService.addTag(makeTag(tagDto));
            return WSUtils.respond(CREATED, SUCCESS, makeTagDto(createdTag));
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }


    @DELETE
    @Path("/tags/{id}")
    @Timed
    public Response removeTag(@PathParam("id") Long tagId) {
        try {
            Tag removedTag = catalogService.removeTag(tagId);
            if (removedTag != null) {
                return WSUtils.respond(OK, SUCCESS, makeTagDto(removedTag));
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, removedTag.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/tags/{id}")
    @Timed
    public Response addOrUpdateTag(@PathParam("id") Long tagId, TagDto tagDto) {
        try {
            Tag newTag = catalogService.addOrUpdateTag(tagId, makeTag(tagDto));
            return WSUtils.respond(OK, SUCCESS, makeTagDto(newTag));
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/tags/{id}/entities")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagEntities(@PathParam("id") Long tagId) {
        try {
            List<Storable> result = catalogService.getEntities(tagId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, makeDto(result));
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, tagId.toString());
    }

    private Collection<TagDto> makeTagDto(Collection<Tag> tags) {
        List<TagDto> tagDtos = new ArrayList<>();
        for (Tag tag : tags) {
            tagDtos.add(makeTagDto(tag));
        }
        return tagDtos;
    }

    private TagDto makeTagDto(Tag newTag) {
        return new TagDto(newTag);
    }

    private Tag makeTag(TagDto tagDto) {
        List<Tag> parentTags = new ArrayList<>();
        if (tagDto.getTagIds() != null) {
            for (Long tagId : tagDto.getTagIds()) {
                parentTags.add(catalogService.getTag(tagId));
            }
        }
        Tag tag = new Tag();
        tag.setId(tagDto.getId());
        tag.setName(tagDto.getName());
        tag.setDescription(tagDto.getDescription());
        tag.setTimestamp(tagDto.getTimestamp());
        tag.setTags(parentTags);
        return tag;
    }

    private DataSourceDto makeDataSourceDto(DataSource dataSource) throws Exception {
        return dataSourceFacade.getDataSource(dataSource.getId());
    }

    private Collection<Object> makeDto(Collection<Storable> storables) throws Exception {
        List<Object> result = new ArrayList<>();
        for(Storable storable: storables) {
            if(storable instanceof Tag) {
                result.add(makeTagDto((Tag) storable));
            } else if(storable instanceof DataSource) {
                result.add(makeDataSourceDto((DataSource) storable));
            } else {
                throw new StorageException("Storable " + storable + " does not have a corresponding dto");
            }
        }
        return result;
    }
}
