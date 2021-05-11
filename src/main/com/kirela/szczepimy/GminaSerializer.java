package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.JsonSerializer;

public class GminaSerializer extends JsonSerializer<Gmina> {
    @Override
    public void serialize(Gmina value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        gen.writeString(value.terc());
    }
}
