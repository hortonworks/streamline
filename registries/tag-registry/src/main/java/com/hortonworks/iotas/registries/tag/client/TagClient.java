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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.registries.tag.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.iotas.common.catalog.CatalogResponse;
import com.hortonworks.iotas.registries.tag.TaggedEntity;
import com.hortonworks.iotas.registries.tag.Tag;
import com.hortonworks.iotas.registries.tag.dto.TagDto;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TagClient {

    private Client client;
    private String catalogRootUrl;
    private String tagRootUrl;

    public TagClient(String catalogRootUrl) {
        this(catalogRootUrl, new ClientConfig());
    }

    public TagClient(String catalogRootUrl, ClientConfig clientConfig) {
        this.catalogRootUrl = catalogRootUrl;
        this.tagRootUrl = catalogRootUrl + "/tags";
        client = ClientBuilder.newClient(clientConfig);
        client.register(MultiPartFeature.class);
    }
    /**
     * Add a given {@link Tag} into the system. This should
     * also handle nested tags.
     *
     * @param tag the tag to add
     * @return the added tag
     */
    public Tag addTag(Tag tag) {
        Response response = client.target(tagRootUrl).request().post(Entity.json(new TagDto(tag)));
        TagDto tagDto = getEntity(response, TagDto.class);
        return makeTag(tagDto);
    }

    /**
     * Adds a given {@link Tag}, or updates an existing tag in the system.
     *
     * @param tag   the tag
     * @return the added or updated tag
     */
    public Tag addOrUpdateTag(Tag tag) {
        String tagUpdateUrl = String.format("%s/%s", tagRootUrl, tag.getId());
        Response response = client.target(tagUpdateUrl).request().put(Entity.json(new TagDto(tag)));
        TagDto tagDto = getEntity(response, TagDto.class);
        return makeTag(tagDto);
    }

    /**
     * Removes the tag associated with the tag id from the system.
     * This could throw a {@link RuntimeException} if the tag being
     * removed has entities associated with it.
     *
     * @param tagId the tag id
     * @return the removed tag
     */
    public Tag removeTag(Long tagId) {
        String tagRemoveUrl = String.format("%s/%s/", tagRootUrl, tagId);
        Response response = client.target(tagRemoveUrl).request().delete();
        TagDto tagDto = getEntity(response, TagDto.class);
        return makeTag(tagDto);
    }


    /**
     * Returns the Tag associated with the given tag id.
     *
     * @param tagId the tag id
     * @return the tag
     */
    public Tag getTag(Long tagId) {
        Response response = client.target(String.format("%s/%s/", tagRootUrl, tagId)).request().get();
        return makeTag(getEntity(response, TagDto.class));
    }

    /**
     * List all the tags in the system.
     *
     * @return all the tags in the system.
     */
    public List<Tag> listTags() {
        return makeTags(getEntities(client.target(String.format("%s", tagRootUrl)), TagDto.class));
    }

    /**
     * Returns the list of tags matching the given query criteria.
     *
     * @param queryParams the query params
     * @return the tags matching the query params
     */
    public List<Tag> listTags(Map<String, Object> queryParams) {
        if(queryParams.size() == 0)
            listTags();
        String queryString = getQueryString(queryParams);
        return makeTags(getEntities(client.target(String.format("%s?%s", tagRootUrl, queryString)), TagDto.class));
    }

    /**
     *
     * @param queryParams
     * @return
     */
    private String getQueryString(Map<String, Object> queryParams) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : queryParams.entrySet())
        {
            if(sb.length() > 0){
                sb.append('&');
            }

            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Tags the entity with the given tag
     *
     * @param taggedEntity
     * @param tagId
     */
    public void addTagForEntity(TaggedEntity taggedEntity, Long tagId) {
        String entityTagUrl = String.format("%s/%s/%s/%s/%s", tagRootUrl, tagId, "entities", taggedEntity.getNamespace(), taggedEntity.getId());
        Response responseObject = client.target(entityTagUrl).request().post(Entity.json(taggedEntity));
        String response = responseObject.readEntity(String.class);
        int responseCode =  getResponseCode(response);
        if (responseCode != CatalogResponse.ResponseMessage.SUCCESS.getCode()) {
            throw new RuntimeException("Error occurred while tagging entity : "+ getResponseMessage(response));
        }
    }


    /**
     * Tags the entity with the given tags
     *
     * @param taggedEntity
     * @param tags
     */
    public void addTagsForEntity(TaggedEntity taggedEntity, List<Tag> tags) {
        for (Tag tag: tags) {
            addTagForEntity(taggedEntity , tag.getId());
        }
    }

    /**
     * Removes the given tag from the entity.
     *
     * @param taggedEntity
     * @param tagId
     */
    public void removeTagForEntity(TaggedEntity taggedEntity, Long tagId) {
        String entityTagUrl = String.format("%s/%s/%s/%s/%s", tagRootUrl, tagId, "entities", taggedEntity.getNamespace(), taggedEntity.getId());
        Response responseObject = client.target(entityTagUrl).request().delete();
        String response = responseObject.readEntity(String.class);
        int responseCode =  getResponseCode(response);
        if (responseCode != CatalogResponse.ResponseMessage.SUCCESS.getCode()) {
            throw new RuntimeException("Error occurred while removing tag for entity : "+ getResponseMessage(response));
        }
    }

    /**
     * Removes the given tag from the entity.
     *
     * @param taggedEntity
     * @param tag
     */
    public void removeTagForEntity(TaggedEntity taggedEntity, Tag tag) {
            removeTagForEntity(taggedEntity , tag.getId());
    }

    /**
     *
     * @param taggedEntity
     * @param tags
     */
    public void removeTagsForEntity(TaggedEntity taggedEntity, List<Tag> tags) {
        for (Tag tag: tags) {
            removeTagForEntity(taggedEntity , tag);
        }
    }

    /**
     * Updates the tags for the given entity with the new set of tags
     *
     * @param taggedEntity
     * @param tags
     */
    public void addOrUpdateTagsForEntity(TaggedEntity taggedEntity, List<Tag> tags) {
        List<Tag> existingTags = getTags(taggedEntity);
        updateTags(taggedEntity, getTagsToBeAdded(existingTags, tags), getTagsToBeRemoved(existingTags, tags));
    }

    private List<Tag> getTagsToBeRemoved(List<Tag> existing, List<Tag> newList) {
        return Lists.newArrayList(
                Sets.difference(ImmutableSet.copyOf(existing), ImmutableSet.copyOf(newList)));
    }

    private List<Tag> getTagsToBeAdded(List<Tag> existing, List<Tag> newList) {
        return Lists.newArrayList(
                Sets.difference(ImmutableSet.copyOf(newList), ImmutableSet.copyOf(existing)));
    }

    private void updateTags(TaggedEntity taggedEntity, List<Tag> tagsToBeAdded, List<Tag> tagsToBeRemoved) {
        removeTagsForEntity(taggedEntity, tagsToBeRemoved);
        addTagsForEntity(taggedEntity, tagsToBeAdded);
    }

    /**
     * Return the list of all the tags associated with the given entity.
     *
     * @param taggedEntity
     * @return
     */
    public List<Tag> getTags(TaggedEntity taggedEntity) {
        List<TagDto>  tagDtos = getTagDtos(taggedEntity);
        return makeTags(tagDtos);
    }

    private List<TagDto> getTagDtos(TaggedEntity taggedEntity) {
        return getEntities(client.target(String.format("%s/%s/%s/%s/%s", catalogRootUrl, "taggedentities", taggedEntity.getNamespace(), taggedEntity.getId(), "tags")), TagDto.class);
    }

    /**
     * Gets all the entities under the given tag id
     *
     * @param tagId   the tag id
     */
    public List<TaggedEntity> getTaggedEntities(Long tagId) {
        return getEntities(client.target(String.format("%s/%s/%s", tagRootUrl, tagId , "entities")), TaggedEntity.class);
    }

    private <T> List<T> getEntities(WebTarget target, Class<T> clazz) {
        List<T> entities = new ArrayList<T>();
        Response responseObject = target.request(MediaType.APPLICATION_JSON_TYPE).get();
        try {
            String response = responseObject.readEntity(String.class);
            int responseCode = getResponseCode(response);
            if (responseCode != CatalogResponse.ResponseMessage.SUCCESS.getCode()) {
                throw new RuntimeException("Error occurred :"+ getResponseMessage(response));
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            Iterator<JsonNode> it = node.get("entities").elements();
            while (it.hasNext()) {
                entities.add(mapper.treeToValue(it.next(), clazz));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return entities;
    }

    private <T> T getEntity(Response r, Class<T> clazz) {
        try {
            String response = r.readEntity(String.class);
            int responseCode =  getResponseCode(response);
            if (responseCode == CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND.getCode()) {
                return null;
            }
            else if(responseCode != CatalogResponse.ResponseMessage.SUCCESS.getCode()) {
                throw new RuntimeException("Error occurred "+ getResponseMessage(response));
            }
            else {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(response);
                return mapper.treeToValue(node.get("entity"), clazz);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private int getResponseCode(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get("responseCode"), Integer.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getResponseMessage(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get("responseMessage"), String.class);
        }  catch (Exception ex) {
            throw new RuntimeException(ex);

        }
    }


    private Tag makeTag(TagDto tagDto) {
        if (tagDto == null)
            return null;
        List<Tag> parentTags = new ArrayList<>();
        if (tagDto.getTagIds() != null) {
            for (Long tagId : tagDto.getTagIds()) {
                Tag tag = getTag(tagId);
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

    private List<Tag> makeTags(List<TagDto> tagDtos) {
        List<Tag> tags = new ArrayList<>();
        for (TagDto tagDto : tagDtos) {
            tags.add(makeTag(tagDto));
        }
        return tags;
    }
}
