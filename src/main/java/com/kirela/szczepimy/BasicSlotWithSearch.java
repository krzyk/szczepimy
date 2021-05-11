package com.kirela.szczepimy;

import java.time.Instant;
import java.util.UUID;

public record BasicSlotWithSearch(
    UUID id, Instant startAt, int duration, Result.BasicServicePoint servicePoint,
    VaccineType vaccineType, int dose, String status, String mobility,
    Main.Search search
) {
    public BasicSlotWithSearch(Result.BasicSlot slot, Main.Search srh) {
        this(
            slot.id(),
            slot.startAt(),
            slot.duration(),
            slot.servicePoint(),
            slot.vaccineType(),
            slot.dose(),
            slot.status(),
            slot.mobility(),
            srh
        );
    }
}
