package com.kirela.szczepimy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class VoivodeshipDeserializer extends JsonDeserializer<Voivodeship> {
    @Override
    public Voivodeship deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node.isInt()) {
            return Voivodeship.from(node.asInt());
        } else if (node.isTextual()) {
            return Voivodeship.valueOf(node.asText().replace("-", "_"));
        } else {
            throw new IllegalArgumentException("Unknown type for voivodeship: %s".formatted(node.getNodeType()));
        }
    }
}
