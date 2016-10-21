package org.apache.streamline.streams.catalog.storage;

import org.apache.streamline.common.Schema;
import org.apache.streamline.registries.parser.ParserInfo;
import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.StorableTest;
import org.apache.streamline.streams.catalog.FileInfo;

import java.util.ArrayList;

public class CatalogTests {


    public ArrayList<StorableTest> getAllInMemoryTests() {
        return new ArrayList<StorableTest>() {{
            add(new ParsersTest());
            add(new FilesTest());
        }};
    }

    public ArrayList<StorableTest> getAllTests() {
        return new ArrayList<StorableTest>() {{
            add(new ParsersTest());
            add(new FilesTest());
        }};
    }

    public class FilesTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                add(createFiles(1l, "file-1"));
                add(createFiles(1l, "file-2"));
                add(createFiles(2l, "file-3"));
                add(createFiles(3l, "file-4"));
            }};
        }

        protected FileInfo createFiles(Long id, String name) {
            FileInfo file = new FileInfo();
            file.setId(id);
            file.setName(name);
            file.setStoredFileName("/tmp/parser.jar");
            file.setVersion(0l);
            file.setTimestamp(System.currentTimeMillis());
            return file;
        }
    }


    public class ParsersTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                add(createParserInfo(1l, "parser-1"));
                add(createParserInfo(1l, "parser-2"));
                add(createParserInfo(2l, "parser-3"));
                add(createParserInfo(3l, "parser-4"));
            }};
        }

        protected ParserInfo createParserInfo(Long id, String name) {
            ParserInfo pi = new ParserInfo();
            pi.setId(id);
            pi.setName(name);
            pi.setClassName("com.org.apache.TestParser");
            pi.setJarStoragePath("/tmp/parser.jar");
            pi.setParserSchema(new Schema.SchemaBuilder().fields(new Schema.Field("deviceId", Schema.Type.LONG), new Schema.Field("deviceName", Schema.Type.STRING)).build());
            pi.setVersion(0l);
            pi.setTimestamp(System.currentTimeMillis());
            return pi;
        }
    }

}
