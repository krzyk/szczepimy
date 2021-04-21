package com.kirela.szczepimy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class VoivodeshipSerializer extends JsonSerializer<Voivodeship> {
    @Override
    public void serialize(Voivodeship value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString("%02d".formatted(value.id()));
    }
}
