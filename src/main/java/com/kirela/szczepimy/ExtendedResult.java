package com.kirela.szczepimy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExtendedResult(String geoDepth, List<Slot> list) {
    public static record Slot(
        UUID id, Instant startAt, int duration, ServicePoint servicePoint,
        VaccineType vaccineType, int dose, String status, Search search
    ) {
        public Slot(BasicSlotWithSearch slot, ServicePoint servicePoint) {
            this(slot.id(), slot.startAt(), slot.duration(), servicePoint, slot.vaccineType(), slot.dose(), slot.status(), slot.search());
        }

    }

    public static record ServicePoint(UUID id, String name, String addressText, String place, Voivodeship voivodeship, String simc) {
        public ServicePoint(Result.BasicServicePoint basic, String place, Voivodeship voivodeship, String simc) {
            this(
                basic.id(),
                basic.name(),
                streetAddress(basic),
                place,
                voivodeship,
                simc
            );
        }

        private static String streetAddress(Result.BasicServicePoint basic) {
            if (basic.addressText().contains(",")) {
                return basic.addressText().substring(0, basic.addressText().lastIndexOf(','));
            }
            return basic.addressText();
        }
    }
}
