package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Arrays;

@JsonSerialize(using = VoivodeshipSerializer.class)
@JsonDeserialize(using = VoivodeshipDeserializer.class)
public enum Voivodeship {
    DOLNOŚLĄSKIE(2, "dolnośląskie"),
    KUJAWSKO_POMORSKIE(4, "kujawsko-pomorskie"),
    LUBELSKIE(6, "lubelskie"),
    LUBUSKIE(8, "lubuskie"),
    ŁÓDZKIE(10, "łódzkie"),
    MAŁOPOLSKIE(12, "małopolskie"),
    MAZOWIECKIE(14, "mazowieckie"),
    OPOLSKIE(16, "opolskie"),
    PODKARPACKIE(18, "podkarpackie"),
    PODLASKIE(20, "podlaskie"),
    POMORSKIE(22, "pomorskie"),
    ŚLĄSKIE(24, "śląskie"),
    ŚWIĘTOKRZYSKIE(26, "świętokrzyskie"),
    WARMIŃSKO_MAZURSKIE(28, "warmińsko-mazurskie"),
    WIELKOPOLSKIE(30, "wielkopolskie"),
    ZACHODNIOPOMORSKIE(32, "zachodniopomorskie");

    private final int id;
    private final String readable;

    Voivodeship(int id, String readable) {
        this.id = id;
        this.readable = readable;
    }
    
    public int id() {
        return id;
    }

    public String readable() {
        return readable;
    }

    public String urlName() {
        return Gmina.normalize(readable()).replace('-', '_');
    }

    public static Voivodeship from(int num) {
        return Arrays.stream(Voivodeship.values())
            .filter(v -> v.id() == num)
            .findAny()
            .orElseThrow();
    }

}
