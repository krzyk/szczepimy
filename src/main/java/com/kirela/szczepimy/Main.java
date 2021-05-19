package com.kirela.szczepimy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.mizosoft.methanol.MoreBodyHandlers;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);
    private static final Logger STATS = LogManager.getLogger("STATS");
    private static final int WANTED_TRIES = 13;
    // https://www.gov.pl/api/data/covid-vaccination-point   -> service-points.json

    // punkty adresowe z https://eteryt.stat.gov.pl/eTeryt/rejestr_teryt/udostepnianie_danych/baza_teryt/uzytkownicy_indywidualni/pobieranie/pliki_pelne.aspx?contrast=default
    // albo z https://pacjent.erejestracja.ezdrowie.gov.pl/teryt/api/woj/06/simc?namePrefix=Krak


    // https://pacjent.erejestracja.ezdrowie.gov.pl/api/servicePoints/find
    // "{"voiID":"12","geoID":"1261011"}

//    public static record FoundAppointment(String id, LocalDateTime date, Specialization specialization, Clinic clinic, String  doctor) implements Comparable<FoundAppointment> {
//        @Override
//        public int compareTo(FoundAppointment other) {
//            return date.compareTo(other.date);
//        }
//    }


    record SlotWithVoivodeship(BasicSlotWithSearch slot, Voivodeship voivodeship) {};

    private static record Creds(String csrf, String sid, String prescriptionId) {}

    private static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36";
    private static final String USER_AGENT = "User-Agent";

    public static void main(String[] args) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();

        Options options = CommandLine.populateCommand(new Options(), args);
        if (options.usageHelpRequested) {
            CommandLine.usage(options, System.out);
            return;
        }
        LOG.info("Options used: {}", options);

        GminaFinder gminaFinder = new GminaFinder();
        PlaceFinder placeFinder = new PlaceFinder();
