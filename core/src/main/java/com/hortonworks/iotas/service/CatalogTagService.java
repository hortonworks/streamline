package com.hortonworks.iotas.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.iotas.catalog.Tag;
import com.hortonworks.iotas.catalog.TagStorableMapping;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;

import static com.hortonworks.iotas.service.CatalogService.QueryParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Catalog db based tag service.
 */
public class CatalogTagService implements TagService {
    private static final String TAG_NAMESPACE = new Tag().getNameSpace();
    private static final String TAG_STORABLE_MAPPING_NAMESPACE = new TagStorableMapping().getNameSpace();
    private final StorageManager dao;

    public CatalogTagService(StorageManager dao) {
        this.dao = dao;
    }

    @Override
    public Tag addTag(Tag tag) {
        if (tag.getId() == null) {
            tag.setId(dao.nextId(TAG_NAMESPACE));
        }
        if (tag.getTimestamp() == null) {
            tag.setTimestamp(System.currentTimeMillis());
        }
        dao.add(tag);
        addTagsForStorable(tag, tag.getTags());
        return tag;
    }

    @Override
    public Tag addOrUpdateTag(Long tagId, Tag tag) {
        tag.setId(tagId);
        tag.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(tag);
        addOrUpdateTagsForStorable(tag, tag.getTags());
        return tag;
    }

    @Override
    public Tag getTag(Long tagId) {
        Tag tag = new Tag();
        tag.setId(tagId);
        Tag result = this.dao.<Tag>get(new StorableKey(TAG_NAMESPACE, tag.getPrimaryKey()));
        if (result != null) {
            result.setTags(getTags(result));
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
            removeTagsFromStorable(tag, tag.getTags());
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
    public void addTagsForStorable(Storable storable, List<Tag> tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                TagStorableMapping tagStorable = new TagStorableMapping();
                tagStorable.setTagId(tag.getId());
                tagStorable.setStorableNamespace(storable.getNameSpace());
                tagStorable.setStorableId(storable.getId());
                this.dao.add(tagStorable);
            }
        }
    }

    @Override
    public void addOrUpdateTagsForStorable(Storable storable, List<Tag> tags) {
        List<Tag> existingTags = getTags(storable);
        Sets.SetView<Tag> tagsToBeRemoved = Sets.difference(ImmutableSet.copyOf(existingTags), ImmutableSet.copyOf(tags));
        Sets.SetView<Tag> tagsToBeAdded = Sets.difference(ImmutableSet.copyOf(tags), ImmutableSet.copyOf(existingTags));
        removeTagsFromStorable(storable, Lists.newArrayList(tagsToBeRemoved));
        addTagsForStorable(storable, Lists.newArrayList(tagsToBeAdded));
    }

    @Override
    public void removeTagsFromStorable(Storable storable, List<Tag> tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                TagStorableMapping tagStorable = new TagStorableMapping();
                tagStorable.setTagId(tag.getId());
                tagStorable.setStorableId(storable.getId());
                tagStorable.setStorableNamespace(storable.getNameSpace());
                this.dao.remove(tagStorable.getStorableKey());
            }
        }
    }

    @Override
    public List<Tag> getTags(Storable storable) {
        List<Tag> tags = new ArrayList<>();
        QueryParam qp1 = new QueryParam(TagStorableMapping.FIELD_STORABLE_ID,
                                        String.valueOf(storable.getId()));
        QueryParam qp2 = new QueryParam(TagStorableMapping.FIELD_STORABLE_NAMESPACE,
                                        String.valueOf(storable.getNameSpace()));
        for (TagStorableMapping mapping : listTagStorableMapping(ImmutableList.of(qp1, qp2))) {
            tags.add(getTag(mapping.getTagId()));
        }
        return tags;
    }

    @Override
    public List<Storable> getEntities(Long tagId, boolean recurse) {
        List<Storable> result = new ArrayList<>();
        for (Storable storable : getTagStorable(tagId)) {
            if (recurse && storable instanceof Tag) {
                result.addAll(getEntities(storable.getId(), recurse));
            } else {
                result.add(storable);
            }
        }
        return result;
    }

    private List<Storable> getTagStorable(Long tagId) {
        List<Storable> storables = new ArrayList<>();
        QueryParam qp1 = new QueryParam(TagStorableMapping.FIELD_TAG_ID, String.valueOf(tagId));
        for (TagStorableMapping mapping : listTagStorableMapping(ImmutableList.of(qp1))) {
            storables.addAll(dao.find(mapping.getStorableNamespace(),
                                      ImmutableList.of(new QueryParam("id", String.valueOf(mapping.getStorableId())))));
        }
        return storables;
    }

    private Collection<TagStorableMapping> listTagStorableMapping(List<QueryParam> params) {
        return dao.<TagStorableMapping>find(TAG_STORABLE_MAPPING_NAMESPACE, params);
    }

    private Collection<Tag> makeTags(Collection<Tag> tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                tag.setTags(getTags(tag));
            }
        }
        return tags;
    }
}
