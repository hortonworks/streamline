package com.hortonworks.iotas.streams.catalog.storage;

import com.google.common.collect.Lists;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableTest;
import com.hortonworks.iotas.streams.catalog.NotifierInfo;
import com.hortonworks.iotas.streams.catalog.StreamInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by mkumar on 7/8/16.
 */
public class StreamCatagoryTests {


   public ArrayList<StorableTest> getAllTests() {
       return new ArrayList<StorableTest>() {{
           add(new StreamInfoTest());
           add(new NotifierInfoTest());
       }};
   }

    public class StreamInfoTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                List<Schema.Field> fields = Lists.newArrayList(Schema.Field.of("foo", Schema.Type.INTEGER), Schema.Field.of("bar", Schema.Type.STRING));
                List<Schema.Field> updatedFields = Lists.newArrayList(Schema.Field.of("foo", Schema.Type.INTEGER), Schema.Field.of("fubar", Schema.Type.STRING));

                add(createStreamInfo(1l, fields));
                add(createStreamInfo(1l, updatedFields));
                add(createStreamInfo(2l, fields));
                add(createStreamInfo(3l, fields));
            }};
        }

        private StreamInfo createStreamInfo(long id, List<Schema.Field> fields) {
            StreamInfo streamInfo = new StreamInfo();
            streamInfo.setId(id);
            streamInfo.setTopologyId(1L);
            streamInfo.setStreamId("Stream-" + id);
            streamInfo.setFields(fields);
            streamInfo.setTimestamp(System.currentTimeMillis());

            return streamInfo;
        }
    }

    public class NotifierInfoTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                add(createNotifierInfo(1l, "notifier-1"));
                add(createNotifierInfo(1l, "notifier-2"));
                add(createNotifierInfo(2l, "notifier-3"));
                add(createNotifierInfo(3l, "notifier-4"));
            }};
        }

        protected NotifierInfo createNotifierInfo(Long id, String name) {
            NotifierInfo notifierInfo = new NotifierInfo();
            notifierInfo.setId(id);
            notifierInfo.setName(name);
            Map<String, String> props = new HashMap<String, String>(){{put("cur", new Random().nextInt()+"");}};
            notifierInfo.setProperties(props);
            notifierInfo.setTimestamp(System.currentTimeMillis());
            return notifierInfo;
        }
    }
}
