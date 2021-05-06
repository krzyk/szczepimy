package com.kirela.szczepimy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlaceCoords {
    private static final Logger LOG = LogManager.getLogger(PlaceCoords.class);

    private final Map<String, Coordinates> coords;

    public PlaceCoords() {
        coords = new BufferedReader(
            new InputStreamReader(
                Objects.requireNonNull(
                    GminaFinder.class.getResourceAsStream("coords.csv")
                )
            )
        )
            .lines()
            .map(l -> l.split(","))
            .map(l -> Map.entry(l[1], new Coordinates(l[2], l[3])))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Coordinates find(String simc, String name) {
        if (name == null) {
            LOG.warn("Missing SIMC for {}", name);
        }
        if (!coords.containsKey(simc)) {
            LOG.warn("Can't find simc {} for {} in coords", simc, name);
        }
        return coords.get(simc);
    }
}
