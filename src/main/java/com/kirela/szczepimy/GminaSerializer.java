package com.kirela.szczepimy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class GminaSerializer extends JsonSerializer<Gmina> {
    @Override
    public void serialize(Gmina value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        gen.writeString(value.terc().toString());
    }
}
