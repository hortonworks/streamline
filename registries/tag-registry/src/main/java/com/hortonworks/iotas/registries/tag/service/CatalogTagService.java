package com.hortonworks.iotas.registries.tag.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.registries.tag.TaggedEntity;
import com.hortonworks.iotas.registries.tag.Tag;
import com.hortonworks.iotas.registries.tag.TagStorableMapping;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import org.apache.commons.io.IOUtils;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Catalog db based tag service.
 */
public class CatalogTagService implements TagService {
    private static final String TAG_NAMESPACE = new Tag().getNameSpace();
    private static final String TAG_STORABLE_MAPPING_NAMESPACE = new TagStorableMapping().getNameSpace();
    private final StorageManager dao;

    public CatalogTagService(StorageManager dao) {
        this.dao = dao;
        dao.registerStorables(getStorableClasses());

    }

    public static Collection<Class<? extends Storable>> getStorableClasses() {
        InputStream resourceAsStream = CatalogTagService.class.getClassLoader().getResourceAsStream("tagstorables.props");
        HashSet<Class<? extends Storable>> classes = new HashSet<>();
        try {
            List<String> classNames = IOUtils.readLines(resourceAsStream);
            for (String className : classNames) {
                classes.add((Class<? extends Storable>) Class.forName(className));
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return classes;
    }

    @Override
    public Tag addTag(Tag tag) {
        if (tag.getId() == null) {
            tag.setId(dao.nextId(TAG_NAMESPACE));
        }
        if (tag.getTimestamp() == null) {
            tag.setTimestamp(System.currentTimeMillis());
        }
        checkCycles(tag, tag.getTags());
        dao.add(tag);
        addTagsForStorable(getEntityId(tag), tag.getTags());
        return tag;
    }

    private void checkCycles(Tag current, List<Tag> tags) {
        for (Tag tag : tags) {
            if (tag.equals(current) || flatten(getTags(getEntityId(tag))).contains(current)) {
                throw new IllegalArgumentException("Tagging " + current +
                        " with " + tag + " would result in a cycle.");
            }
        }
    }


    private TaggedEntity getEntityId(Tag tag) {
        return new TaggedEntity(tag.getNameSpace(), tag.getId());
    }

    private List<Tag> flatten(Tag tag) {
        return flatten(Collections.singletonList(tag));
    }

    private List<Tag> flatten(List<Tag> tags) {
        List<Tag> res = new ArrayList<>();
        for (Tag tag: tags) {
            res.add(tag);
            res.addAll(flatten(tag.getTags()));
        }
        return res;
    }

    @Override
    public Tag addOrUpdateTag(Long tagId, Tag tag) {
        if (tag.getId() == null) {
            tag.setId(tagId);
        }
        if (tag.getTimestamp() == null) {
            tag.setTimestamp(System.currentTimeMillis());
        }
        List<Tag> existingTags = getTags(getEntityId(tag));
        List<Tag> tagsToBeAdded = getTagsToBeAdded(existingTags, tag.getTags());
        List<Tag> tagsToBeRemoved = getTagsToBeRemoved(existingTags, tag.getTags());
        checkCycles(tag, tagsToBeAdded);
        this.dao.addOrUpdate(tag);
        updateTags(getEntityId(tag), tagsToBeAdded, tagsToBeRemoved);
        return tag;
    }

    @Override
    public Tag getTag(Long tagId) {
        Tag tag = new Tag();
        tag.setId(tagId);
        Tag result = this.dao.<Tag>get(new StorableKey(TAG_NAMESPACE, tag.getPrimaryKey()));
        if (result != null) {
            result.setTags(getTags(getEntityId(result)));
        }
        return result;
    }

    @Override
    public Tag removeTag(Long tagId) {
        Tag tag = getTag(tagId);
        if (tag != null) {
            if (!getEntities(tagId, false).isEmpty()) {
                throw new TagNotEmptyException("Tag not empty, has child entities.");
            }
            removeTagsFromStorable(getEntityId(tag), tag.getTags());
            dao.<Tag>remove(new StorableKey(TAG_NAMESPACE, tag.getPrimaryKey()));
        }
        return tag;
    }

    @Override
    public Collection<Tag> listTags() {
        return makeTags(this.dao.<Tag>list(TAG_NAMESPACE));
    }

    @Override
    public Collection<Tag> listTags(List<QueryParam> queryParams) {
        return makeTags(dao.<Tag>find(TAG_NAMESPACE, queryParams));
    }

    @Override
    public void addTagsForStorable(TaggedEntity entityId, List<Tag> tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                TagStorableMapping tagStorable = new TagStorableMapping();
                tagStorable.setTagId(tag.getId());
                tagStorable.setStorableNamespace(entityId.getNamespace());
                tagStorable.setStorableId(entityId.getId());
                this.dao.add(tagStorable);
            }
        }
    }

