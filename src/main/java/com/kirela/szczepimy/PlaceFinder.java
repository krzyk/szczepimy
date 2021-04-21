package com.kirela.szczepimy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlaceFinder {
    private final Map<NormalizedPlaceVoivodeship, String> places;

    public PlaceFinder() {
        places = new BufferedReader(
            new InputStreamReader(
                Objects.requireNonNull(
                    GminaFinder.class.getResourceAsStream("SIMC_Adresowy_2021-04-15.csv")
                )
            )
        )
            .lines()
            .filter(l -> !l.startsWith("\uFEFFWOJ"))
            .filter(l -> l.contains(";"))
            .map(PlaceFinder::keyFromLine)
            .collect(Collectors.toMap(RealNamePlace::normalized, RealNamePlace::name, (k1, k2) -> k1));
    }

    private static RealNamePlace keyFromLine(String line) {
        String[] splitted = line.split(";");
        return new RealNamePlace(splitted[6], key(splitted[6], Voivodeship.from(Integer.parseInt(splitted[0]))));
    }

    public String findInAddress(String name, Voivodeship voivodeship) {

        NormalizedPlaceVoivodeship normalizedPlace = key(
            name.substring(name.lastIndexOf(',') + 1).trim(),
            voivodeship
        );
        if (!places.containsKey(normalizedPlace)) {
            throw new IllegalArgumentException("Can't find place %s in %s".formatted(name, voivodeship.readable()));
        }
        return places.get(normalizedPlace);
    }

    public static NormalizedPlaceVoivodeship key(String name, Voivodeship voivodeship) {
        return new NormalizedPlaceVoivodeship(
            Gmina.normalize(name)
                .replace("m. st. ", "")
                .replace("pawlowice/pniowek", "pniowek")
                .replaceAll("([^ ]+) [0-9]+$", "$1")
                .replaceAll("gdansk .+", "gdansk")
                .replaceAll("krakow-.+", "krakow")
                .replaceAll("warszawa .+", "warszawa")
                .replaceAll("(.+) zdroj", "$1-zdroj")
            ,
            voivodeship
        );
    }

    record RealNamePlace(String name, NormalizedPlaceVoivodeship normalized) {}
    record NormalizedPlaceVoivodeship(String name, Voivodeship voivodeship) {}
}
