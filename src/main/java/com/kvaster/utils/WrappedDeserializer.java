package com.kvaster.utils.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

import static java.util.Objects.requireNonNull;

public class WrappedDeserializer<T> extends JsonDeserializer<T> implements ResolvableDeserializer {
    private JsonDeserializer<T> deserializer;

    public WrappedDeserializer(JsonDeserializer<T> deserializer) {
        this.deserializer = requireNonNull(deserializer);
    }

    @Override
    public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return afterDeserialize(deserializer.deserialize(jp, ctxt));
    }

    @Override
    public T deserialize(JsonParser jp, DeserializationContext ctxt, T intoValue) throws IOException {
        return afterDeserialize(deserializer.deserialize(jp, ctxt, intoValue));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserializeWithType(
            JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer
    ) throws IOException {
        return afterDeserialize((T) deserializer.deserializeWithType(jp, ctxt, typeDeserializer));
    }

    protected T afterDeserialize(T obj) {
        return obj;
    }

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        if (deserializer instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) deserializer).resolve(ctxt);
        }
    }
}
