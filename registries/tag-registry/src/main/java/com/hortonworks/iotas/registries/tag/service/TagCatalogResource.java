package com.hortonworks.iotas.registries.tag.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.registries.tag.TaggedEntity;
import com.hortonworks.iotas.registries.tag.Tag;
import com.hortonworks.iotas.registries.tag.dto.TagDto;

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
import java.util.Collections;
import java.util.List;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.*;
import static javax.ws.rs.core.Response.Status.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * REST resource for managing hierarchical tags in the Iotas system.
 * <p>
 * <b>Note:</b> The JAVADOCS should be updated whenever there are any changes to the api.
 */
@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TagCatalogResource {
    private TagService tagService;

    public TagCatalogResource(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * <p>
     * Lists all the tags in the system or the ones matching specific query params. For example to
     * list all the tags in the system,
     * </p>
     * <b>GET /api/v1/catalog/tags</b>
     * <p>
     * <pre>
     * {
     *  "responseCode": 1000,
     *  "responseMessage": "Success",
     *  "entities": [
     *    {
     *      "id": 1,
     *      "name": "device",
     *      "description": "device tag",
     *      "timestamp": 1462865727579,
     *      "tagIds": []
     *    },
     *    {
     *      "id": 2,
     *      "name": "thermostat",
     *      "description": "thermostat device",
     *      "timestamp": 1462866539146,
     *      "tagIds": [1]
     *    }
     *    ..
     *    ..
     *  ]
     * }
     * </pre>
     * <p>
     * <p>
     * The tags can also be listed based on specific query params. For example to list
     * tag(s) with the name "device",
     * </p>
     * <b>GET /api/v1/catalog/tags?name=device</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entities": [{
     *     "id": 1,
     *     "name": "device",
     *     "description": "device tag",
     *     "timestamp": 1462865727579,
     *     "tagIds": []
     *   }]
     * }
     * </pre>
     *
     * @param uriInfo the URI info which contains the query params
     * @return the response
     */
    @GET
    @Path("/tags")
    @Timed
    public Response listTags(@Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<Tag> tags;
            if (params.isEmpty()) {
                tags = tagService.listTags();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                tags = tagService.listTags(queryParams);
            }
            if (tags != null)
                return WSUtils.respond(OK, SUCCESS, makeTagDto(tags));
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    /**
     * <p>
     * Gets a specific tag by Id. For example,
     * </p>
     * <b>GET /api/v1/catalog/tags/1</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "name": "device",
     *     "description": "device tag",
     *     "timestamp": 1462865727579,
     *     "tagIds": []
     *   }
     * }
     * </pre>
     *
     * @param tagId the tag id
     * @return the response
     */
    @GET
    @Path("/tags/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagById(@PathParam("id") Long tagId) {
        try {
            Tag result = tagService.getTag(tagId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, makeTagDto(result));
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, tagId.toString());
    }

    /**
     * <p>
     * Creates a tag in the system. For example,
     * </p>
     * <b>POST /api/v1/catalog/tags</b>
     * <pre>
     * {
     *   "name": "device",
     *   "description": "device tag"
     * }
     * </pre>
     * <i>Sample success response: </i>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *     "entity": {
     *       "id": 1,
     *       "name": "device",
     *       "description": "device tag",
     *       "timestamp": 1462865727579,
     *       "tagIds": []
     *     }
     * }
     * </pre>
     * <p>
     * Tags can be optionally nested. To create a nested tag, specify a parent tag id
     * while creating the tag. Parent tags can also be added later via the update api (PUT).
     * For example a thermostat tag can be added as a child of device tag as follows.
     * </p>
     * <p>
     * <b>POST /api/v1/catalog/tags</b>
     * <pre>
     * {
     *   "name": "thermostat",
     *   "description": "thermostat device",
     *   "tagIds": [1]
     * }
     * </pre>
     * <i>Response:</i>
     * <pre>
     * {
     *    "responseCode": 1000,
     *    "responseMessage": "Success",
     *    "entity": {
     *      "id": 2,
     *      "name": "thermostat",
     *      "description": "thermostat device",
     *      "timestamp": 1462866539146,
     *      "tagIds": [1]
     *    }
     * }
     * </pre>
     *
     * @param tagDto the tag object to be created
     * @return the response
     */
    @POST
    @Path("/tags")
    @Timed
    public Response addTag(TagDto tagDto) {
        try {
            Tag createdTag = tagService.addTag(makeTag(tagDto));
            return WSUtils.respond(CREATED, SUCCESS, makeTagDto(createdTag));
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }


    /**
     * <p>
     * Removes a tag from the system. Tags cannot be removed if it has any associated entities.
     * </p>
     * <b>DELETE /api/v1/catalog/tags/3</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 3,
     *     "name": "weather",
     *     "description": "weather related",
     *     "timestamp": 1462868853582,
     *     "tagIds": []
     *   }
     * }
     * </pre>
     * <p>
     * Trying to delete a tag that has some child entities would result in an error. A tag should be
     * first removed from all the entities (including child tags) before it can be deleted.
     * </p>
     * <b>DELETE /api/v1/catalog/tags/1</b>
     * <pre>
     * {
     * "responseCode": 1102,
     * "responseMessage": "An exception with message [Tag not empty, has child entities.]
     *   was thrown while processing request. Please check webservice/ErrorCodes.md
     *   for more details."
     * }</pre>
     *
     * @param tagId the id of the tag to be deleted
     * @return the response
     */
    @DELETE
    @Path("/tags/{id}")
    @Timed
    public Response removeTag(@PathParam("id") Long tagId) {
        try {
            Tag removedTag = tagService.removeTag(tagId);
            if (removedTag != null) {
                return WSUtils.respond(OK, SUCCESS, makeTagDto(removedTag));
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, removedTag.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>Updates a tag in the system.</p>
     * <p>
     * <b>PUT /api/v1/catalog/tags/1</b>
     * <pre>
     * {
     *   "name": "device",
     *   "description": "updated device tag"
     * }
     * </pre>
     * <i>Sample success response: </i>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *     "entity": {
     *       "id": 1,
     *       "name": "device",
     *       "description": "updated device tag",
     *       "timestamp": 1462870576965,
     *       "tagIds": []
     *     }
     * }
     * </pre>
     *
     * @param tagId  the id of the tag to be updated
     * @param tagDto the updated tag object
     * @return the response
     */
    @PUT
    @Path("/tags/{id}")
    @Timed
    public Response addOrUpdateTag(@PathParam("id") Long tagId, TagDto tagDto) {
        try {
            Tag newTag = tagService.addOrUpdateTag(tagId, makeTag(tagDto));
            return WSUtils.respond(OK, SUCCESS, makeTagDto(newTag));
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>
     * Tags the entity with the given tag. For example,
     * </p>
     * <b>POST /api/v1/catalog/tags/:TAG_ID/entities/:NAME_SPACE/:ENTITY_ID</b>
     * <i>Sample success response: </i>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     * }
     * </pre>
     *
     * @param tagId
     * @param namespace
     * @param entityId
     * @return
     */
    @POST
    @Path("/tags/{id}/entities/{namespace}/{entity-id}")
    @Timed
    public Response addTagForEntity(@PathParam("id") Long tagId, @PathParam("namespace") String namespace, @PathParam("entity-id") Long entityId) {
        try {
            Tag tag = tagService.getTag(tagId);
            if(tag != null) {
                tagService.addTagsForStorable(new TaggedEntity(namespace, entityId), Collections.singletonList(tag));
                return WSUtils.respond(CREATED, SUCCESS);
            }
            else {
                return WSUtils.respond(BAD_REQUEST, ENTITY_NOT_FOUND, tagId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>
     * Removes the given tag from the entity.
     * </p>
     * <b>DELETE /api/v1/catalog/tags/:TAG_ID/entities/:NAME_SPACE/:ENTITY_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     * }
     * </pre>
     * <p>
     *
     * @param tagId
     * @param namespace
     * @param entityId
     * @return
     */
    @DELETE
    @Path("/tags/{id}/entities/{namespace}/{entity-id}")
    @Timed
    public Response removeTagFromEntity(@PathParam("id") Long tagId, @PathParam("namespace") String namespace, @PathParam("entity-id") Long entityId) {
        try {
            Tag tag = tagService.getTag(tagId);
            if(tag !=null ) {
                tagService.removeTagsFromStorable(new TaggedEntity(namespace, entityId), Collections.singletonList(tag));
                return WSUtils.respond(CREATED, SUCCESS);
            }
            else {
                return WSUtils.respond(BAD_REQUEST, ENTITY_NOT_FOUND, tagId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>
     * Gets all the entities tagged with the given tag id. This also recursively gets
     * all the entities belonging to any child tags of the given tag. The child tags itself
     * are not included in the result.
     * </p>
     * <b>GET /api/v1/catalog/tags/1/entities</b>
     * <pre>
     * {
     *   "responseCode":1000,
     *   "responseMessage":"Success",
     *   "entities":[{
     *     "id":12,
     *     "namespace":"tag"
     *     }]
     * }
     * </pre>
     *
     * @param tagId the tag id
     * @return the response
     */
    @GET
    @Path("/tags/{id}/entities")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTaggedEntities(@PathParam("id") Long tagId) {
        try {
            List<TaggedEntity> result = tagService.getEntities(tagId, true);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, tagId.toString());
    }

    /**
     * <p>
     * Gets all associated tags for a given entity id.
     *
     * </p>
     * <b>GET /api/v1/catalog//taggedentities/{namespace}/{id}/tags</b>
     * <pre>
     * {
     *   "responseCode":1000,
     *   "responseMessage":"Success",
     *   "entities":[{
     *       "id": 1,
     *       "name": "device",
     *       "description": "updated device tag",
     *       "timestamp": 1462870576965,
     *       "tagIds": []
     *     }]
     * }
     * </pre>
     *
     * @return the response
     */
    @GET
    @Path("/taggedentities/{namespace}/{id}/tags")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagsForEntity(@PathParam("namespace") String namespace, @PathParam("id") Long entityId) {
        try {
            List<Tag> tags = tagService.getTags(new TaggedEntity(namespace, entityId));
            if (tags != null) {
                return WSUtils.respond(OK, SUCCESS, makeTagDto(tags));
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, entityId.toString());
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
                Tag tag = tagService.getTag(tagId);
                if (tag == null) {
                    throw new IllegalArgumentException("Tag with id " + tagId + " does not exist.");
                }
                parentTags.add(tag);
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

}
