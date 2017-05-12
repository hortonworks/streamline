package com.hortonworks.streamline.streams.storm.common;

import com.hortonworks.streamline.common.Constants;
import org.apache.commons.io.FileUtils;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StormJaasCreator {
    public static final String STORM_JAAS_CONFIG_TEMPLATE = "storm_jaas.config_template";

    private final String stormJaasConfigTemplate;
    private final String keyTabPath;
    private final String streamlinePrincipal;

    public StormJaasCreator() {
        URL templateFileUrl = getClass().getClassLoader().getResource(STORM_JAAS_CONFIG_TEMPLATE);
        if (templateFileUrl == null) {
            throw new RuntimeException("Unable to read JAAS template file for Storm.");
        }

        try {
            stormJaasConfigTemplate = FileUtils.readFileToString(new File(templateFileUrl.toURI()), Charset.defaultCharset());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Unable to read JAAS template file for Storm.");
        }

        Configuration configuration = Configuration.getConfiguration();
        AppConfigurationEntry[] streamlineConfigurations = configuration.getAppConfigurationEntry(Constants.JAAS_STREAMLINE_APP_CONFIG_ENTRY_NAME);
        if (streamlineConfigurations == null || streamlineConfigurations.length == 0) {
            throw new RuntimeException("Streamline is not initialized with JAAS config. Unable to create JAAS for Storm.");
        }

        AppConfigurationEntry streamlineConf = streamlineConfigurations[0];
        Map<String, ?> options = streamlineConf.getOptions();

        keyTabPath = (String) options.get("keyTab");
        streamlinePrincipal = (String) options.get("principal");
    }

    public File create(String desiredFilePath, String serviceName) throws IOException {

        String jaasConfig = stormJaasConfigTemplate;
        jaasConfig = jaasConfig.replace("{{keyTab}}", keyTabPath);
        jaasConfig = jaasConfig.replace("{{serviceName}}", serviceName);

        jaasConfig = jaasConfig.replace("{{principal}}", streamlinePrincipal);

        try (FileWriter fw = new FileWriter(desiredFilePath)) {
            fw.write(jaasConfig);
        }

        return new File(desiredFilePath);
    }
}
