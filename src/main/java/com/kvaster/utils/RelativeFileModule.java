package com.kvaster.utils.serialize;

import java.io.File;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;

public class RelativeFileModule extends Module {
    private class RelativeFileDeserializer extends WrappedDeserializer<File> {
        public RelativeFileDeserializer(JsonDeserializer<File> deserializer) {
            super(deserializer);
        }

        @Override
        public File afterDeserialize(File file) {
            if (file.isAbsolute())
                return file;

            return new File(base, file.getPath());
        }
    }

    private class RealtiveFileDeserializerModifier extends BeanDeserializerModifier {
        @Override
        @SuppressWarnings("unchecked")
        public JsonDeserializer<?> modifyDeserializer(
                DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer
        ) {
            if (beanDesc.getBeanClass().isAssignableFrom(File.class)) {
                return new RelativeFileDeserializer((JsonDeserializer<File>)deserializer);
            }

            return deserializer;
        }
    }

    private final File base;

    public RelativeFileModule(File base) {
        this.base = base;
    }

    @Override
    public String getModuleName()
    {
        return getClass().getSimpleName();
    }

    @Override
    public Version version()
    {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(Module.SetupContext context) {
        context.addBeanDeserializerModifier(new RealtiveFileDeserializerModifier());
    }
}
