package com.hortonworks.iotas.service;

import com.hortonworks.iotas.catalog.Tag;
import com.hortonworks.iotas.storage.Storable;

import java.util.Collection;
import java.util.List;

/**
 * Interface for managing tags in the Iotas system. A storable entity in
 * Iotas can be tagged with one or more tags and the tags can be nested.
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
    Collection<Tag> listTags(List<CatalogService.QueryParam> queryParams);

    /**
     * Tags the storable with the given tags
     *
     * @param storable the storable that should be tagged
     * @param tags     the tags to be added
     */
    void addTagsForStorable(Storable storable, List<Tag> tags);

    /**
     * Updates the tags for the given storable with the new set of tags
     *
     * @param storable the storable
     * @param tags     the new set of tags
     */
    void addOrUpdateTagsForStorable(Storable storable, List<Tag> tags);

    /**
     * Removes the given tags from the storable.
     *
     * @param storable the storable
     * @param tags     the tags to be removed
     */
    void removeTagsFromStorable(Storable storable, List<Tag> tags);

    /**
     * Return the list of all the tags associated with the given storable.
     *
     * @param storable the storable entity
     * @return the list of tags associated with the storable.
     */
    List<Tag> getTags(Storable storable);

    /**
     * Gets all the entities under the given tag id
     *
     * @param tagId   the tag id
     * @param recurse recursively traverse nested tags or not
     */
    List<Storable> getEntities(Long tagId, boolean recurse);
}
