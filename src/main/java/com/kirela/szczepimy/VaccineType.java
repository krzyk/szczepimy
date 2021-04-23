package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonDeserialize(using = VaccineTypeDeserializer.class)
@JsonSerialize(using = VaccineTypeSerializer.class)
public enum VaccineType {
    PFIZER("cov19.pfizer", "Pfizer"),
    MODERNA("cov19.moderna", "Moderna"),
    JJ("cov19.johnson_and_johnson", "J &amp; J"),
    AZ("cov19.astra_zeneca", "AstraZeneca");

    private final String name;
    private final String readable;

    VaccineType(String name, String readable) {
        this.name = name;
        this.readable = readable;
    }

    @Override
    public String toString() {
        return name;
    }

    public String readable() {
        return readable;
    }
}
