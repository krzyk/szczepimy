package com.kirela.szczepimy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExtendedResult(String geoDepth, List<Slot> list) {
    public static record Slot(
        UUID id, Instant startAt, int duration, ServicePoint servicePoint,
        VaccineType vaccineType, int dose, String status
    ) {
        public Slot(Result.BasicSlot slot, ServicePoint servicePoint) {
            this(slot.id(), slot.startAt(), slot.duration(), servicePoint, slot.vaccineType(), slot.dose(), slot.status());
        }

    }

    public static record ServicePoint(UUID id, String name, String addressText, String place, Voivodeship voivodeship) {
        public ServicePoint(Result.BasicServicePoint basic, String place, Voivodeship voivodeship) {
            this(
                basic.id(),
                basic.name(),
                basic.addressText().substring(0, basic.addressText().lastIndexOf(',')),
                place,
                voivodeship
            );
        }
    }
}
