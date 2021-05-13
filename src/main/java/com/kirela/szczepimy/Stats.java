package com.kirela.szczepimy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Stats {
    private final String outputDirectory;

    public Stats(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void store(Set<Main.SlotWithVoivodeship> results) throws IOException {
        counts(results);
        dates(results);
    }

    private void counts(Set<Main.SlotWithVoivodeship> results) throws IOException {
        Map<Voivodeship, Map<VaccineType, Long>> count = results.stream()
            .map(this::toSlot)
            .collect(
                Collectors.groupingBy(
                    k -> k.servicePoint().voivodeship(),
                    Collectors.groupingBy(
                        ExtendedResult.Slot::vaccineType,
                        Collectors.counting()
                    )
                )
            );

        for (Map.Entry<Voivodeship, Map<VaccineType, Long>> entry : count.entrySet()) {
            Voivodeship voivodeship = entry.getKey();
            Map<VaccineType, Long> stats = entry.getValue();
            var voiFile = Paths.get(outputDirectory, "_includes", "stats", "count", "%s.html".formatted(voivodeship.urlName()));
            voiFile.toFile().mkdirs();
            if (voiFile.toFile().exists()) {
                voiFile.toFile().delete();
            }
            Files.writeString(voiFile, """
                <tr data-date="%s">
                    <td>%s</td>
                """.formatted(LocalDateTime.now(), voivodeship.readable()),
                StandardOpenOption.CREATE_NEW
            );
            for (VaccineType vaccine : List.of(VaccineType.PFIZER, VaccineType.MODERNA, VaccineType.JJ, VaccineType.AZ)) {
                long sum;
                if (stats.containsKey(vaccine)) {
                    sum = stats.get(vaccine);
                } else {
                    sum = 0;
                }
                Files.writeString(voiFile, """
                        <td>%s</td>
                    """.formatted(sum),
                    StandardOpenOption.APPEND
                );
            }
            Files.writeString(voiFile, """
                    <td>%s</td>
                </tr>
                """.formatted(stats.values().stream().mapToLong(i -> i).sum()),
                StandardOpenOption.APPEND
            );
        }
    }

    private void dates(Set<Main.SlotWithVoivodeship> results) throws IOException {
        record VoivodeshipWithDate(Voivodeship voivodeship, VaccineType vaccineType, LocalDate date) {}

        Map<Voivodeship, Map<VaccineType, Optional<VoivodeshipWithDate>>> dates = results.stream()
            .map(this::toSlot)
            .map(s -> new VoivodeshipWithDate(
                s.servicePoint().voivodeship(),
                s.vaccineType(),
                LocalDate.ofInstant(s.startAt(), TableFormatter.ZONE)
            ))
            .collect(
                Collectors.groupingBy(
                    VoivodeshipWithDate::voivodeship,
                    Collectors.groupingBy(
                        VoivodeshipWithDate::vaccineType,
                        Collectors.maxBy(Comparator.comparingLong(v -> v.date().toEpochDay()))
                    )
                )
            );

        for (Map.Entry<Voivodeship, Map<VaccineType, Optional<VoivodeshipWithDate>>> entry : dates.entrySet()) {
            Voivodeship voivodeship = entry.getKey();
            Map<VaccineType, Optional<VoivodeshipWithDate>> stats = entry.getValue();

            var voiFile = Paths.get(outputDirectory, "_includes", "stats", "dates", "%s.html".formatted(voivodeship.urlName()));
            voiFile.toFile().mkdirs();
            if (voiFile.toFile().exists()) {
                voiFile.toFile().delete();
            }
            Files.writeString(voiFile, """
                <tr data-date="%s">
                    <td>%s</td>
                """.formatted(LocalDateTime.now(), voivodeship.readable()),
                StandardOpenOption.CREATE_NEW
            );
            for (VaccineType vaccine : List.of(VaccineType.PFIZER, VaccineType.MODERNA, VaccineType.JJ, VaccineType.AZ)) {
                LocalDate date;
                if (stats.containsKey(vaccine)) {
                    date = stats.get(vaccine).map(VoivodeshipWithDate::date).orElse(LocalDate.MAX);
                } else {
                    date = LocalDate.MAX;
                }
                Files.writeString(voiFile, """
                        <td data-order="%d">%s</td>
                    """.formatted(date.toEpochDay(), date == LocalDate.MAX ? "" : date.toString()),
                    StandardOpenOption.APPEND
                );
            }
            LocalDate minDate = stats.values().stream()
                .map(i -> i.orElse(new VoivodeshipWithDate(null, null, LocalDate.MAX)))
                .map(VoivodeshipWithDate::date)
                .min(Comparator.comparingLong(LocalDate::toEpochDay))
                .orElse(LocalDate.MAX);

            Files.writeString(voiFile, """
                    <td data-order="%d">%s</td>
                </tr>
                """.formatted(minDate.toEpochDay(), minDate == LocalDate.MIN ? "" : minDate.toString()),
                StandardOpenOption.APPEND
            );
        }
    }

    private ExtendedResult.Slot toSlot(Main.SlotWithVoivodeship slot1) {
        return new ExtendedResult.Slot(
            slot1.slot(),
            new ExtendedResult.ServicePoint(
                slot1.slot().servicePoint(),
                "",
                slot1.voivodeship(),
                ""
            )
        );
    }
}
