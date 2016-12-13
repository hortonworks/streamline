package org.apache.streamline.streams.catalog.storage;

import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.StorableTest;
import org.apache.streamline.streams.catalog.FileInfo;

import java.util.ArrayList;

public class CatalogTests {


    public ArrayList<StorableTest> getAllInMemoryTests() {
        return new ArrayList<StorableTest>() {{
            add(new FilesTest());
        }};
    }

    public ArrayList<StorableTest> getAllTests() {
        return new ArrayList<StorableTest>() {{
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

}
