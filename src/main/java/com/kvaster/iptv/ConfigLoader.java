package com.kvaster.iptv;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kvaster.utils.serialize.RelativeFileModule;

public class ConfigLoader {
    public static <T> T loadConfig(File configFile, Class<T> configClass) {
        try {
            ObjectMapper mapper = YAMLMapper.builder()
                    .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .visibility(new VisibilityChecker.Std(JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.ANY, JsonAutoDetect.Visibility.ANY))
                    .configure(MapperFeature.AUTO_DETECT_GETTERS, false)
                    .configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false).configure(MapperFeature.AUTO_DETECT_SETTERS, false)
                    .configure(MapperFeature.AUTO_DETECT_SETTERS, false)
                    .addModules(new RelativeFileModule(configFile.getParentFile()), new JavaTimeModule())
                    .build();

            return mapper.readValue(configFile, configClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