//        System.out.println(gminaFinder.find("Rzeszów", Voivodeship.PODKARPACKIE));

        var creds = new Creds(options.csrf, options.sid, options.prescriptionId);
        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

        ObjectMapper mapper = getMapper();

        List<SearchCity> findVoi = Arrays.stream(Voivodeship.values())
            .map(v -> new SearchCity(null, v, WANTED_TRIES))
            .toList();

        List<SearchCity> find = List.of(
            new SearchCity("Wrocław", Voivodeship.DOLNOŚLĄSKIE, WANTED_TRIES),
            new SearchCity("Jelenia Góra", Voivodeship.DOLNOŚLĄSKIE),
            new SearchCity("Lubin", Voivodeship.DOLNOŚLĄSKIE),
            new SearchCity("Wałbrzych", Voivodeship.DOLNOŚLĄSKIE),
            new SearchCity("Głogów", Voivodeship.DOLNOŚLĄSKIE),

            new SearchCity("Bydgoszcz", Voivodeship.KUJAWSKO_POMORSKIE, WANTED_TRIES),
            new SearchCity("Toruń", Voivodeship.KUJAWSKO_POMORSKIE, WANTED_TRIES),
            new SearchCity("Inowrocław", Voivodeship.KUJAWSKO_POMORSKIE),
            new SearchCity("Grudziądz", Voivodeship.KUJAWSKO_POMORSKIE),
            new SearchCity("Włocławek", Voivodeship.KUJAWSKO_POMORSKIE),
            new SearchCity("Żnin", Voivodeship.KUJAWSKO_POMORSKIE),

            new SearchCity("Kcynia", Voivodeship.KUJAWSKO_POMORSKIE),


            new SearchCity("Lublin", Voivodeship.LUBELSKIE, WANTED_TRIES),
            new SearchCity("Zamość", Voivodeship.LUBELSKIE),
            new SearchCity("Biała Podlaska", Voivodeship.LUBELSKIE),
            new SearchCity("Chełm", Voivodeship.LUBELSKIE),
            new SearchCity("Puławy", Voivodeship.LUBELSKIE),
            new SearchCity("Łuków", Voivodeship.LUBELSKIE),
            new SearchCity("Kraśnik", Voivodeship.LUBELSKIE),
            new SearchCity("Świdnik", Voivodeship.LUBELSKIE),
            new SearchCity("Biłgoraj", Voivodeship.LUBELSKIE),
            new SearchCity("Łęczna", Voivodeship.LUBELSKIE),


            new SearchCity("Zielona Góra", Voivodeship.LUBUSKIE, WANTED_TRIES),
            new SearchCity("Gorzów Wielkopolski", Voivodeship.LUBUSKIE, WANTED_TRIES),
            new SearchCity("Nowa Sól", Voivodeship.LUBUSKIE),
            new SearchCity("Świebodzin", Voivodeship.LUBUSKIE),
            //            new SearchCity("Żary", Voivodeship.LUBUSKIE),


            new SearchCity("Łódź", Voivodeship.ŁÓDZKIE, WANTED_TRIES),
            new SearchCity("Piotrków Trybunalski", Voivodeship.ŁÓDZKIE),
            new SearchCity("Łowicz", Voivodeship.ŁÓDZKIE),
            new SearchCity("Skierniewice", Voivodeship.ŁÓDZKIE),
            new SearchCity("Kutno", Voivodeship.ŁÓDZKIE),
            new SearchCity("Pabianice", Voivodeship.ŁÓDZKIE),
            new SearchCity("Zduńska Wola", Voivodeship.ŁÓDZKIE),
            new SearchCity("Łęczyca", Voivodeship.ŁÓDZKIE),


            new SearchCity("Kraków", Voivodeship.MAŁOPOLSKIE, WANTED_TRIES),
            new SearchCity("Tarnów", Voivodeship.MAŁOPOLSKIE),
            new SearchCity("Nowy Targ", Voivodeship.MAŁOPOLSKIE),
            new SearchCity("Chrzanów", Voivodeship.MAŁOPOLSKIE),
            new SearchCity("Nowy Sącz", Voivodeship.MAŁOPOLSKIE),
            new SearchCity("Wieliczka", Voivodeship.MAŁOPOLSKIE),
            new SearchCity("Olkusz", Voivodeship.MAŁOPOLSKIE),

            new SearchCity("Warszawa", Voivodeship.MAZOWIECKIE, WANTED_TRIES),
            new SearchCity("Radom", Voivodeship.MAZOWIECKIE),
            new SearchCity("Płock", Voivodeship.MAZOWIECKIE),
            new SearchCity("Ostrołęka", Voivodeship.MAZOWIECKIE),
            new SearchCity("Siedlce", Voivodeship.MAZOWIECKIE),
            new SearchCity("Wyszków", Voivodeship.MAZOWIECKIE),
            new SearchCity("Płońsk", Voivodeship.MAZOWIECKIE),
            new SearchCity("Nowy Dwór Mazowiecki", Voivodeship.MAZOWIECKIE),


            new SearchCity("Opole", Voivodeship.OPOLSKIE, WANTED_TRIES),
            new SearchCity("Kędzierzyn-Koźle", Voivodeship.OPOLSKIE),
            new SearchCity("Kluczbork", Voivodeship.OPOLSKIE),
            new SearchCity("Krapkowice", Voivodeship.OPOLSKIE),
            new SearchCity("Nysa", Voivodeship.OPOLSKIE),


            new SearchCity("Białystok", Voivodeship.PODLASKIE, WANTED_TRIES),
            new SearchCity("Białystok", Voivodeship.PODLASKIE, 1, UUID.fromString("48324f72-a003-438a-90a1-b5f4c887f2de")), // Broniewskiego 14
            new SearchCity("Łomża", Voivodeship.PODLASKIE),
            new SearchCity("Suwałki", Voivodeship.PODLASKIE),
            new SearchCity("Grajewo", Voivodeship.PODLASKIE),
            new SearchCity("Wysokie Mazowieckie", Voivodeship.PODLASKIE),


            new SearchCity("Rzeszów", Voivodeship.PODKARPACKIE, WANTED_TRIES),
            new SearchCity("Rzeszów", Voivodeship.PODKARPACKIE, 1, UUID.fromString("7760b351-dcd8-4919-b2de-745b9219f9f1")), // Graniczna 4b/2b
            new SearchCity("Mielec", Voivodeship.PODKARPACKIE),
            new SearchCity("Krosno", Voivodeship.PODKARPACKIE),
            new SearchCity("Przemyśl", Voivodeship.PODKARPACKIE),
            new SearchCity("Jasło", Voivodeship.PODKARPACKIE),
            new SearchCity("Tarnobrzeg", Voivodeship.PODKARPACKIE),
            new SearchCity("Boguchwała", Voivodeship.PODKARPACKIE),
            new SearchCity("Ropczyce", Voivodeship.PODKARPACKIE),
            new SearchCity("Wielopole Skrzyńskie", Voivodeship.PODKARPACKIE),



            new SearchCity("Gdańsk", Voivodeship.POMORSKIE, WANTED_TRIES),
            new SearchCity("Gdynia", Voivodeship.POMORSKIE, WANTED_TRIES),
            new SearchCity("Słupsk", Voivodeship.POMORSKIE),
            new SearchCity("Chojnice", Voivodeship.POMORSKIE),
            new SearchCity("Wejherowo", Voivodeship.POMORSKIE),
            new SearchCity("Hel", Voivodeship.POMORSKIE),


            new SearchCity("Katowice", Voivodeship.ŚLĄSKIE, WANTED_TRIES),
            new SearchCity("Bielsko-Biała", Voivodeship.ŚLĄSKIE),
            new SearchCity("Częstochowa", Voivodeship.ŚLĄSKIE),
            new SearchCity("Ruda Śląska", Voivodeship.ŚLĄSKIE),
            new SearchCity("Zabrze", Voivodeship.ŚLĄSKIE),
            new SearchCity("Gliwice", Voivodeship.ŚLĄSKIE),
            new SearchCity("Sosnowiec", Voivodeship.ŚLĄSKIE),
            new SearchCity("Bytom", Voivodeship.ŚLĄSKIE),
            new SearchCity("Chorzów", Voivodeship.ŚLĄSKIE),
            new SearchCity("Jaworzno", Voivodeship.ŚLĄSKIE),
            new SearchCity("Pszczyna", Voivodeship.ŚLĄSKIE),
            new SearchCity("Tarnowskie Góry", Voivodeship.ŚLĄSKIE),
            new SearchCity("Tychy", Voivodeship.ŚLĄSKIE),
            new SearchCity("Mikołów", Voivodeship.ŚLĄSKIE),

            new SearchCity("Kielce", Voivodeship.ŚWIĘTOKRZYSKIE, WANTED_TRIES),
            new SearchCity("Ostrowiec Świętokrzyski", Voivodeship.ŚWIĘTOKRZYSKIE),
            new SearchCity("Skarżysko-Kamienna", Voivodeship.ŚWIĘTOKRZYSKIE),
            new SearchCity("Jędrzejów", Voivodeship.ŚWIĘTOKRZYSKIE),
            new SearchCity("Sandomierz", Voivodeship.ŚWIĘTOKRZYSKIE),


            new SearchCity("Olsztyn", Voivodeship.WARMIŃSKO_MAZURSKIE, WANTED_TRIES),
            new SearchCity("Elbląg", Voivodeship.WARMIŃSKO_MAZURSKIE),
            new SearchCity("Ełk", Voivodeship.WARMIŃSKO_MAZURSKIE),
            new SearchCity("Szczytno", Voivodeship.WARMIŃSKO_MAZURSKIE),
            new SearchCity("Giżycko", Voivodeship.WARMIŃSKO_MAZURSKIE),

            new SearchCity("Poznań", Voivodeship.WIELKOPOLSKIE, WANTED_TRIES),
            new SearchCity("Konin", Voivodeship.WIELKOPOLSKIE),
            new SearchCity("Gniezno", Voivodeship.WIELKOPOLSKIE),
            new SearchCity("Piła", Voivodeship.WIELKOPOLSKIE),
            new SearchCity("Kalisz", Voivodeship.WIELKOPOLSKIE),

            new SearchCity("Szczecin", Voivodeship.ZACHODNIOPOMORSKIE, WANTED_TRIES),
            new SearchCity("Koszalin", Voivodeship.ZACHODNIOPOMORSKIE),
            new SearchCity("Szczecinek", Voivodeship.ZACHODNIOPOMORSKIE),
            new SearchCity("Kołobrzeg", Voivodeship.ZACHODNIOPOMORSKIE),
            new SearchCity("Stargard", Voivodeship.ZACHODNIOPOMORSKIE)
        );
        LocalDateTime startDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endDate = startDate.plusWeeks(4).withHour(23).withMinute(59);
        Deque<Search> input = new ConcurrentLinkedDeque<>(
            Stream.concat(findVoi.stream(), find.stream())
                .filter(s -> options.voivodeships.contains(s.voivodeship()))
                .flatMap(
                    s -> Arrays.stream(VaccineType.values()).map(
                        v -> new Search(
                            new DateRange(startDate.toLocalDate(), endDate.toLocalDate()),
                            new TimeRange(
                                startDate.toLocalTime(),
                                endDate.toLocalTime()
                            ),
                            creds.prescriptionId(),
                            List.of(v),
                            s.voivodeship(),
                            Optional.ofNullable(s.name())
                                .map(n -> gminaFinder.find(n, s.voivodeship))
                                .orElse(null),
                            s.servicePointId(),
                            null,
                            0, s.tries()
                        )
                    )
                ).toList()
        );

        STATS.info("Preparation time: {}", System.currentTimeMillis() - start);
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(10);
        AtomicInteger searchCount = new AtomicInteger(0);
        AtomicInteger retryCount = new AtomicInteger(0);
        Queue<BasicSlotWithSearch> output = new ConcurrentLinkedQueue<>();
        CountDownLatch end = new CountDownLatch(1);
        exec.scheduleAtFixedRate(() -> queueSearch(creds, client, mapper, input, output, searchCount, endDate, end, retryCount), 0, 1075, TimeUnit.MILLISECONDS);

        start = System.currentTimeMillis();
        end.await();
        LOG.info("*********** Finished search ***********");
        exec.shutdown();
        final long searchTime = System.currentTimeMillis() - start;
        STATS.info("Waited for search: {}", searchTime);

