package com.kirela.szczepimy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class VaccineTypeSerializer extends JsonSerializer<VaccineType> {
    @Override
    public void serialize(VaccineType value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        gen.writeString(value.toString());
    }
}
