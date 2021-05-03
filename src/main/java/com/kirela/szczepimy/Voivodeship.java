package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Arrays;

@JsonSerialize(using = VoivodeshipSerializer.class)
@JsonDeserialize(using = VoivodeshipDeserializer.class)
public enum Voivodeship {
    DOLNOŚLĄSKIE(2, "dolnośląskie", new Coordinates("50.95083795", "16.36124870197179")),
    KUJAWSKO_POMORSKIE(4, "kujawsko-pomorskie", new Coordinates("53.3220016", "18.3392939")),
    LUBELSKIE(6, "lubelskie", new Coordinates("50.8586338", "22.7732404")),
    LUBUSKIE(8, "lubuskie", new Coordinates("52.1001754", "15.3605075")),
    ŁÓDZKIE(10, "łódzkie", new Coordinates("51.4721678", "19.3460637")),
    MAŁOPOLSKIE(12, "małopolskie", new Coordinates("49.790952", "20.3793521")),
    MAZOWIECKIE(14, "mazowieckie", new Coordinates("52.5461934", "21.2073404")),
    OPOLSKIE(16, "opolskie", new Coordinates("50.8918612", "17.9321175")),
    PODKARPACKIE(18, "podkarpackie", new Coordinates("49.9927121", "22.177107")),
    PODLASKIE(20, "podlaskie", new Coordinates("53.2668455", "22.8525787")),
    POMORSKIE(22, "pomorskie", new Coordinates("54.24556", "18.1099")),
    ŚLĄSKIE(24, "śląskie", new Coordinates("50.5687422", "19.2343995")),
    ŚWIĘTOKRZYSKIE(26, "świętokrzyskie", new Coordinates("50.7504894", "20.7829122")),
    WARMIŃSKO_MAZURSKIE(28, "warmińsko-mazurskie", new Coordinates("53.9311892", "21.1260808")),
    WIELKOPOLSKIE(30, "wielkopolskie", new Coordinates("52.1458506", "17.397672")),
    ZACHODNIOPOMORSKIE(32, "zachodniopomorskie", new Coordinates("53.5450793", "15.5661586"));

    private final int id;
    private final String readable;
    private final Coordinates cords;

    Voivodeship(int id, String readable, Coordinates cords) {
        this.id = id;
        this.readable = readable;
        this.cords = cords;
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

    public Coordinates cords() {
        return cords;
    }
}
