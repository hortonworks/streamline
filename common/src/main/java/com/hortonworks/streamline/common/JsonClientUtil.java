package org.apache.streamline.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.exception.WrappedWebApplicationException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonClientUtil {

    public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_JSON_TYPE;

    public static <T> T getEntity(WebTarget target, Class<T> clazz) {
        return getEntity(target, DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> T getEntity(WebTarget target, MediaType mediaType, Class<T> clazz) {
        try {
            String response = target.request(mediaType).get(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node, clazz);
        }  catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> T getEntity(WebTarget target, String fieldName, Class<T> clazz) {
        return getEntity(target, fieldName, DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> T getEntity(WebTarget target, String fieldName, MediaType mediaType, Class<T> clazz) {
        try {
            String response = target.request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get(fieldName), clazz);
        }  catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> List<T> getEntities(WebTarget target, Class<T> clazz) {
        return getEntities(target, DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> List<T> getEntities(WebTarget target, MediaType mediaType, Class<T> clazz) {
        List<T> entities = new ArrayList<>();
        try {
            String response = target.request(mediaType).get(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                entities.add(mapper.treeToValue(it.next(), clazz));
            }
            return entities;
        }  catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> List<T> getEntities(WebTarget target, String fieldName, Class<T> clazz) {
        return getEntities(target, fieldName, DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> List<T> getEntities(WebTarget target, String fieldName, MediaType mediaType, Class<T> clazz) {
        List<T> entities = new ArrayList<>();
        try {
            String response = target.request(mediaType).get(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            Iterator<JsonNode> it = node.get(fieldName).elements();
            while (it.hasNext()) {
                entities.add(mapper.treeToValue(it.next(), clazz));
            }
            return entities;
        }  catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> T postForm(WebTarget target, MultivaluedMap<String, String> form, Class<T> clazz) {
        return postForm(target, form, DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> T postForm(WebTarget target, MultivaluedMap<String, String> form, MediaType mediaType, Class<T> clazz) {
        return target.request(mediaType).post(Entity.form(form), clazz);
    }
}
