package com.kirela.szczepimy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GminaFinder {
    private static final Logger LOG = LogManager.getLogger(GminaFinder.class);
    private final Map<NormalizedCityVoivodeship, TercPlace> cities;

    public GminaFinder() {
        cities = new BufferedReader(
            new InputStreamReader(
                Objects.requireNonNull(
                    GminaFinder.class.getResourceAsStream("TERC_Adresowy_2021-04-15.csv")
                )
            )
        )
            .lines()
            .filter(l -> !l.startsWith("\uFEFFWOJ"))
            .filter(l -> l.contains(";"))
            .map(GminaFinder::toSimcCity)
            .collect(Collectors.toMap(TercPlace::key, k -> k, GminaFinder::preferCities));
    }

    private static TercPlace preferCities(TercPlace k1, TercPlace k2) {
        if (k1.typGminy().equals("1")) {
            return k1;
        }
        if (k2.typGminy().equals("1")) {
            return k2;
        }
        if (k1.typGminy().equals("4")) {
            return k1;
        }
        if (k2.typGminy().equals("4")) {
            return k2;
        }
        if (!k1.powiat().equals(k2.powiat())) {
            LOG.warn("Problem with city %s, terc1=%s, terc2=%s (same powiat)".formatted(k1.name(), k1.terc(), k2.terc()));
            return k1;
        }
//        if (!k1.gmina().equals(k2.gmina())) {
//            LOG.warn("Problem with city %s, terc1=%s, terc2=%s (same gmina)".formatted(k1.name(), k1.terc(), k2.terc()));
//            return k1
//        }
        throw new IllegalStateException("Don't know what to do: (%s) ? (%s), (%s)".formatted(k1.name(), k1.terc(), k2.terc()));
    }

    public Gmina find(String name, Voivodeship voivodeship) {
        var key = new NormalizedCityVoivodeship(Gmina.normalize(name), voivodeship);
        if (!cities.containsKey(key)) {
            throw new IllegalArgumentException("Can't find city %s in %s".formatted(name, voivodeship.readable()));
        }
        final TercPlace city = cities.get(key);
        return new Gmina(city.name(), city.terc(), 0);
    }

    private static TercPlace toSimcCity(String line) {
        try {
            String[] splitted = line.split(";");
            String terc =
                splitted[0] + splitted[1] + (splitted[2].isBlank() ? "01" : splitted[2]) + (splitted[3].isBlank() ?
                    "1" : splitted[3]);
            Voivodeship voivodeship = Voivodeship.from(Integer.parseInt(splitted[0]));
            return new TercPlace(splitted[4], terc, voivodeship);
        } catch (ArrayIndexOutOfBoundsException ex) {
            LOG.error("Issue with line: {}", line);
            throw ex;
        }
    }

    record TercPlace(String name, String terc, Voivodeship voivodeship) {

        public String powiat() {
            return terc.substring(2, 4);
        }

        public String gmina() {
            return terc.substring(4, 6);
        }

        public String typGminy() {
            return terc.substring(6);
        }

        public String normalized() {
            return Gmina.normalize(name);
        }

        public NormalizedCityVoivodeship key() {
            return new NormalizedCityVoivodeship(Gmina.normalize(name), voivodeship);
        }
    }

    record NormalizedCityVoivodeship(String name, Voivodeship voivodeship) {}
}
