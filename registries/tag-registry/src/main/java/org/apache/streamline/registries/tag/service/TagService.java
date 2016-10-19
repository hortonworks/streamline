package org.apache.streamline.registries.tag.service;

import org.apache.streamline.common.QueryParam;
import org.apache.streamline.registries.tag.Tag;
import org.apache.streamline.registries.tag.TaggedEntity;

import java.util.Collection;
import java.util.List;

/**
 * Interface for managing tags in the system. A storable entity in
 * the system can be tagged with one or more tags and the tags can be nested.
 * The implementation for this could be based on catalog db,
 * or managed by external system like Atlas.
 */
public interface TagService {
    /**
     * Add a given {@link Tag} into the system. This should
     * also handle nested tags.
     *
     * @param tag the tag to add
     * @return the added tag
     */
    Tag addTag(Tag tag);

    /**
     * Adds a given {@link Tag}, or updates an existing tag in the system.
     *
     * @param tagId the unique id of the tag to be updated
     * @param tag   the tag
     * @return the added or updated tag
     */
    Tag addOrUpdateTag(Long tagId, Tag tag);

    /**
     * Returns the Tag associated with the given tag id.
     *
     * @param tagId the tag id
     * @return the tag
     */
    Tag getTag(Long tagId);

    /**
     * Removes the tag associated with the tag id from the system.
     * This could throw a {@link TagNotEmptyException} if the tag being
     * removed has entities associated with it.
     *
     * @param tagId the tag id
     * @return the removed tag
     */
    Tag removeTag(Long tagId);

    /**
     * List all the tags in the system.
     *
     * @return all the tags in the system.
     */
    Collection<Tag> listTags();

    /**
     * Returns the list of tags matching the given query criteria.
     *
     * @param queryParams the query params
     * @return the tags matching the query params
     */
    Collection<Tag> listTags(List<QueryParam> queryParams);

    /**
     * Tags the entity with the given tags
     *
     * @param entityId
     * @param tags
     */
    void addTagsForStorable(TaggedEntity entityId, List<Tag> tags);

    /**
     * Updates the tags for the given storable with the new set of tags
     * @param storableId
     * @param nameSpace
     * @param tags
     */

    /**
     * Updates the tags for the given entity with the new set of tags
     *
     * @param entityId
     * @param tags
     */
    void addOrUpdateTagsForStorable(TaggedEntity entityId, List<Tag> tags);

    /**
     *  Removes the given tags from the storable.
     *
     * @param storableId
     * @param nameSpace
     * @param tags
     */

    /**
     * Removes the given tags from the entity.
     *
     * @param entityId
     * @param tags
     */
    void removeTagsFromStorable(TaggedEntity entityId, List<Tag> tags);

    /**
     * Return the list of all the tags associated with the given entity.
     *
     * @param entityId
     * @return
     */
    List<Tag> getTags(TaggedEntity entityId);


    /**
     * Gets all the entities under the given tag id
     *
     * @param tagId   the tag id
     * @param recurse recursively traverse nested tags or not
     */
    List<TaggedEntity> getEntities(Long tagId, boolean recurse);
}
