package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class ProjectionTest {

    @Test
    public void testJsonSerializeProjection1() throws Exception {
        Projection projection = new Projection(null, "UPPER", Collections.singletonList("review_id"), "AA");
        ObjectMapper om = new ObjectMapper();
        String str = om.writeValueAsString(projection);
        Assert.assertEquals("{\"expr\":\"UPPER(review_id) AS AA\"}", str);
    }

    @Test
    public void testJsonSerializeProjection2() throws Exception {
        Projection projection = new Projection("UPPER(review_id) AS AA", null, null, null);
        ObjectMapper om = new ObjectMapper();
        String str = om.writeValueAsString(projection);
        Assert.assertEquals("{\"expr\":\"UPPER(review_id) AS AA\"}", str);
    }

    @Test
    public void testJsonSerializeProjection3() throws Exception {
        Projection projection = new Projection(null, "TRIM_FN", Collections.singletonList("user_id"), "BB");
        ObjectMapper om = new ObjectMapper();
        String str = om.writeValueAsString(projection);
        Assert.assertEquals("{\"expr\":\"TRIM(user_id) AS BB\"}", str);
    }

    @Test
    public void testFromToJson() throws Exception {
        String json = "{\"functionName\":\"TRIM_FN\",\"args\":[\"user_id\"],\"outputFieldName\":\"BB\"}";
        ObjectMapper om = new ObjectMapper();
        Projection projection = om.readValue(json, Projection.class);
        Assert.assertEquals("{\"expr\":\"TRIM(user_id) AS BB\"}", om.writeValueAsString(projection));
    }


    @Test
    public void testProjectionToString1() throws Exception {
        Projection projection = new Projection("UPPER(review_id) AS Camel", null, null, null);
        String str = projection.toString();
        Assert.assertEquals("UPPER(review_id) AS \"Camel\"", str);
    }

    @Test
    public void testProjectionToString2() throws Exception {
        Projection projection = new Projection("UPPER(review_id) AS \"Camel\"", null, null, null);
        String str = projection.toString();
        Assert.assertEquals("UPPER(review_id) AS \"Camel\"", str);
    }

    @Test
    public void testProjectionToString3() throws Exception {
        Projection projection = new Projection(null, "UPPER", Collections.singletonList("review_id"), "Camel");
        String str = projection.toString();
        Assert.assertEquals("UPPER(review_id) AS \"Camel\"", str);
    }
}