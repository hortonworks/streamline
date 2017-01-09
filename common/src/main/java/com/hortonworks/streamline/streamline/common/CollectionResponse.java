package com.hortonworks.streamline.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;

/**
 * A wrapper entity for passing collection (more than one resource) back to the client.
 * This response is used only for succeed requests.
 * <p/>
 * This can be expanded to handle paged result.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionResponse {
  /**
   * For response that returns a collection of entities.
   */
  private Collection<?> entities;

  private CollectionResponse() {}

  public void setEntities(Collection<?> entities) {
    this.entities = entities;
  }

  public Collection<?> getEntities() {
    return entities;
  }

  public static Builder newResponse() {
    return new Builder();
  }

  public static class Builder {
    private Collection<?> entities;

    private Builder() {
    }

    public CollectionResponse.Builder entities(Collection<?> entities) {
      this.entities = entities;
      return this;
    }

    public CollectionResponse build() {
      CollectionResponse response = new CollectionResponse();
      response.setEntities(entities);
      return response;
    }
  }
}
