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

//    @Test
//    public void guessDuration() {
//        int duration = 5;
//        List<TableFormatter.TimeRange> ranges = TableFormatter.getRanges(
//            List.of(
//                create(duration*3, LocalDateTime.parse("2020-01-01T10:00:00")),
//                create(duration, LocalDateTime.parse("2020-01-01T10:05:00")),
//                create(duration, LocalDateTime.parse("2020-01-01T10:10:00")),
//                create(duration, LocalDateTime.parse("2020-01-01T10:15:00"))
//            ),
//            create(duration*3, LocalDateTime.parse("2020-01-01T10:00:00"))
//        );
//
//        Assertions.assertThat(ranges.get(0)).isEqualTo(
//            new TableFormatter.TimeRange(LocalTime.parse("10:00"), LocalTime.parse("10:15"))
//        );
//        Assertions.assertThat(ranges).hasSize(1);
//    }

    @Test
    public void longerRangeWithBreak() {
        int duration = 15;
        List<TableFormatter.TimeRange> ranges = TableFormatter.getRanges(
            List.of(
                create(duration, LocalDateTime.parse("2020-01-01T15:00:00")),
                create(duration, LocalDateTime.parse("2020-01-01T15:15:00")),
                create(duration, LocalDateTime.parse("2020-01-01T15:30:00")),
                create(duration, LocalDateTime.parse("2020-01-01T16:45:00")),
                create(duration, LocalDateTime.parse("2020-01-01T17:00:00")),
                create(duration, LocalDateTime.parse("2020-01-01T17:15:00")),
                create(duration, LocalDateTime.parse("2020-01-01T17:30:00")),
                create(duration, LocalDateTime.parse("2020-01-01T17:45:00")),
                create(duration, LocalDateTime.parse("2020-01-01T18:00:00"))
            ),
            create(duration, LocalDateTime.parse("2020-01-01T15:00:00"))
        );

        Assertions.assertThat(ranges).containsExactly(
            new TableFormatter.TimeRange(LocalTime.parse("15:00"), LocalTime.parse("15:30")),
            new TableFormatter.TimeRange(LocalTime.parse("16:45"), LocalTime.parse("18:00"))
        );
    }

    @Test
    public void rangeAfterRange() {
        int duration = 5;
        List<TableFormatter.TimeRange> ranges = TableFormatter.getRanges(
            List.of(
                create(duration, LocalDateTime.parse("2020-01-01T10:00:00")),
                create(duration, LocalDateTime.parse("2020-01-01T10:05:00")),
                create(duration, LocalDateTime.parse("2020-01-01T10:05:00")),
                create(duration, LocalDateTime.parse("2020-01-01T10:10:00")),
                create(duration, LocalDateTime.parse("2020-01-01T10:10:00")),
                create(duration, LocalDateTime.parse("2020-01-01T10:15:00"))
            ),
            create(duration, LocalDateTime.parse("2020-01-01T10:00:00"))
        );

        Assertions.assertThat(ranges.get(0)).isEqualTo(
            new TableFormatter.TimeRange(LocalTime.parse("10:00"), LocalTime.parse("10:15"))
        );
        Assertions.assertThat(ranges).hasSize(1);
    }

    @Test
    public void twoRanges() {
        int duration = 5;
        LocalDateTime time = LocalDateTime.parse("2020-01-01T10:00:00");
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

    private ExtendedResult.Slot create(int duration, LocalDateTime time) {
        return new ExtendedResult.Slot(
            UUID.randomUUID(),
            time.atZone(ZoneId.of("Europe/Warsaw")).toInstant(),
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
    }
}
