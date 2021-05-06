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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private final String outputDirectory;
    private final ServicePointFinder servicePointFinder;
    private final List<Main.SearchCity> searchCities;
    private final Instant now;

    private final Map<UUID, String> phoneCorrections = Map.of(
        UUID.fromString("48324f72-a003-438a-90a1-b5f4c887f2de"), "507816804 503893600", // Broniewskiego 14
        UUID.fromString("b0408af7-d22b-45af-9aab-0dfc61816483"), "124003305, 124003306",
        UUID.fromString("22cd6a69-a441-40fc-b974-053a92a785ff"), "124003305, 124003306",
        UUID.fromString("81a97b5e-6159-4844-b1d9-c81e7f6d644e"), "690694186"
    );

    private final Map<UUID, Coordinates> coordsCorrections = Map.of(
    );

    public TableFormatter(String outputDirectory, ObjectMapper mapper,
        List<Main.SearchCity> searchCities, Instant now) {
        this.searchCities = searchCities;
        this.outputDirectory = outputDirectory;
        this.servicePointFinder = new ServicePointFinder(mapper);
        this.now = now;
    }

    public void store(PlaceFinder placeFinder, Set<Main.SlotWithVoivodeship> results)
        throws IOException {
        //        Map<Voivodeship, List<ExtendedResult.Slot>> groupByVoi = results.stream()

        Map<Voivodeship, Map<LocalDate, Map<ExtendedResult.ServicePoint, List<ExtendedResult.Slot>>>> groupByVoi =
            results.stream()
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
                    Collectors.groupingBy(
                        k -> k.servicePoint().voivodeship(),
                        Collectors.groupingBy(
                            k -> LocalDate.ofInstant(k.startAt(), ZONE),
                            Collectors.groupingBy(ExtendedResult.Slot::servicePoint)
                        )
                    )
                );

        for (Map.Entry<Voivodeship, Map<LocalDate, Map<ExtendedResult.ServicePoint, List<ExtendedResult.Slot>>>> entry : groupByVoi
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

            for (Map.Entry<LocalDate, Map<ExtendedResult.ServicePoint, List<ExtendedResult.Slot>>> mapEntry : entry.getValue()
                .entrySet()) {
                LocalDate date = mapEntry.getKey();
                for (Map.Entry<ExtendedResult.ServicePoint, List<ExtendedResult.Slot>> listEntry : mapEntry.getValue()
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
                    if (slots.size() >= 4) {
//                        List<TimeRange> ranges = getRanges(slots, slot);
//                        List<TimeRange> incorrect = ranges.stream()
//                            .filter(r -> r.start() == r.end())
//                            .toList();
//                        if (!incorrect.isEmpty()) {
//                            LOG.error("Ranges are incorrect (%s) input: %s".formatted(incorrect, slots));
//                        }
//                        times = """
//                                <small class="smaller">(liczba = %d)</small>
//                                <small class="smaller">(co&nbsp;%s&nbsp;min)</small>
//                                %s
//                                <br/>
//                                """.formatted(
//                            slots.size(),
//                            slot.duration(),
//                            ranges.stream()
//                                .map(this::rangeToDisplay)
//                                .collect(Collectors.joining())

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

                    Files.writeString(voiFile, """
                                    <tr data-lat="%s" data-lon="%s" data-service-point-id="%s" data-service-point-uuid="%s">
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
                                            <a href="tel:989" title="Zadwoń na infolinię i umów się na ten termin">📞</img>&nbsp;989</a><br/>
                                            <a target="_blank" title="Skorzystaj z profilu zaufanego i umów się przez internet" href="https://pacjent.erejestracja.ezdrowie.gov.pl/wizyty">🔗</img>&nbsp;e-rejestracja</a><br/>
                                        </td>
                                    </tr>
                                                
                            """.formatted(
                        cords.lat(), cords.lon(),
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

                        //                    slot.startAt().getEpochSecond(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(
                        //                        LocalDateTime.ofInstant(slot.startAt(), ZoneId.of("Europe/Warsaw"))
                        //                    ),
                        slot.vaccineType().ordinal(),
                        slot.vaccineType().readable(),
                        """
                            <div class="bug" style="visibility: hidden"><a href="https://github.com/szczepienia/szczepienia.github.io/issues/new?labels=incorrect_address&title=[%s]+Z%%C5%%82y+adres+plac%%C3%%B3wki+(id=%s)" title="Zgłoś błąd">Zgłoś</a></div>
                            """.formatted(URLEncoder.encode(voivodeship.name(), StandardCharsets.UTF_8), maybe.map(ExtendedServicePoint::id).map(String::valueOf).orElse(slot.servicePoint().id().toString())),
                        getAddress(slot, maybe),
                        """
                            <div class="bug"><a href="https://github.com/szczepienia/szczepienia.github.io/issues/new?labels=incorrect_phone&title=[%s]+Z%%C5%%82y+number+telefonu+do+plac%%C3%%B3wki+(id=%s)" title="Zgłoś błąd">Zgłoś</a></div>
                            """.formatted(URLEncoder.encode(voivodeship.name(), StandardCharsets.UTF_8), maybe.map(ExtendedServicePoint::id).map(String::valueOf).orElse(slot.servicePoint().id().toString())),
                        getPhone(slot, maybe)
                        ),
                        StandardOpenOption.APPEND, StandardOpenOption.CREATE
                    );
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

    private ZonedDateTime calculateNextRun() {
        ZonedDateTime nextRun;
        ZonedDateTime proposedNextRun = now.plusSeconds(Duration.ofMinutes(30).toSeconds() + Duration.ofMinutes(6).toSeconds()).atZone(ZONE);
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

    public static record TimeRange(LocalTime start, LocalTime end) {
    }
}
