package org.apache.streamline.streams.layout.storm;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*
{
"from" : {"stream": "stream1", "key": "k1"},
"joins" :
  [
    {"type" : "left",  "stream": "s2", "key":"k2", "with": "s1"},
    {"type" : "left",  "stream": "s3", "key":"k3", "with": "s1"},
    {"type" : "inner", "stream": "s4", "key":"k4", "with": "s2"}
  ],
  "outputKeys" : [ "k1", "k2" ],
  "window" : { "windowLength": { "count" : 20 }, {"slidingInterval" : { "count": 10} } }
}
 */

public class JoinBoltFluxComponent extends AbstractFluxComponent {

    @Override
    protected void generateComponent()  {
        String boltId        = "joinBolt_" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.streamline.streams.runtime.storm.bolt.query.WindowedQueryBolt";

        String firstStream = conf.get("firstStream").toString();
        String firstStreamKey = conf.get("firstStreamKey").toString();

        List boltConstructorArgs = new ArrayList();
        boltConstructorArgs.add("STREAM");
        boltConstructorArgs.add(firstStream);
        boltConstructorArgs.add(firstStreamKey);


        String[] configMethodNames = getConfiguredMethodNames(conf);
        Object[] configValues = getConfiguredMethodArgs(conf);

        List configMethods = getConfigMethodsYaml(configMethodNames, configValues);

        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, configMethods);
        addParallelismToComponent();

    }

    private static String[] getConfiguredMethodNames(Map<String, Object> conf) {
        ArrayList<String> result = new ArrayList<>(conf.size());

        Object val;
        if ( (val = conf.get("joins")) != null  ) {
            for (Object joinItem : ((Object[]) val)) {
                Map<String, Object> ji = ((Map<String, Object>) joinItem);
                String joinType = ji.get("type").toString();
                if( joinType.compareToIgnoreCase("inner")==0 )
                    result.add("join");
                else if( joinType.compareToIgnoreCase("left")==0 )
                    result.add("left");
                else
                    throw new IllegalArgumentException("Unsupported Join type: " + joinType);
            }
        }
        if( conf.containsKey("outputKeys") )
            result.add("selectStreamLine");

        if( conf.containsKey("window") )
            result.add("withWindowConfig");

        return result.toArray(new String[]{});
    }


    private Object[] getConfiguredMethodArgs(Map<String, Object> conf) {
        ArrayList<Object[]> result = new ArrayList<>(conf.size());

        // joins
        Object val;
        if( (val = conf.get("joins")) != null  ) {
            for (Object joinInfo : ((Object[]) val)) {
                Map<String, Object> ji = ((Map<String, Object>) joinInfo);
                result.add( getJoinArgs(ji) );
            }
        }

        // select()
        String[] outputKeys = (String[]) conf.get("outputKeys");
        String outputKeysStr = String.join(",", outputKeys);
        result.add(new String[]{outputKeysStr});

        // window config
        String windowID = addWindowToComponents((Map<String, Object>) conf.get("window"));
        result.add( new Object[]{ getRefYaml(windowID) } );

        return result.toArray(new Object[]{});
    }

    private String[] getJoinArgs(Map<String, Object> ji) {
        return new String[]{ji.get("stream").toString(), ji.get("key").toString(), ji.get("with").toString()};
    }

    // Creates component for the window and add it to the components list
    // returns the window ID
    private String addWindowToComponents(Map<String,Object> windowMap) {
        String windowId = "window_" + UUID_FOR_COMPONENTS;
        String windowClassName = "org.apache.streamline.streams.layout.component.rule.expression.Window";
        List constructorArgs = new ArrayList();
        try {
            String windowJson = new ObjectMapper().writeValueAsString( windowMap );
            constructorArgs.add(windowJson);
            this.addToComponents(this.createComponent(windowId, windowClassName, null, constructorArgs, null));
            return windowId;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to crate json for window definition", e);
        }
    }

    public static void main(String[] args) throws Exception {
        JoinBoltFluxComponent me = new JoinBoltFluxComponent();
        List<Map.Entry<String, Map<String, Object>>> map = getYamlComponents(me);
        createYamlFile(map);

        BufferedReader br = new BufferedReader(new FileReader("/tmp/flux.yaml"));
        for (String line; (line = br.readLine()) != null;) {
            System.out.print(line);
        }
        br.close();
    }


    private static List<Map.Entry<String, Map<String, Object>>> getYamlComponents(FluxComponent fluxComponent) throws IOException {
        String json = "{\n" +
                "\"from\" : {\"stream\": \"stream1\", \"key\": \"k1\"},\n" +
                "\"joins\" :\n" +
                "  [\n" +
                "    {\"type\" : \"left\",  \"stream\": \"s2\", \"key\":\"k2\", \"with\": \"s1\"},\n" +
                "    {\"type\" : \"left\",  \"stream\": \"s3\", \"key\":\"k3\", \"with\": \"s1\"},\n" +
                "    {\"type\" : \"inner\", \"stream\": \"s4\", \"key\":\"k4\", \"with\": \"s2\"}\n" +
                "  ],\n" +
                "  \"outputKeys\" : [ \"k1\", \"k2\" ],\n" +
                "  \"window\" : { \"windowLength\": { \"count\" : 20 }, {\"slidingInterval\" : { \"count\": 10} } }\n" +
                "}";

        Map<String, Object> props = new ObjectMapper().readValue(json, new TypeReference<HashMap<String, Object>>(){});

        fluxComponent.withConfig(props);

        List<Map.Entry<String, Map<String, Object>>> keysAndComponents = new ArrayList<>();

        for (Map<String, Object> referencedComponent : fluxComponent.getReferencedComponents()) {
            keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_COMPONENTS, referencedComponent));
        }

        Map<String, Object> yamlComponent = fluxComponent.getComponent();
        yamlComponent.put(StormTopologyLayoutConstants.YAML_KEY_ID, "roshan");


        keysAndComponents.add( makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS,  yamlComponent) );

        return keysAndComponents;
    }

    private static Map.Entry<String, Map<String, Object>> makeEntry(String key, Map<String, Object> component) {
        return new AbstractMap.SimpleImmutableEntry<>(key, component);
    }


    private static String createYamlFile (List<Map.Entry<String, Map<String, Object>>> keysAndComponents) throws Exception {
        Map<String, Object> yamlMap;
        File f;
        FileWriter fileWriter = null;
        try {
            f = new File("/tmp/flux.yaml");
            if (f.exists()) {
                if (!f.delete()) {
                    throw new Exception("Unable to delete old flux ");
                }
            }
            yamlMap = new LinkedHashMap<>();
            yamlMap.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "topo1");

            for (Map.Entry<String, Map<String, Object>> entry: keysAndComponents) {
                addComponentToCollection(yamlMap, entry.getValue(), entry.getKey());
            }


            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setSplitLines(false);
            //options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
            Yaml yaml = new Yaml (options);
            fileWriter = new FileWriter(f);
            yaml.dump(yamlMap, fileWriter);
            return f.getAbsolutePath();
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }

    }


    private static void addComponentToCollection (Map<String, Object> yamlMap, Map<String, Object> yamlComponent, String collectionKey) {
        if (yamlComponent == null ) {
            return;
        }

        List<Map<String, Object>> components = (List) yamlMap.get(collectionKey);
        if (components == null) {
            components = new ArrayList<>();
            yamlMap.put(collectionKey, components);
        }
        components.add(yamlComponent);
    }
}
