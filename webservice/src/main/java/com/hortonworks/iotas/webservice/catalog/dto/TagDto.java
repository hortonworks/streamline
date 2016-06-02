package com.hortonworks.iotas.webservice.catalog.dto;

import com.hortonworks.iotas.catalog.Tag;

import java.util.ArrayList;
import java.util.List;

public class TagDto {
    private Long id;
    private String name;
    private String description = "";
    private Long timestamp;
    // parent tag ids
    private List<Long> tagIds;

    // for jackson
    public TagDto() {
    }

    public TagDto(Tag tag) {
        this.id = tag.getId();
        this.name = tag.getName();
        this.description = tag.getDescription();
        this.timestamp = tag.getTimestamp();
        this.tagIds = getTagIds(tag.getTags());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    /**
     * Set parent tag ids
     */
    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    private List<Long> getTagIds(List<Tag> tags) {
        List<Long> tagIds = new ArrayList<>();
        if (tags != null) {
            for (Tag tag : tags) {
                tagIds.add(tag.getId());
            }
        }
        return tagIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TagDto tagDto = (TagDto) o;

        if (id != null ? !id.equals(tagDto.id) : tagDto.id != null) return false;
        if (name != null ? !name.equals(tagDto.name) : tagDto.name != null) return false;
        if (description != null ? !description.equals(tagDto.description) : tagDto.description != null) return false;
        if (timestamp != null ? !timestamp.equals(tagDto.timestamp) : tagDto.timestamp != null) return false;
        return tagIds != null ? tagIds.equals(tagDto.tagIds) : tagDto.tagIds == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (tagIds != null ? tagIds.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TagDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                ", tagIds=" + tagIds +
                '}';
    }
}
