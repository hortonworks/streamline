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
    private TagDto() {
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
}
