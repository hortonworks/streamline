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
package com.hortonworks.iotas.tag.service;


import com.google.common.collect.ImmutableList;
import com.hortonworks.iotas.common.test.IntegrationTest;
import com.hortonworks.iotas.registries.tag.Tag;
import com.hortonworks.iotas.registries.tag.TaggedEntity;
import com.hortonworks.iotas.registries.tag.client.TagClient;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Category(IntegrationTest.class)
public class TagRestServiceTest {

    @ClassRule
    public static final DropwizardAppRule<TestConfiguration> RULE = new DropwizardAppRule<>(TestApplication.class, ResourceHelpers.resourceFilePath("tag-test.yaml"));

    private String catalogRootUrl = String.format("http://localhost:%d/api/v1/catalog", RULE.getLocalPort());

    @Test
    public void testTagResource() throws Exception {
        TagClient tagClient = new TagClient(catalogRootUrl);

        long parentTagId = 10L;
        long childTagId = 11L;

        //create a "parent-tag"
        Tag parent = createTag(parentTagId, "parent-tag");
        Assert.assertTrue(tagClient.addTag(parent).getId() == parentTagId);

        //create a "child-tag" which is tagged under "parent-tag"
        Tag child = createTag(childTagId, "child-tag", ImmutableList.<Tag>of(parent));
        Assert.assertTrue(tagClient.addTag(child).getId() == childTagId);

        //update parent-tag
        parent = createTag(parentTagId, "parent-update-tag");
        Assert.assertTrue(tagClient.addOrUpdateTag(parent).getId() == parentTagId);

        //get a Tag by Id
        Tag tag= tagClient.getTag(parentTagId);
        Assert.assertTrue("Tag Id is different" , tag.getId() == parentTagId);

        //get a unkonwn tag by Id
        tag= tagClient.getTag(100L);

        //add another tag
        Tag testTag = createTag(12L, "to-delete-tag");
        Assert.assertTrue(tagClient.addTag(testTag).getId() ==  12L);

        //list all tags
        List<Tag> allTags = tagClient.listTags();
        Assert.assertTrue("tag count mismatch", allTags.size() == 3);

        //list tags with queryParams
        Map<String, Object>  queryParams = new HashMap<>();
        queryParams.put("name", "child-tag");
        queryParams.put("description", "child-tag");
        allTags = tagClient.listTags(queryParams);
        Assert.assertTrue("tag count mismatch", allTags.size() == 1);

        //delete a tag
        tagClient.removeTag(12L);
        allTags = tagClient.listTags();
        Assert.assertTrue("count mismatch", allTags.size() == 2);

        //add Tag for Entity
        tagClient.addTagForEntity(new TaggedEntity("Device", 1L), parentTagId);
        tagClient.addTagForEntity(new TaggedEntity("Device", 2L), parentTagId);
        tagClient.addTagForEntity(new TaggedEntity("Device", 3L), parentTagId);

        //get All Entities For Tag
        List<TaggedEntity>  allEntities = tagClient.getTaggedEntities(parentTagId);
        Assert.assertTrue("entity count mismatch", allEntities.size() == 3);

        //remove Tag for Entity
        tagClient.removeTagForEntity(new TaggedEntity("Device", 1L), parentTagId);
        allEntities = tagClient.getTaggedEntities(parentTagId);
        Assert.assertTrue("entity count mismatch", allEntities.size() == 2);

        //add new Tag to existing entity
        Tag newTag = createTag(13L, "new-tag");
        Assert.assertTrue(tagClient.addTag(newTag).getId() == 13L);
        tagClient.addTagForEntity(new TaggedEntity("Device", 2L), 13L);

        //get All Tags For a given Entity
        allTags = tagClient.getTags(new TaggedEntity("Device", 2L));
        Assert.assertTrue("tag count mismatch", allTags.size() == 2);

        //try adding unknown tag for a Entity
        try {
            tagClient.addTagForEntity(new TaggedEntity("Device", 1L), 100L);
             Assert.fail("should have thrown error");
        }
        catch (RuntimeException e) {
        }

        //try removing unknown tag for a Entity
        try {
            tagClient.removeTagForEntity(new TaggedEntity("Device", 1L), 100L);
            Assert.fail("should have thrown error");
        }
        catch (RuntimeException e) {
        }
    }

    private Tag createTag(Long id, String name) {
        return createTag(id, name, Collections.<Tag>emptyList());
    }

    private Tag createTag(Long id, String name, List<Tag> parentTags) {
        Tag newTag = new Tag();
        newTag.setId(id);
        newTag.setName(name);
        newTag.setDescription(name);
        newTag.setTags(parentTags);
        return newTag;
    }
}