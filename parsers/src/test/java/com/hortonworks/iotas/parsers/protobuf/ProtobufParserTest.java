package com.hortonworks.iotas.parsers.protobuf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufFactory;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by aiyer on 8/13/15.
 */
public class ProtobufParserTest {
    ObjectMapper mapper;

    private static class Point {
        public int x;
        public int y;
        public int color;

        public Point() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Point point = (Point) o;

            if (x != point.x) return false;
            if (y != point.y) return false;
            return color == point.color;

        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + color;
            return result;
        }

        public Point(int i, int j) {
            this.x = i;
            this.y = j;
        }

        @Override
        public String toString() {
            return "[x=" + x + ",y=" + y + ", color=" + color + "]";
        }
    }

    private static class Box {
        public Point topLeft, bottomRight;

        public Box() {
        }

        public Box(Point tl, Point br) {
            topLeft = tl;
            bottomRight = br;
        }

        public Box(int x1, int y1, int x2, int y2) {
            this(new Point(x1, y1), new Point(x2, y2));
        }

        @Override
        public String toString() {
            return "[topLeft=" + topLeft + ",bottomRight=" + bottomRight + "]";
        }
    }

    private String loadFile(String fileName) throws IOException {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        return IOUtils.toString(stream);
    }

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper(new ProtobufFactory());
    }

    @Test
    public void testParseSimple() throws Exception {
        // create protobuf bytes
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(loadFile("point.proto"), "Point");
        final ObjectWriter w = mapper.writerFor(Point.class).with(schema);
        Point point = new Point(10, 10);
        byte[] bytes = w.writeValueAsBytes(point);
        // System.out.println(bytes);
        // pass the protobuf bytes to parser and get back the object.
        ProtobufParser parser = ProtobufParser.newBuilder().protoBufSchemaString(loadFile("point.proto")).clazz(Point.class).buid();
        Map<String, Object> res = parser.parse(bytes);
        assertEquals(10, res.get("x"));
        assertEquals(10, res.get("y"));
        assertEquals(0, res.get("color"));
    }

    @Test
    public void testParseSimpleNoFieldLookup() throws Exception {
        // create protobuf bytes
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(loadFile("point.proto"), "Point");
        final ObjectWriter w = mapper.writerFor(Point.class).with(schema);
        Point point = new Point(10, 10);
        byte[] bytes = w.writeValueAsBytes(point);
        // System.out.println(bytes);
        // pass the protobuf bytes to parser and get back the object.
        ProtobufParser parser = ProtobufParser.newBuilder()
                                .protoBufSchemaString(loadFile("point.proto"))
                                .clazz(Point.class)
                                .noFieldLookup()
                                .buid();
        Map<String, Object> res = parser.parse(bytes);
        assertTrue(res.containsKey("value"));
        assertEquals(1, res.keySet().size());
    }

    @Test
    public void testParseNested() throws Exception {
        // create protobuf bytes
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(loadFile("box.proto") + loadFile("point.proto"), "Box");
        final ObjectWriter w = mapper.writerFor(Box.class).with(schema);
        Point tl = new Point(10, 10);
        Point br = new Point(100, 100);
        Box box = new Box(tl, br);
        byte[] bytes = w.writeValueAsBytes(box);
       // System.out.println(bytes);
        // pass the protobuf bytes to parser and get back the object.
        ProtobufParser parser = ProtobufParser.newBuilder()
                .protoBufSchemaString(loadFile("box.proto") + loadFile("point.proto")).clazz(Box.class).buid();
        Map<String, Object> res = parser.parse(bytes);
        assertEquals(tl, res.get("topLeft"));
        assertEquals(br, res.get("bottomRight"));
    }
}