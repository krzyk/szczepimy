package com.kirela.szczepimy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Result(String geoDepth, List<BasicSlot> list) {
    public static record BasicSlot(
        UUID id, Instant startAt, int duration, BasicServicePoint servicePoint,
        VaccineType vaccineType, int dose, String status
    ) {}

    public static record BasicServicePoint(UUID id, String name, String addressText) {
        public String normalizedCity() {
            return Gmina.normalize(addressText.substring(addressText.lastIndexOf(",") + 1)).trim().toLowerCase();
        }
    }
}
