package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TableFormatter {
    private static final Logger LOG = LogManager.getLogger(TableFormatter.class);
    public static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final int LARGE_SLOT_START = 4;
    private final String outputDirectory;
    private final ServicePointFinder servicePointFinder;
    private final List<Main.SearchCity> searchCities;
    private final Instant now;
    private final PlaceFinder placeFinder;

    private final Map<UUID, String> phoneCorrections = Map.ofEntries(
        Map.entry(UUID.fromString("b0408af7-d22b-45af-9aab-0dfc61816483"), "124248600"),
        Map.entry(UUID.fromString("7760b351-dcd8-4919-b2de-745b9219f9f1"), "517177267"),
        Map.entry(UUID.fromString("6b416e04-cd63-4164-b74b-7a5da39aee4e"), "616771011"),
        Map.entry(UUID.fromString("5e5d3118-2bab-466d-8546-649d4dc11471"), ""), // bo nie chcą odbierać, kierują na 989
        Map.entry(UUID.fromString("7c6bccd4-8a99-4b47-8a1b-eb3ef6f33258"), "123797167 123797115"),
        Map.entry(UUID.fromString("d199b3c1-d47c-45c2-be41-34d1133f404c"), "587270505"), // (potem wewnętrzny 4), Dębowa 21, Gdańsk
        Map.entry(UUID.fromString("98d02de7-2c97-48ba-a0dd-2586bac96146"), "222990354"), //
        Map.entry(UUID.fromString("3e91cfb4-9d77-40e9-bdce-dacdc14b71b6"), "664067606"), // Szpitalna 2, Oborniki
        Map.entry(UUID.fromString("48324f72-a003-438a-90a1-b5f4c887f2de"), "507816804 503893600"), // Broniewskiego 14
        Map.entry(UUID.fromString("22cd6a69-a441-40fc-b974-053a92a785ff"), "124003305 124003306"),
        Map.entry(UUID.fromString("81a97b5e-6159-4844-b1d9-c81e7f6d644e"), "690694186"),
        Map.entry(UUID.fromString("966e6b13-d82f-452c-9bec-53b284f9f83f"), "126372791"), // Podchorążych 3, Kraków
        Map.entry(UUID.fromString("c841eda9-ee36-46f6-8332-f451727edd26"), "123491500") // Lema 7, Kraków
    );

    private final Map<UUID, Coordinates> coordsCorrections = Map.of(
    );

    public TableFormatter(String outputDirectory, ObjectMapper mapper,
        List<Main.SearchCity> searchCities, Instant now, PlaceFinder placeFinder) {
        this.searchCities = searchCities;
        this.outputDirectory = outputDirectory;
        this.servicePointFinder = new ServicePointFinder(mapper, phoneCorrections);
        this.now = now;
        this.placeFinder = placeFinder;
    }

    public void store(Set<Main.SlotWithVoivodeship> results)
        throws IOException {
        //        Map<Voivodeship, List<ExtendedResult.Slot>> groupByVoi = results.stream()

        List<ExtendedResult.Slot> sorted = results.stream()
            .map(this::toSlot)
            .sorted(
                Comparator.comparing((ExtendedResult.Slot s) -> s.servicePoint().place())
                    .thenComparing(ExtendedResult.Slot::vaccineType)
                    .thenComparing(ExtendedResult.Slot::startAt)
            ).toList();

//        Map<Voivodeship, Map<LocalDate, Map<ExtendedResult.ServicePoint, List<ExtendedResult.Slot>>>> groupByVoi =
        Map<Voivodeship, Map<LocalDate, Map<VaccineType, Map<ExtendedResult.ServicePoint, List<ExtendedResult.Slot>>>>>
            groupByVoi = sorted.stream()
            .collect(
                Collectors.groupingBy(
                    k -> k.servicePoint().voivodeship(),
                    Collectors.groupingBy(
                        k -> LocalDate.ofInstant(k.startAt(), ZONE),
                        Collectors.groupingBy(
                            ExtendedResult.Slot::vaccineType,
                            Collectors.groupingBy(ExtendedResult.Slot::servicePoint)
                        )
                    )
                )
            );

        for (Map.Entry<Voivodeship, Map<LocalDate, Map<VaccineType, Map<ExtendedResult.ServicePoint, List<ExtendedResult.Slot>>>>> entry : groupByVoi
            .entrySet()) {
            Voivodeship voivodeship = entry.getKey();
            var voiFile = Paths.get(outputDirectory, "%s.html".formatted(voivodeship.urlName()));
            if (voiFile.toFile().exists()) {
                voiFile.toFile().delete();
            }
            ZonedDateTime nextRun = calculateNextRun();
            Files.writeString(voiFile, """
                ---
                layout: page
                title: %s
                permalink: /%s
                ---
                <p>Ostatnia aktualizacja: <strong><time class="timeago" datetime="%s">%s</time></strong>,
                kolejna: <strong>~ <time id="nexttime" class="timeago" datetime="%s">%s</time></strong></p>
                <p>
                <small>Wyszukujemy w miastach: %s</small>.
                </p>
                <table id="szczepienia" class="stripe" data-lat="%s" data-lon="%s">
                    <thead>
                        <tr>
                            <th>Miasto</th>
                            <th>Data</th>
                            <th>Godz.</th>
                            <th>Rodzaj</th>
                            <th>Adres</th>
                            <th>Umów</th>
                        </tr>
                    </thead>
                    <tbody>
                                    
                """.formatted(
                    voivodeship.readable(), voivodeship.urlName(),
                    now.atZone(ZONE).format(DateTimeFormatter.ISO_INSTANT),
                    now.atZone(ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    nextRun.format(DateTimeFormatter.ISO_INSTANT),
                    nextRun.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    searchCities.stream()
                        .filter(c -> c.voivodeship() == voivodeship)
                        .map(Main.SearchCity::name)
                        .collect(Collectors.joining(", ")),
                    voivodeship.cords().lat(), voivodeship.cords().lon()
                ),
                StandardOpenOption.CREATE_NEW
            );

            for (Map.Entry<LocalDate, Map<VaccineType, Map<ExtendedResult.ServicePoint, List<ExtendedResult.Slot>>>> mapEntry : entry.getValue()
                .entrySet()) {
                LocalDate date = mapEntry.getKey();
                for (Map.Entry<VaccineType, Map<ExtendedResult.ServicePoint, List<ExtendedResult.Slot>>> typeMapEntry : mapEntry
                    .getValue().entrySet()) {

                    for (Map.Entry<ExtendedResult.ServicePoint, List<ExtendedResult.Slot>> listEntry : typeMapEntry.getValue()
                        .entrySet()) {
                        ExtendedResult.ServicePoint servicePoint = listEntry.getKey();
                        List<ExtendedResult.Slot> slots = listEntry.getValue();
                        ExtendedResult.Slot slot = slots.get(0);
                        List<String> slotTimes = slots.stream()
                            .map(s -> LocalTime.ofInstant(s.startAt(), ZONE))
                            .sorted()
                            .distinct()
                            .map(t -> DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pl")).format(t))
                            .toList();
                        //                        .collect(Collectors.joining("<br/>"));

                        String times;

                        // https://github.com/szczepienia/szczepienia.github.io/issues/new?labels=incorrect_phone&title=Z%C5%82y+number+telefonu+do+plac%C3%B3wki+(id=1234)
                        if (slots.size() > LARGE_SLOT_START) {
                            times = """
                                <div class="slot-count">(terminów: <strong>%d</strong>)</div>
                                <div class="toggle-times">%s</div>
                                <div class="extended-times">
                                %s
                                </div>
                                <br/>
                                """.formatted(
                                slots.size(),
                                slotTimes.get(0),
                                String.join("<br/>", slotTimes.subList(1, slotTimes.size()))
                            );
                        } else {
                            times = String.join("<br/>", slotTimes);
                        }

                        Optional<ExtendedServicePoint> maybe = servicePointFinder.findByAddress(slot.servicePoint());
                        Coordinates cords = maybe.map(e -> new Coordinates(e.lat(), e.lon()))
                            .orElse(coordsCorrections.getOrDefault(slot.servicePoint().id(), new Coordinates("", "")));

                        final String slotRow = slotRow(voivodeship, slots, slot, times, maybe, cords);
                        Files.writeString(voiFile, slotRow, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                    }
                }
            }

            Files.writeString(voiFile, """
                        <tbody>
                    </table>
                    """,
                StandardOpenOption.APPEND, StandardOpenOption.CREATE
            );
        }
    }

    private String slotRow(Voivodeship voivodeship, List<ExtendedResult.Slot> slots, ExtendedResult.Slot slot,
        String times, Optional<ExtendedServicePoint> maybe, Coordinates cords) {
        final ExtendedResult.Slot middleSlot = slots.get(slots.size() / 2);
        return """
            <tr %s %s data-lat="%s" data-lon="%s" data-service-point-id="%s" data-service-point-uuid="%s">
                <td>%s</td>
                <td data-order="%d">%s</td>
                <td class="dt-body-center times">%s</td>
                <td class="dt-body-center" data-order="%d">%s</td>
                <td class="address">
                %s
                %s
                </td>
                <td>
                    %s
                    %s
                    <a href="tel:989" title="Zadwoń na infolinię i umów się na ten termin">📞</img>&nbsp;989</a> (<a class="data-989" href="#">Dane</a>)<br/>
                    <a target="_blank" title="Skorzystaj z profilu zaufanego i umów się przez internet" href="https://pacjent.erejestracja.ezdrowie.gov.pl/wizyty">🔗</img>&nbsp;e-rejestracja</a><br/>
                </td>
            </tr>
            """.formatted(
            slots.size() > LARGE_SLOT_START ? "class=\"large-slot\"" : "",
            searchMeta(middleSlot.search(), middleSlot.startAt(), slot.servicePoint().place()),
            cords.lat(),
            cords.lon(),
            maybe.map(ExtendedServicePoint::id).map(String::valueOf).orElse(""),
            slot.servicePoint().id(),
            slot.servicePoint().place(),
            slot.startAt().getEpochSecond(),
            DateTimeFormatter.ofPattern("d MMMM;EEEE|", Locale.forLanguageTag("pl")).format(
                LocalDateTime.ofInstant(slot.startAt(), ZONE)
            )
                .replace(";", "<br/><small>")
                .replace("|", "</small>")
                .replace(" ", "&nbsp;"),
            times,
            slot.vaccineType().ordinal(),
            slot.vaccineType().readable(),
            """
                <div class="bug" style="visibility: hidden"><a href="https://github.com/szczepienia/szczepienia.github.io/issues/new?labels=incorrect_address&title=[%s]+Z%%C5%%82y+adres+plac%%C3%%B3wki+(id=%s)" title="Zgłoś błąd">Błąd?</a></div>
                """.formatted(
                URLEncoder.encode(voivodeship.name(), StandardCharsets.UTF_8),
                maybe.map(ExtendedServicePoint::id)
                    .map(String::valueOf)
                    .orElse(slot.servicePoint().id().toString())
            ),
            getAddress(slot, maybe),
            """
                <div class="bug"><a href="https://github.com/szczepienia/szczepienia.github.io/issues/new?labels=incorrect_phone&title=[%s]+Z%%C5%%82y+number+telefonu+do+plac%%C3%%B3wki+(uuid=%s)" title="Zgłoś błąd">Błąd?</a></div>
                """.formatted(
                URLEncoder.encode(voivodeship.name(), StandardCharsets.UTF_8),
                slot.servicePoint().id()
            ),
            getPhone(slot, maybe)
        );
    }

    private String searchMeta(Main.Search search, Instant startAt, String place) {
        List<String> datas = new ArrayList<>();
        datas.add("data-place=\"%s\"".formatted(place));
        datas.add("data-search-slot-time=\"%s\"".formatted(LocalTime.ofInstant(startAt, ZONE).truncatedTo(ChronoUnit.MINUTES)));
        datas.add("data-search-date-from=\"%s\"".formatted(search.dayRange().from()));
        datas.add("data-search-date-to=\"%s\"".formatted(search.dayRange().to()));
        datas.add("data-search-voivodeship=\"%s\"".formatted(search.voiId().readable()));
        datas.add(
            "data-search-vaccines=\"%s\"".formatted(
                search.vaccineTypes().stream().map(VaccineType::readable).collect(Collectors.joining(", ")))
        );
        if (search.hourRange() != null) {
            // wszystko - 0:00 - 23:59
            // 8:00 - 12:00
            // 12:00 - 16:00
            // 16:00 - 20:00
            // 20:00 - 23:59
            String from = search.hourRange().from().truncatedTo(ChronoUnit.MINUTES).toString();
            String to = search.hourRange().to().truncatedTo(ChronoUnit.MINUTES).toString();
            datas.add("data-search-hour-from=\"%s\"".formatted(from));
            datas.add("data-search-hour-to=\"%s\"".formatted(to));
        }
        if (search.geoId() != null) {
            datas.add("data-search-city=\"%s\"".formatted(search.geoId().name()));
        }
//         datas.add("data-search-service-point=\"%s\"".formatted(search.servicePointId()));
        return datas.stream().collect(Collectors.joining(" "));
    }

    private ZonedDateTime calculateNextRun() {
        ZonedDateTime nextRun;
        ZonedDateTime proposedNextRun = now.plusSeconds(Duration.ofMinutes(30).toSeconds() + Duration.ofMinutes(2).toSeconds()).atZone(ZONE);
        if (proposedNextRun.getHour() >= 2 && proposedNextRun.getHour() < 6) {
            nextRun = proposedNextRun.withHour(7).plusMinutes(30);
        } else {
            nextRun = proposedNextRun;
        } return nextRun;
    }

    private String rangeToDisplay(TimeRange r) {
        if (r.start.equals(r.end)) {
            return "<br/>%s".formatted(r.start);
        } else {
            return "<br/><hr/>%s<br/><small>↓</small><br/>%s".formatted(r.start(), r.end());
        }
    }

    public static List<TimeRange> getRanges(List<ExtendedResult.Slot> slots, ExtendedResult.Slot slot) {
        List<TimeRange> ranges = new ArrayList<>();
        int lastRange = -1;
        final List<LocalTime> times = slots.stream()
            .map(s -> LocalTime.ofInstant(s.startAt(), ZONE))
            .sorted()
            .distinct()
            .toList();
        int duration = slot.duration();
//        int duration = guessDuration(slot, times);
        for (LocalTime time : times) {
            if (lastRange == -1) {
                ranges.add(new TimeRange(time, time));
                lastRange = 0;
            } else {
                TimeRange last = ranges.get(lastRange);
                if (last.end().plusMinutes(duration).equals(time)) {
                    ranges.set(lastRange, new TimeRange(last.start(), time));
                } else {
                    ranges.add(new TimeRange(time, time));
                    lastRange++;
                }
            }
        }
        return ranges;
    }

    private static int guessDuration(ExtendedResult.Slot slot, List<LocalTime> times) {
        int duration = slot.duration();
        final int diff = times.get(1).getMinute() - times.get(0).getMinute();
        if (diff != duration) {
            duration = diff;
        }
        return duration;
    }

    private String getPhone(ExtendedResult.Slot slot, Optional<ExtendedServicePoint> maybe) {
        String dirtyPhone;
        if (maybe.isEmpty() || maybe.get().telephone() == null || maybe.get().telephone().isBlank()) {
            dirtyPhone = phoneCorrections.getOrDefault(slot.servicePoint().id(), "");
        } else {
            ExtendedServicePoint found = maybe.get();
            dirtyPhone = found.telephone();
        }
        dirtyPhone = dirtyPhone.trim();
        List<String> phoneList;
        if (dirtyPhone.contains("/")) {
            phoneList = Arrays.asList(dirtyPhone.split("/"));
        } else if (dirtyPhone.contains(",")) {
            phoneList = Arrays.asList(dirtyPhone.split(","));
        } else if (dirtyPhone.length() > 9 && dirtyPhone.indexOf(' ') == 9) {
            phoneList = Arrays.asList(dirtyPhone.split(" "));
        } else if (dirtyPhone.isBlank()) {
            phoneList = List.of();
        } else {
            phoneList = List.of(dirtyPhone);
        }

        List<String> shuffledPhoneList = new ArrayList<>(phoneList);
        Collections.shuffle(shuffledPhoneList);
        return shuffledPhoneList.stream()
            .map(this::cleanupPhone)
            .filter(Predicate.not(String::isBlank))
            .map(p -> """
                <a href="tel:+48%s" title="Zadzwoń do punktu szczepień">📞<strong>&nbsp;%s</strong></a><br/>
                """.formatted(p.replace(" ", ""), p.replace(" ", "&nbsp;")))
            .collect(Collectors.joining());
    }

    private String cleanupPhone(String phone) {
        String dirtyPhone = phone.replace(" ","").replace("-", "");
        if (dirtyPhone.length() == 11 && dirtyPhone.startsWith("48")) {
            dirtyPhone = dirtyPhone.substring(2);
        }
        if (dirtyPhone.startsWith("+48")) {
            dirtyPhone = dirtyPhone.substring(3);
        }
        if (dirtyPhone.length() == 9 && !dirtyPhone.contains(" ")) {
            phone = "%s %s %s".formatted(
                dirtyPhone.substring(0, 3),
                dirtyPhone.substring(3, 6),
                dirtyPhone.substring(6)
            );
        } else {
            phone = dirtyPhone;
        }
        return phone;
    }

    private String getAddress(ExtendedResult.Slot slot, Optional<ExtendedServicePoint> maybe) {
        final String address = "<div class=\"point-description\">%s</div><div class=\"point-address\">%s</div>".formatted(
            slot.servicePoint().name(),
            slot.servicePoint().addressText()
        );
        if (maybe.isEmpty()) {
            return """
                <a target="_blank" href="https://www.google.com/maps/search/?api=1&query=%s,%s">%s</a>
                """.formatted(
                URLEncoder.encode(slot.servicePoint().addressText(), StandardCharsets.UTF_8),
                URLEncoder.encode(slot.servicePoint().place(), StandardCharsets.UTF_8),
                address
            );
        } else {
            ExtendedServicePoint found = maybe.get();
            return """
                <a target="_blank" href="https://www.google.com/maps/search/?api=1&query=%s,%s">%s</a>
                """.formatted(found.lat(), found.lon(), address);
        }
    }

    private ExtendedResult.Slot toSlot(Main.SlotWithVoivodeship slot1) {
        PlaceFinder.RealNamePlace address =
            placeFinder.findInAddress(slot1.slot().servicePoint().addressText(), slot1.voivodeship());
        return new ExtendedResult.Slot(
            slot1.slot(),
            new ExtendedResult.ServicePoint(
                slot1.slot().servicePoint(),
                address.name(),
                slot1.voivodeship(),
                address.simc()
            )
        );
    }

    public static record TimeRange(LocalTime start, LocalTime end) {
    }
}
