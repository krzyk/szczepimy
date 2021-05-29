package com.kirela.szczepimy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlaceFinder {
    private static final Logger LOG = LogManager.getLogger(PlaceFinder.class);

    private final Map<NormalizedPlaceVoivodeship, RealNamePlace> places;

    public PlaceFinder() {
        places = new BufferedReader(
            new InputStreamReader(
                Objects.requireNonNull(
                    GminaFinder.class.getResourceAsStream("SIMC_Urzedowy_2021-04-15.csv")
                )
            )
        )
            .lines()
            .filter(l -> !l.startsWith("\uFEFFWOJ"))
            .filter(l -> l.contains(";"))
            .map(PlaceFinder::keyFromLine)
            .collect(Collectors.toMap(RealNamePlace::normalized, k -> k, (k1, k2) -> k1));
    }

    private static RealNamePlace keyFromLine(String line) {
        String[] splitted = line.split(";");
        return new RealNamePlace(splitted[6], splitted[7], key(splitted[6], Voivodeship.from(Integer.parseInt(splitted[0]))));
    }

    public RealNamePlace findInAddress(String name, Voivodeship voivodeship) {
        final String maybeCity;
        if (name.startsWith("Szczytno,")) {
            maybeCity = "Szczytno";
        }
        else if (name.contains(",")) {
            maybeCity = name.substring(name.lastIndexOf(',') + 1).trim();
        } else {
            maybeCity = name;
        }

        NormalizedPlaceVoivodeship normalizedPlace = key(maybeCity, voivodeship);
        if (!places.containsKey(normalizedPlace)) {
            LOG.error("Can't find place %s (key = %s) in %s".formatted(name, normalizedPlace, voivodeship.readable()));
            return new RealNamePlace(maybeCity, null, new NormalizedPlaceVoivodeship(maybeCity, voivodeship));
//            throw new IllegalArgumentException("Can't find place %s in %s".formatted(name, voivodeship.readable()));
        }
        return places.get(normalizedPlace);
    }

    public static NormalizedPlaceVoivodeship key(String inputName, Voivodeship voivodeship) {
        String name = Gmina.normalize(inputName);

        if (voivodeship.equals(Voivodeship.PODLASKIE) && name.equals("piatnica")) {
            name = "piatnica poduchowna";
        }
        if (voivodeship.equals(Voivodeship.MAZOWIECKIE) && name.equals("")) {
            name = "piatnica poduchowna";
        }
        return new NormalizedPlaceVoivodeship(
            name
                .replace("m. st. ", "")
                .replace("pawlowice/pniowek", "pniowek")
                .replace("dziegowice", "dziergowice")
                .replace("strzelce kraj.", "strzelce krajenskie")
                .replace("krasienin kolonia", "krasienin-kolonia")
                .replace("belchatow - szkola w dobrzelowie", "belchatow")
                .replace("lodz - poradnie", "lodz")
                .replace("milejow osada", "milejow")
                .replace("mlp.", "malopolski")
                .replace("wlkp.", "wielkopolski")
                .replace("ostrowiec sw.", "ostrowiec swietokrzyski")
                .replaceAll(" +", " ")
                .replaceAll("([^ ]+) ?- ?([^ ]+)", "$1-$2")
                .replaceAll("([^ ]+) [0-9]+[a-z]?\\.?$", "$1")
                .replaceAll("czerwiensk odrzanski", "czerwiensk")
                .replaceAll(" kolonia", "-kolonia")
                .replaceAll("gdansk .+", "gdansk")
                .replaceAll("krakow-.+", "krakow")
                .replaceAll("warszawa .+", "warszawa")
                .replaceAll("(.+) zdroj", "$1-zdroj")
            ,
            voivodeship
        );
    }

    public record RealNamePlace(String name, String simc, NormalizedPlaceVoivodeship normalized) {}
    public record NormalizedPlaceVoivodeship(String name, Voivodeship voivodeship) {}
}
