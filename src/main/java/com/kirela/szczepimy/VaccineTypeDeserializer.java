package com.kirela.szczepimy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Arrays;

public class VaccineTypeDeserializer extends JsonDeserializer<VaccineType> {
    @Override
    public VaccineType deserialize(JsonParser parser, DeserializationContext context)
        throws IOException, JsonProcessingException {
        JsonNode node = parser.getCodec().readTree(parser);
        return Arrays.stream(VaccineType.values())
            .filter(v -> v.toString().endsWith(node.asText()))
            .findAny()
            .orElseThrow();
    }
}
