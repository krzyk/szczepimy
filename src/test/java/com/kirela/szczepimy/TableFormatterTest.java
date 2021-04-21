package com.kirela.szczepimy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TableFormatterTest {

    @Test
    public void rangeAfterRange() {
        int duration = 5;
        final Instant time = LocalDateTime.parse("2020-01-01T10:00:00").atZone(ZoneId.of("Europe/Warsaw")).toInstant();
        ExtendedResult.Slot slot = create(duration, time);
        List<TableFormatter.TimeRange> ranges = TableFormatter.getRanges(
            List.of(
                slot,
                create(duration, time.plusSeconds(Duration.ofMinutes(duration).toSeconds())),
                create(duration, time.plusSeconds(Duration.ofMinutes(duration).toSeconds())),
                create(duration, time.plusSeconds(Duration.ofMinutes(duration*2).toSeconds())),
                create(duration, time.plusSeconds(Duration.ofMinutes(duration*2).toSeconds())),
                create(duration, time.plusSeconds(Duration.ofMinutes(duration*3).toSeconds()))
            ),
            slot
        );

        Assertions.assertThat(ranges.get(0)).isEqualTo(
            new TableFormatter.TimeRange(LocalTime.parse("10:00"), LocalTime.parse("10:15"))
        );
        Assertions.assertThat(ranges).hasSize(1);
    }

    @Test
    public void twoRanges() {
        int duration = 5;
        final Instant time = LocalDateTime.parse("2020-01-01T10:00:00").atZone(ZoneId.of("Europe/Warsaw")).toInstant();
        ExtendedResult.Slot slot = create(duration, time);
        List<TableFormatter.TimeRange> ranges = TableFormatter.getRanges(
            List.of(
                slot,
                create(duration, time.plusSeconds(Duration.ofMinutes(duration).toSeconds())),
                create(duration, time.plusSeconds(Duration.ofMinutes(duration*2).toSeconds())),
                create(duration, time.plusSeconds(Duration.ofMinutes(duration*3).toSeconds())),
                create(duration, time.plusSeconds(Duration.ofMinutes(duration*5).toSeconds())),
                create(duration, time.plusSeconds(Duration.ofMinutes(duration*6).toSeconds()))
            ),
            slot
        );

        Assertions.assertThat(ranges.get(0)).isEqualTo(
            new TableFormatter.TimeRange(LocalTime.parse("10:00"), LocalTime.parse("10:15"))
        );
        Assertions.assertThat(ranges.get(1)).isEqualTo(
            new TableFormatter.TimeRange(LocalTime.parse("10:25"), LocalTime.parse("10:30"))
        );
        Assertions.assertThat(ranges).hasSize(2);
    }

    private ExtendedResult.Slot create(int duration, Instant time) {
        ExtendedResult.Slot slot = new ExtendedResult.Slot(
            UUID.randomUUID(),
            time,
            duration,
            new ExtendedResult.ServicePoint(
                UUID.randomUUID(),
                "name",
                "address",
                "place",
                Voivodeship.DOLNOŚLĄSKIE
            ),
            VaccineType.PFIZER,
            1,
            null
        );
        return slot;
    }
}
