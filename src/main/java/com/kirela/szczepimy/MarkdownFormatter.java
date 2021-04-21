package com.kirela.szczepimy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MarkdownFormatter {

    private final String outputDirectory;

    public MarkdownFormatter(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    public void store(PlaceFinder placeFinder, Set<Main.SlotWithVoivodeship> results)
        throws IOException {
        Map<Voivodeship, Map<String, Map<VaccineType, List<ExtendedResult.Slot>>>> groupByVoi = results.stream()
            .map(
                s -> new ExtendedResult.Slot(
                    s.slot(),
                    new ExtendedResult.ServicePoint(
                        s.slot().servicePoint(),
                        placeFinder.findInAddress(s.slot().servicePoint().addressText(), s.voivodeship()),
                        s.voivodeship()
                    )
                )
            )
            .collect(
                Collectors.groupingBy(
                    k -> k.servicePoint().voivodeship(),
                    Collectors.groupingBy(
                        k -> k.servicePoint().place(),
                        Collectors.groupingBy(ExtendedResult.Slot::vaccineType)
                    )
                )
            );

        for (Voivodeship voivodeship : groupByVoi.keySet()) {
            var voiFile = Paths.get(outputDirectory, "%s.md.tmp".formatted(voivodeship.urlName()));
            Files.writeString(voiFile, """
                            ---
                            layout: page
                            title: %s
                            permalink: /%s
                            ---
                            """.formatted(voivodeship.readable(), voivodeship.urlName()),
                StandardOpenOption.APPEND, StandardOpenOption.CREATE
            );
            for (String place: groupByVoi.get(voivodeship).keySet()) {
                Files.writeString(voiFile, """
                                           ---
                                           # %s
                                        
                                           """.formatted(place),
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE
                );
                for (VaccineType vaccineType : groupByVoi.get(voivodeship).get(place).keySet()) {
                    List<ExtendedResult.Slot> slots = groupByVoi.get(voivodeship).get(place).get(vaccineType);
                    Files.writeString(voiFile, """
                        #### %s
                        ```
                        %s
                        ```
                        
                        """.formatted(
                        vaccineType.readable(),
                        slots.stream()
                            .map(
                                i -> "%s | %s, %s".formatted(
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(
                                        LocalDateTime.ofInstant(i.startAt(), ZoneId.of("Europe/Warsaw"))
                                    ),
                                    i.servicePoint().addressText(), place)
                            )
                            .collect(Collectors.joining("\n"))
                        ),
                        StandardOpenOption.APPEND, StandardOpenOption.CREATE
                    );
                }
            }
        }
    }

}