//        try {
//            start = System.currentTimeMillis();
//            long lastSearchTime = 0;
//            for (SearchCity searchCity : Stream.concat(findVoi.stream(), find.stream())
//                .filter(s -> options.voivodeships.contains(s.voivodeship()))
//                .toList()) {
//                LOG.info("Processing {}", searchCity);
//                Optional<Gmina> gmina = Optional.ofNullable(searchCity.name())
//                    .map(n -> gminaFinder.find(n, searchCity.voivodeship));
//                for (List<VaccineType> vaccines : vaccineSets(options)) {
//                    LocalDateTime startDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
//                    final LocalDateTime endDate = startDate.plusWeeks(4).withHour(23).withMinute(59);
////                    final LocalDateTime endDate = LocalDateTime.of(2021, 5, 31, 23, 59);
//                    int tries = 0;
//                    while (startDate.isBefore(endDate)) {
//                        LOG.info("city={}, vaccine={}: try={}, start={}, end={}, pointId={}", searchCity.name(), vaccines, tries, startDate, endDate, searchCity.servicePointId());
//                        var search = new Search(
//                            new DateRange(startDate.toLocalDate(), endDate.toLocalDate()),
//                            new TimeRange(
//                                startDate.toLocalTime(),
//                                endDate.toLocalTime()
//                            ),
//                            creds.prescriptionId(),
//                            vaccines,
//                            searchCity.voivodeship(),
//                            gmina.orElse(null),
//                            searchCity.servicePointId(),
//                            null
//                        );
//                        searchCount++;
//                        long now = System.currentTimeMillis();
//                        LOG.info("time since last search = {}", now - lastSearchTime);
//                        if (now - lastSearchTime < options.wait) {
//                            Thread.sleep(options.wait - (now - lastSearchTime));
//                        }
//                        lastSearchTime = System.currentTimeMillis();
//                        String body = webSearch(options, creds, client, mapper, search);
//
//                        final Set<BasicSlotWithSearch> list = Optional.ofNullable(mapper.readValue(body, Result.class).list()).orElse(List.of()).stream()
//                            .map(s -> new BasicSlotWithSearch(s, search)).collect(Collectors.toSet());
//
////                        startDate = startDateFromMissingDates(startDate, endDate, list);
//                        startDate = startDateFromLastFoundDate(endDate, list);
//                        LOG.info("Found for {}, {}: {} slots, nextDate = {}", searchCity.name, vaccines, list.size(), startDate);
//
//                        final Set<SlotWithVoivodeship> lastResults = list.stream()
//                            .map(s -> new SlotWithVoivodeship(s, searchCity.voivodeship()))
//                            .collect(Collectors.toSet());
//                        results.addAll(lastResults);
//                        tries++;
//                        final int unwantedTries = 1;
//                        if (finishLoop(voiCities, searchCity, vaccines, tries, lastResults, WANTED_TRIES, unwantedTries)) {
//                            if (!unwantedVaccines(vaccines) && tries > unwantedTries && !lastResults.isEmpty()) {
//                                LOG.info(
//                                    "More data exists for {}, {}, {}, last startDate={}",
//                                    searchCity.voivodeship(),
//                                    searchCity.name(),
//                                    vaccines,
//                                    startDate
//                                );
//                            }
//                            break;
//                        }
//                    }
//                }
//            }
//            STATS.info("Search time: {}, time/search: {}", System.currentTimeMillis() - start, (System.currentTimeMillis() - start)/searchCount);
//        } catch (Exception ex) {
//            LOG.error("Exception", ex);
//            StringWriter writer = new StringWriter();
//            ex.printStackTrace(new PrintWriter(writer));
//            telegram("Prawdopodobnie wygasła sesja (%s): \n ```\n%s\n```".formatted(ex.getMessage(), writer.toString()));
//        } finally {
//            LOG.info("Search count = {}, results = {}, results/count = {}", searchCount, results.size(), results.size()/searchCount);
//        }

        try {
            start = System.currentTimeMillis();
            Set<SlotWithVoivodeship> results = output.stream()
                .map(s -> new SlotWithVoivodeship(s, s.search().voiId()))
                .collect(Collectors.toSet());
            LOG.info(
                "Search count = {}, results = {}, results/count = {}, time/results = {}, retries = {}",
                searchCount.get(), results.size(), results.size()/searchCount.get(), searchTime / searchCount.get(), retryCount.get());
            new TableFormatter(options.output, mapper, find, Instant.now(), placeFinder).store(results);
            STATS.info(
                "Formatter time: {}, time/result: {}",
                System.currentTimeMillis() - start,
                (System.currentTimeMillis() - start) / results.size()
            );
            new Stats(options.output).store(results);
        } catch (Exception ex) {
            LOG.error("Received error in formatter/stats", ex);
            telegram("Error in formatter (%s)".formatted(ex.getMessage()));
        }
    }

    private static LocalDateTime startDateFromLastFoundDate(LocalDateTime endDate, Set<BasicSlotWithSearch> list) {
        LocalDateTime startDate;
        startDate = list.stream()
            .map(BasicSlotWithSearch::startAt)
            .map(s -> s.atZone(ZoneId.of("Europe/Warsaw")))
            .map(ZonedDateTime::toLocalDateTime)
            //.map(s -> s.plusDays(1))
            .map(s -> s.plusMinutes(1))
            .distinct()
            .max(Comparator.naturalOrder())
            .orElse(endDate);
        return startDate;
    }

    private static LocalDateTime startDateFromMissingDates(LocalDateTime startDate, LocalDateTime endDate,
        Set<BasicSlotWithSearch> list) {
        List<LocalDate> allDates = Stream.iterate(startDate.toLocalDate(), d -> d.plusDays(1))
            .limit(ChronoUnit.DAYS.between(startDate, endDate))
            .toList();
        List<LocalDate> foundDates = list.stream()
            .map(BasicSlotWithSearch::startAt)
            .map(s -> s.atZone(ZoneId.of("Europe/Warsaw")))
            .map(ZonedDateTime::toLocalDate)
            .distinct()
            .toList();
        startDate = allDates.stream()
            .filter(Predicate.not(foundDates::contains))
            .map(LocalDate::atStartOfDay)
            .sorted()
            .findFirst()
            .orElse(startDate.plusDays(1));
        return startDate;
    }

    private static boolean finishLoop(Set<String> voiCities, SearchCity searchCity, List<VaccineType> vaccines,
        int tries, Set<SlotWithVoivodeship> lastResults, int wantedTries, int unwantedTries) {

        if ((unwantedVaccines(vaccines) && tries >= unwantedTries) || lastResults.isEmpty() || tries >= wantedTries  || (tries >= unwantedTries && searchCity.name() != null && !voiCities.contains(searchCity.name()))) {
            return true;
        }
        return false;
    }

    private static boolean unwantedVaccines(List<VaccineType> vaccines) {
        return vaccines.equals(List.of(VaccineType.AZ));
//        return false;
    }

    private static Set<List<VaccineType>> vaccineSets(Options options) {
//        return Set.of(
//            List.of(VaccineType.MODERNA),
//            List.of(VaccineType.JJ)
//            List.of(VaccineType.MODERNA, VaccineType.JJ),
//            List.of(VaccineType.AZ)
//            List.of(VaccineType.PFIZER),
//            List.of(VaccineType.MODERNA, VaccineType.JJ),
//            List.of(VaccineType.AZ)
//        );
        return options.vaccineTypes.stream()
            .map(List::of)
            .collect(Collectors.toSet());
    }

    private static String webSearch(Options options, Creds creds, HttpClient client, ObjectMapper mapper,
        Search search) throws IOException, InterruptedException {
        String searchStr = mapper.writeValueAsString(search);
        int retryCount = 0;
        long now = System.currentTimeMillis();
        try {
            for (int i = 0; i < options.retries; i++) {
                long searchStart = System.currentTimeMillis();
                HttpResponse<String> out = client.send(
                    requestBuilder(creds).uri(URI.create(
                        "https://pacjent.erejestracja.ezdrowie.gov.pl/api/calendarSlots/find"))
                        .POST(HttpRequest.BodyPublishers.ofString(searchStr)).build(),
                    MoreBodyHandlers.decoding(HttpResponse.BodyHandlers.ofString())
                );
                STATS.info("time per search: {}, size: {}", System.currentTimeMillis() - searchStart, out.body().length());
                if (options.storeLogs) {
                    Paths.get(options.output, "logs").toFile().mkdirs();
                    Files.writeString(
                        Paths.get(
                            options.output,
                            "logs",
                            "%s_%s.%s.json".formatted(search.geoId().name(), search.vaccineTypes(), Instant.now())
                        ),
                        out.body(),
                        StandardOpenOption.CREATE_NEW
                    );
                }
                if (out.body().contains("errorCode")) {
                    if (out.body().contains("ERR_RATE_LIMITED")) {
                        LOG.warn("retrying");
                        retryCount++;
                        Thread.sleep(300 + new Random().nextInt(300));
                        continue;
                    } else {
                        throw new IllegalArgumentException("Received error: %s".formatted(out.body()));
                    }
                }
                return out.body();
            }

            throw new IllegalArgumentException("Exceeded retry count");
        } finally {
            if (retryCount > 0) {
                LOG.error("Retries count = {}", retryCount);
            }
            STATS.info("total search time: {} (retries = {})", System.currentTimeMillis() - now, retryCount);
        }
    }

    public static ObjectMapper getMapper() {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    private static HttpRequest.Builder requestBuilder(Creds creds) {
        return HttpRequest.newBuilder()
            .header(USER_AGENT, CHROME)
            .header("Accept", "application/json, text/plain, */*")
//            .header("Accept-Encoding", "gzip")
            .header("Referer", "https://pacjent.erejestracja.ezdrowie.gov.pl/wizyty")
            .header("Cookie", "patient_sid=%s".formatted(creds.sid()))
            .header("X-Csrf-Token", creds.csrf())
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
//            .header("TE", "Trailers")  // breaks http 2
            .header("TE", "trailers")
            .header("Upgrade-Insecure-Requests", "1");
    }

    private static void telegram(String msg) {
        // curl -v "https://api.telegram.org/bot759993662:AAFeW6xrNpgSQl1OMixkkPIujxPZU4WoS7g/sendMessage?chat_id=553846308&text=hello"
        String botKey = "759993662:AAFeW6xrNpgSQl1OMixkkPIujxPZU4WoS7g";
        String chatId = "553846308";
        try {
            HttpClient.newBuilder().build().send(
                HttpRequest.newBuilder().uri(
                    URI.create(
                        String.format(
                            "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                            botKey, chatId, URLEncoder.encode(msg, StandardCharsets.UTF_8)
                        )
                    )
                ).GET().build(),
                HttpResponse.BodyHandlers.discarding()
            );
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    record SearchCity(String name, Voivodeship voivodeship, int tries, Set<VaccineType> vaccines, UUID servicePointId) {
        public SearchCity(String name, Voivodeship voivodeship, int maxTries) {
            this(name, voivodeship, maxTries, Arrays.stream(VaccineType.values()).collect(Collectors.toSet()), null);
        }

        public SearchCity(String name, Voivodeship voivodeship, int tries, UUID servicePointId) {
            this(name, voivodeship, tries, Arrays.stream(VaccineType.values()).collect(Collectors.toSet()), servicePointId);
        }
        public SearchCity(String name, Voivodeship voivodeship, int tries, Set<VaccineType> vaccines) {
            this(name, voivodeship, tries, vaccines, null);
        }
        public SearchCity(String name, Voivodeship voivodeship) {
            this(name, voivodeship, 1, Arrays.stream(VaccineType.values()).collect(Collectors.toSet()), null);
        }
    }

    private static void queueSearch(Creds creds, HttpClient client, ObjectMapper mapper,
        Queue<Search> input, Queue<BasicSlotWithSearch> output, AtomicInteger searchCount, LocalDateTime endDate,
        CountDownLatch end, AtomicInteger retryCount) {
        LOG.info("Starting search...");
        Search search = input.poll();
        if (search == null) {
            end.countDown();
            LOG.info("... finishing search, no more data");
            return;
        }
        try {
            String searchStr;
            try {
                searchStr = mapper.writeValueAsString(search);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
            Set<BasicSlotWithSearch> result = testWebSearch(creds, client, searchStr)
                .map(r -> convertSearchResult(mapper, search, r))
                .orElse(Set.of());
            LOG.info("Found {} results", result.size());
            output.addAll(result);

            if (!result.isEmpty() && !unwantedVaccines(search.vaccineTypes()) && search.tries() < search.maxTries()) {
                LOG.info("Doing {} retry", search.tries());
                LocalDateTime newStartDate = startDateFromLastFoundDate(endDate, result);
                input.offer(
                    new Search(
                        new DateRange(newStartDate.toLocalDate(), search.dayRange().to()),
                        new TimeRange(
                            newStartDate.toLocalTime(),
                            search.hourRange().to()
                        ),
                        search.prescriptionId(),
                        search.vaccineTypes(),
                        search.voiId(),
                        search.geoId(),
                        search.servicePointId(),
                        search.mobilities(),
                        search.tries() + 1,
                        search.maxTries()
                    )
                );
            }
        } catch (RateLimitException e) {
            // try another time
            retryCount.incrementAndGet();
            LOG.warn("Rate limit, readding search");
            input.offer(search);
        } finally {
            searchCount.incrementAndGet();
        }
    }

    private static Set<BasicSlotWithSearch> convertSearchResult(ObjectMapper mapper, Search search, String body) {
        try {
            return Optional.ofNullable(
                mapper.readValue(body, Result.class).list()
            ).orElse(List.of())
                .stream()
                .map(s -> new BasicSlotWithSearch(s, search)).collect(Collectors.toSet());
        } catch (JsonProcessingException e) {
            LOG.error("Problem deserializing");
            return Set.of();
        }
    }

    private static Optional<String> testWebSearch(Creds creds, HttpClient client, String searchStr) {
        long now = System.currentTimeMillis();
        try {
            HttpResponse<String> out = client.send(
                requestBuilder(creds).uri(URI.create(
                    "https://pacjent.erejestracja.ezdrowie.gov.pl/api/calendarSlots/find"))
                    .POST(HttpRequest.BodyPublishers.ofString(searchStr)).build(),
                MoreBodyHandlers.decoding(HttpResponse.BodyHandlers.ofString())
            );
            STATS.info("time per search: {}, size: {}", System.currentTimeMillis() - now, out.body().length());
            if (out.body().contains("errorCode")) {
                if (out.body().contains("ERR_RATE_LIMITED")) {
//                    LOG.warn("*** rate limit ***");
                    throw new RateLimitException();
                } else {
                    LOG.error("other error: {}", out.body());
                }
                return Optional.empty();
            }
            return Optional.of(out.body());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
//            STATS.info("total search time: {}", System.currentTimeMillis() - now);
        }
    }
}