    @Override
    public void addOrUpdateTagsForStorable(TaggedEntity entityId, List<Tag> tags) {
        List<Tag> existingTags = getTags(entityId);
        updateTags(entityId, getTagsToBeAdded(existingTags, tags), getTagsToBeRemoved(existingTags, tags));
    }

    private List<Tag> getTagsToBeRemoved(List<Tag> existing, List<Tag> newList) {
        return Lists.newArrayList(
                Sets.difference(ImmutableSet.copyOf(existing), ImmutableSet.copyOf(newList)));
    }

    private List<Tag> getTagsToBeAdded(List<Tag> existing, List<Tag> newList) {
        return Lists.newArrayList(
                Sets.difference(ImmutableSet.copyOf(newList), ImmutableSet.copyOf(existing)));
    }

    private void updateTags(TaggedEntity entityId, List<Tag> tagsToBeAdded, List<Tag> tagsToBeRemoved) {
        removeTagsFromStorable(entityId, tagsToBeRemoved);
        addTagsForStorable(entityId, tagsToBeAdded);
    }

    @Override
    public void removeTagsFromStorable(TaggedEntity entityId, List<Tag> tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                TagStorableMapping tagStorable = new TagStorableMapping();
                tagStorable.setTagId(tag.getId());
                tagStorable.setStorableId(entityId.getId());
                tagStorable.setStorableNamespace(entityId.getNamespace());
                this.dao.remove(tagStorable.getStorableKey());
            }
        }
    }

    @Override
    public List<Tag> getTags(TaggedEntity entityId) {
        List<Tag> tags = new ArrayList<>();
        QueryParam qp1 = new QueryParam(TagStorableMapping.FIELD_STORABLE_ID,
                                        String.valueOf(entityId.getId()));
        QueryParam qp2 = new QueryParam(TagStorableMapping.FIELD_STORABLE_NAMESPACE,
                                        String.valueOf(entityId.getNamespace()));
        for (TagStorableMapping mapping : listTagStorableMapping(ImmutableList.of(qp1, qp2))) {
            tags.add(getTag(mapping.getTagId()));
        }
        return tags;
    }

    enum State {
        VISITING, VISITED
    }

    @Override
    public List<TaggedEntity> getEntities(Long tagId, boolean recurse) {
        return getEntities(tagId, recurse, new HashMap<Long, State>());
    }

    public List<TaggedEntity> getEntities(Long tagId, boolean recurse, Map<Long, State> state) {
        State tagState = state.get(tagId);
        Set<TaggedEntity> result = new HashSet<>();
        if (tagState == State.VISITING) {
            throw new IllegalStateException("Cycle detected");
        } else if (tagState != State.VISITED) {
            state.put(tagId, State.VISITING);
            for (TaggedEntity entityId : getTagStorable(tagId)) {
                if (recurse && Tag.NAMESPACE.equalsIgnoreCase(entityId.getNamespace())) {
                    result.addAll(getEntities(entityId.getId(), recurse, state));
                } else {
                    result.add(entityId);
                }
            }
            state.put(tagId, State.VISITED);
        }
        return new LinkedList<>(result);
    }

    private List<TaggedEntity> getTagStorable(Long tagId) {
        List<TaggedEntity> storables = new ArrayList<>();
        QueryParam qp1 = new QueryParam(TagStorableMapping.FIELD_TAG_ID, String.valueOf(tagId));
        for (TagStorableMapping mapping : listTagStorableMapping(ImmutableList.of(qp1))) {
            storables.add(new TaggedEntity(mapping.getStorableNamespace(), mapping.getStorableId()));
        }
        return storables;
    }

    private Collection<TagStorableMapping> listTagStorableMapping(List<QueryParam> params) {
        return dao.<TagStorableMapping>find(TAG_STORABLE_MAPPING_NAMESPACE, params);
    }

    private Collection<Tag> makeTags(Collection<Tag> tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                tag.setTags(getTags(getEntityId(tag)));
            }
        }
        return tags;
    }
}
