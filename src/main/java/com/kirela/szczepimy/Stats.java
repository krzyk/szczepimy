package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Stats {
    private final String outputDirectory;

    public Stats(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void store(Set<Main.SlotWithVoivodeship> results) throws IOException {
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
                <tr>
                    <td>%s</td>
                """.formatted(voivodeship.readable()),
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
