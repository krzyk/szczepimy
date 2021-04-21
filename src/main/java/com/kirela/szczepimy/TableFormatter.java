package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TableFormatter {
    private final String outputDirectory;
    private final ServicePointFinder servicePointFinder;

    public TableFormatter(String outputDirectory, ObjectMapper mapper) {
        this.outputDirectory = outputDirectory;
        this.servicePointFinder = new ServicePointFinder(mapper);
    }

    public void store(PlaceFinder placeFinder, Set<Main.SlotWithVoivodeship> results)
        throws IOException {
        Map<Voivodeship, List<ExtendedResult.Slot>> groupByVoi = results.stream()
            .map(
                s -> new ExtendedResult.Slot(
                    s.slot(),
                    new ExtendedResult.ServicePoint(
                        s.slot().servicePoint(),
                        placeFinder.findInAddress(s.slot().servicePoint().addressText(), s.voivodeship()),
                        s.voivodeship()
                    )
                )
            ).sorted(
                Comparator.comparing((ExtendedResult.Slot s) -> s.servicePoint().place())
                    .thenComparing(ExtendedResult.Slot::vaccineType)
                    .thenComparing(ExtendedResult.Slot::startAt)
            )
            .collect(
                Collectors.groupingBy(k -> k.servicePoint().voivodeship())
            );

        for (Map.Entry<Voivodeship, List<ExtendedResult.Slot>> entry : groupByVoi.entrySet()) {
            Voivodeship voivodeship = entry.getKey();
            List<ExtendedResult.Slot> slots = entry.getValue();
            var voiFile = Paths.get(outputDirectory, "%s.html".formatted(voivodeship.urlName()));
            voiFile.toFile().delete();
            Files.writeString(voiFile, """
                        ---
                        layout: page
                        title: %s
                        permalink: /%s
                        ---
                        <table id="szczepienia" class="stripe">
                            <thead>
                                <tr>
                                    <th>Miasto</th>
                                    <th>Godzina</th>
                                    <th>Rodzaj</th>
                                    <th>Adres</th>
                                    <th>Umów</th>
                                </tr>
                            </thead>
                            <tbody>
                                            
                        """.formatted(voivodeship.readable(), voivodeship.urlName()),
                StandardOpenOption.APPEND, StandardOpenOption.CREATE
            );

            for (ExtendedResult.Slot slot : slots) {
                Files.writeString(voiFile, """
                                    <tr>
                                        <td>%s</td>
                                        <td data-order="%d">%s</td>
                                        <td data-order="%d">%s</td>
                                        <td>%s</td>
                                        <td>
                                            %s
                                            <a href="tel:989" title="Zadwoń na infolinię i umów się na ten termin"><img src="assets/phone.png" width="11px"></img>&nbsp;989</a><br/>
                                            <a target="_blank" title="Skorzystaj z profilu zaufanego i umów się przez internet" href="https://pacjent.erejestracja.ezdrowie.gov.pl/wizyty"><img src="assets/url.png" width="11px"></img>&nbsp;e-rejestracja</a><br/>
                                        </td>
                                    </tr>
                                                
                            """.formatted(
                    slot.servicePoint().place(),
                    slot.startAt().getEpochSecond(), DateTimeFormatter.ofPattern("d MMMM, ;EEEE|HH:mm", Locale.forLanguageTag("pl")).format(
                        LocalDateTime.ofInstant(slot.startAt(), ZoneId.of("Europe/Warsaw"))
                    ).replace(";", "<small>")
                        .replace("|", "</small><br/>"),
//                    slot.startAt().getEpochSecond(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(
//                        LocalDateTime.ofInstant(slot.startAt(), ZoneId.of("Europe/Warsaw"))
//                    ),
                    slot.vaccineType().ordinal(), slot.vaccineType().readable(),
                    getAddress(slot),
                    getPhone(slot)
                    ),
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE
                );
            }

            Files.writeString(voiFile, """
                            <tbody>
                        </table>
                        """,
                StandardOpenOption.APPEND, StandardOpenOption.CREATE
            );
        }
    }

    private String getPhone(ExtendedResult.Slot slot) {
        Optional<ExtendedServicePoint> maybe = servicePointFinder.findByAddress(slot.servicePoint());
        if (maybe.isEmpty() || maybe.get().telephone() == null || maybe.get().telephone().isBlank()) {
            return "";
        } else {
            ExtendedServicePoint found = maybe.get();
            final String dirtyPhone = found.telephone();
            String phone;
            if (dirtyPhone.length() == 9 && !dirtyPhone.contains(" ")) {
                phone = "%s %s %s".formatted(dirtyPhone.substring(0, 3), dirtyPhone.substring(3, 6), dirtyPhone.substring(6));
            } else {
                phone = dirtyPhone;
            }
            return """
                <a href="tel:%s" title="Zadzwoń do punktu szczepień"><img src="assets/phone.png" width="11px"></img>&nbsp;%s</a><br/>
                """.formatted(found.telephone(), phone.replaceAll("/.*", "").replace(" ", "&nbsp;"));

            // TODO: need to add second phone number if "/" is used
        }
    }

    private String getAddress(ExtendedResult.Slot slot) {
        Optional<ExtendedServicePoint> maybe = servicePointFinder.findByAddress(slot.servicePoint());
        if (maybe.isEmpty()) {
            return slot.servicePoint().addressText();
        } else {
            ExtendedServicePoint found = maybe.get();
            return """
                <a target="_blank" href="https://www.google.com/maps/search/?api=1&query=%s,%s">%s</a>
                """.formatted(found.lat(), found.lon(), slot.servicePoint().addressText());
        }
    }
}
