package com.kirela.szczepimy;

import java.time.Instant;
import java.util.Objects;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (BasicSlotWithSearch) obj;
        return Objects.equals(this.id, that.id) &&
            Objects.equals(this.startAt, that.startAt) &&
            this.duration == that.duration &&
            Objects.equals(this.servicePoint, that.servicePoint) &&
            Objects.equals(this.vaccineType, that.vaccineType) &&
            this.dose == that.dose &&
            Objects.equals(this.status, that.status) &&
            Objects.equals(this.mobility, that.mobility);
        // ignore search
    }

    @Override
    public int hashCode() {
        // ignore search
        return Objects.hash(id, startAt, duration, servicePoint, vaccineType, dose, status, mobility);
    }
}
