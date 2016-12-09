package org.apache.streamline.registries.tag.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.exception.DuplicateEntityException;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.registries.tag.Tag;
import org.apache.streamline.registries.tag.TagStorableMapping;
import org.apache.streamline.registries.tag.TaggedEntity;
import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.StorableKey;
import org.apache.streamline.storage.StorageManager;
import org.apache.commons.io.IOUtils;
import org.apache.streamline.storage.util.StorageUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    // handle this check at application layer since in-memory storage etc does not contain unique key constraint
    private void validateTag(Tag tag) {
        StorageUtils.ensureUnique(tag, this::listTags, QueryParam.params("name", tag.getName()));
    }

    @Override
    public Tag addTag(Tag tag) {
        if (tag.getId() == null) {
            tag.setId(dao.nextId(TAG_NAMESPACE));
        }
        if (tag.getTimestamp() == null) {
            tag.setTimestamp(System.currentTimeMillis());
        }
        validateTag(tag);
        checkCycles(tag, tag.getTags());
        dao.add(tag);
        addTagsForStorable(getTaggedEntity(tag), tag.getTags());
        return tag;
    }

    private void checkCycles(Tag current, List<Tag> tags) {
        for (Tag tag : tags) {
            if (tag.equals(current) || flatten(getTags(getTaggedEntity(tag))).contains(current)) {
                throw new IllegalArgumentException("Tagging " + current +
                        " with " + tag + " would result in a cycle.");
            }
        }
    }


    private TaggedEntity getTaggedEntity(Tag tag) {
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
        validateTag(tag);
        List<Tag> existingTags = getTags(getTaggedEntity(tag));
        List<Tag> tagsToBeAdded = getTagsToBeAdded(existingTags, tag.getTags());
        List<Tag> tagsToBeRemoved = getTagsToBeRemoved(existingTags, tag.getTags());
        checkCycles(tag, tagsToBeAdded);
        this.dao.addOrUpdate(tag);
        updateTags(getTaggedEntity(tag), tagsToBeAdded, tagsToBeRemoved);
        return tag;
    }

    @Override
    public Tag getTag(Long tagId) {
        Tag tag = new Tag();
        tag.setId(tagId);
        Tag result = this.dao.get(new StorableKey(TAG_NAMESPACE, tag.getPrimaryKey()));
        if (result != null) {
            result.setTags(getTags(getTaggedEntity(result)));
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
            removeTagsFromStorable(getTaggedEntity(tag), tag.getTags());
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
    public void addTagsForStorable(TaggedEntity taggedEntity, List<Tag> tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                TagStorableMapping tagStorable = new TagStorableMapping();
                tagStorable.setTagId(tag.getId());
                tagStorable.setStorableNamespace(taggedEntity.getNamespace());
                tagStorable.setStorableId(taggedEntity.getId());
                this.dao.add(tagStorable);
            }
        }
    }

    @Override
    public void addOrUpdateTagsForStorable(TaggedEntity taggedEntity, List<Tag> tags) {
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
        removeTagsFromStorable(taggedEntity, tagsToBeRemoved);
        addTagsForStorable(taggedEntity, tagsToBeAdded);
    }

    @Override
    public void removeTagsFromStorable(TaggedEntity taggedEntity, List<Tag> tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                TagStorableMapping tagStorable = new TagStorableMapping();
                tagStorable.setTagId(tag.getId());
                tagStorable.setStorableId(taggedEntity.getId());
                tagStorable.setStorableNamespace(taggedEntity.getNamespace());
                this.dao.remove(tagStorable.getStorableKey());
            }
        }
    }

    @Override
    public List<Tag> getTags(TaggedEntity taggedEntity) {
        List<Tag> tags = new ArrayList<>();
        QueryParam qp1 = new QueryParam(TagStorableMapping.FIELD_STORABLE_ID,
                                        String.valueOf(taggedEntity.getId()));
        QueryParam qp2 = new QueryParam(TagStorableMapping.FIELD_STORABLE_NAMESPACE,
                                        String.valueOf(taggedEntity.getNamespace()));
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
            for (TaggedEntity taggedEntity : getTaggedEntities(tagId)) {
                if (recurse && Tag.NAMESPACE.equalsIgnoreCase(taggedEntity.getNamespace())) {
                    result.addAll(getEntities(taggedEntity.getId(), recurse, state));
                } else {
                    result.add(taggedEntity);
                }
            }
            state.put(tagId, State.VISITED);
        }
        return new LinkedList<>(result);
    }

    private List<TaggedEntity> getTaggedEntities(Long tagId) {
        List<TaggedEntity> taggedEntities = new ArrayList<>();
        QueryParam qp1 = new QueryParam(TagStorableMapping.FIELD_TAG_ID, String.valueOf(tagId));
        for (TagStorableMapping mapping : listTagStorableMapping(ImmutableList.of(qp1))) {
            taggedEntities.add(new TaggedEntity(mapping.getStorableNamespace(), mapping.getStorableId()));
        }
        return taggedEntities;
    }

    private Collection<TagStorableMapping> listTagStorableMapping(List<QueryParam> params) {
        return dao.find(TAG_STORABLE_MAPPING_NAMESPACE, params);
    }

    private Collection<Tag> makeTags(Collection<Tag> tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                tag.setTags(getTags(getTaggedEntity(tag)));
            }
        }
        return tags;
    }
}
