package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;

public class Stats {
    private final ObjectMapper mapper;

    public Stats(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void store(Set<Main.SlotWithVoivodeship> results) {
//        results.stream()
//            .collect(Collectors.counting(Main.SlotWithVoivodeship::voivodeship));
    }
}
