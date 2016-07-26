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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TagRestClient {

    private Client client;
    private String catagoryRootUrl;
    private String tagRootUrl;

    public TagRestClient(String tagRootUrl) {
        this(tagRootUrl, new ClientConfig());
    }

    public TagRestClient(String catagoryRootUrl, ClientConfig clientConfig) {
        this.catagoryRootUrl = catagoryRootUrl;
        this.tagRootUrl = catagoryRootUrl + "/tags";
        client = ClientBuilder.newClient(clientConfig);
        client.register(MultiPartFeature.class);
    }

    public boolean addTag(Tag tag) {
        String response = client.target(tagRootUrl).request().post(Entity.json(new TagDto(tag)), String.class);
        if(CatalogResponse.ResponseMessage.SUCCESS.getCode() == getResponseCode(response))
            return true;
        else
            return false;
    }

    public boolean addOrUpdateTag(Tag tag) {
        String tagUpdateUrl = String.format("%s/%s", tagRootUrl, tag.getId());
        String response = client.target(tagUpdateUrl).request().put(Entity.json(new TagDto(tag)), String.class);
        if(CatalogResponse.ResponseMessage.SUCCESS.getCode() == getResponseCode(response))
            return true;
        else
            return false;
    }

    public Tag getTag(Long tagId) {
        return makeTag(getEntity(client.target(String.format("%s/%s/", tagRootUrl, tagId)), TagDto.class));
    }

    public boolean removeTag(Long tagId) {
        String tagRemoveUrl = String.format("%s/%s/", tagRootUrl, tagId);
        String response = client.target(tagRemoveUrl).request().delete(String.class);
        if(CatalogResponse.ResponseMessage.SUCCESS.getCode() == getResponseCode(response))
            return true;
        else
            return false;
    }

    public List<Tag> listTags() {
        return makeTags(getEntities(client.target(String.format("%s", tagRootUrl)), TagDto.class));
    }

    public List<Tag> listTags(Map<String, Object> queryParams) {
        if(queryParams.size() == 0)
            listTags();
        String queryString = getQueryString(queryParams);
        return makeTags(getEntities(client.target(String.format("%s?%s", tagRootUrl, queryString)), TagDto.class));
    }

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

    public void addTagForEntity(TaggedEntity taggedEntity, Long tagId) {
        String entityTagUrl = String.format("%s/%s/%s/%s/%s", tagRootUrl, tagId, "entities", taggedEntity.getNamespace(), taggedEntity.getId());
        client.target(entityTagUrl).request().post(Entity.json(taggedEntity), String.class);
    }

    public void addTagIdForEntity(TaggedEntity entityId, List<Long> tagId) {
        for (Long id: tagId) {
            addTagForEntity(entityId , id);
        }
    }

    public void addTagsForEntity(TaggedEntity entityId, List<Tag> tags) {
        for (Tag tag: tags) {
            addTagForEntity(entityId , tag.getId());
        }
    }

    public void removeTagIdForEntity(TaggedEntity taggedEntity, Long tagId) {
        String entityTagUrl = String.format("%s/%s/%s/%s/%s", tagRootUrl, tagId, "entities", taggedEntity.getNamespace(), taggedEntity.getId());
        client.target(entityTagUrl).request().delete(String.class);
    }

    public void removeTagIdForEntity(TaggedEntity entityId, List<Long> tagId) {
        for (Long id: tagId) {
            removeTagIdForEntity(entityId , id);
        }
    }

    public void removeTagForEntity(TaggedEntity entityId, Tag tag) {
            removeTagIdForEntity(entityId , tag.getId());
    }

    public void removeTagForEntity(TaggedEntity entityId, List<Tag> tags) {
        for (Tag tag: tags) {
            removeTagForEntity(entityId , tag);
        }
    }

    public void addOrUpdateTagsForEntity(TaggedEntity entityId, List<Tag> tags) {
        removeTagForEntity(entityId, tags);
        addTagsForEntity(entityId, tags);
    }


    private List<TagDto> getTagDto(TaggedEntity entityId) {
        return getEntities(client.target(String.format("%s/%s/%s/%s/%s", catagoryRootUrl, "taggedentities", entityId.getNamespace(), entityId.getId(), "tags")), TagDto.class);
    }

    public List<Tag> getTags(TaggedEntity entityId) {
        List<TagDto>  tagDtos = getTagDto(entityId);
        return makeTags(tagDtos);
    }

    public List<TaggedEntity> getEntities(Long tagId) {
        return getEntities(client.target(String.format("%s/%s/%s", tagRootUrl, tagId , "entities")), TaggedEntity.class);
    }

    private <T> List<T> getEntities(WebTarget target, Class<T> clazz) {
        List<T> entities = new ArrayList<T>();
        String response = target.request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            Iterator<JsonNode> it = node.get("entities").elements();
            while (it.hasNext()) {
                entities.add(mapper.treeToValue(it.next(), clazz));
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return entities;
    }

    private <T> T getEntity(WebTarget target, Class<T> clazz) {
        String response = target.request(MediaType.APPLICATION_JSON_TYPE).get(String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get("entity"), clazz);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get response code from the response string.
     *
     * @param response
     * @return
     * @throws Exception
     */
    public int getResponseCode(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get("responseCode"), Integer.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    private Tag makeTag(TagDto tagDto) {
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
