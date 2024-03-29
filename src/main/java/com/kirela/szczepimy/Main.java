package com.kirela.szczepimy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import java.time.Duration;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    record SlotWithVoivodeship(BasicSlotWithSearch slot, Voivodeship voivodeship) {};

    private static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36";
    private static final String USER_AGENT = "User-Agent";

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        long start = System.currentTimeMillis();

        Options options = CommandLine.populateCommand(new Options(), args);
        if (options.usageHelpRequested) {
            CommandLine.usage(options, System.out);
            return;
        }
        LOG.info("Options used: {}", options);

        GminaFinder gminaFinder = new GminaFinder();
        PlaceFinder placeFinder = new PlaceFinder();

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
//            new SearchCity("Lubin", Voivodeship.DOLNOŚLĄSKIE),
//            new SearchCity("Wałbrzych", Voivodeship.DOLNOŚLĄSKIE),
//            new SearchCity("Głogów", Voivodeship.DOLNOŚLĄSKIE),
//            new SearchCity("Kłodzko", Voivodeship.DOLNOŚLĄSKIE),
//            new SearchCity("Świdnica", Voivodeship.DOLNOŚLĄSKIE),
//            new SearchCity("Bolesławiec", Voivodeship.DOLNOŚLĄSKIE),
//            new SearchCity("Dzierżoniów", Voivodeship.DOLNOŚLĄSKIE),
//            new SearchCity("Legnica", Voivodeship.DOLNOŚLĄSKIE),


            new SearchCity("Bydgoszcz", Voivodeship.KUJAWSKO_POMORSKIE, WANTED_TRIES),
            new SearchCity("Toruń", Voivodeship.KUJAWSKO_POMORSKIE, WANTED_TRIES),
//            new SearchCity("Inowrocław", Voivodeship.KUJAWSKO_POMORSKIE),
//            new SearchCity("Grudziądz", Voivodeship.KUJAWSKO_POMORSKIE),
//            new SearchCity("Włocławek", Voivodeship.KUJAWSKO_POMORSKIE),
//            new SearchCity("Żnin", Voivodeship.KUJAWSKO_POMORSKIE),
//            new SearchCity("Brodnica", Voivodeship.KUJAWSKO_POMORSKIE),
//            new SearchCity("Ciechocinek", Voivodeship.KUJAWSKO_POMORSKIE),
//            new SearchCity("Wąbrzeźno", Voivodeship.KUJAWSKO_POMORSKIE),
//            new SearchCity("Golub-Dobrzyń", Voivodeship.KUJAWSKO_POMORSKIE),
//            new SearchCity("Świecie", Voivodeship.KUJAWSKO_POMORSKIE),
//            new SearchCity("Kcynia", Voivodeship.KUJAWSKO_POMORSKIE),


            new SearchCity("Lublin", Voivodeship.LUBELSKIE, WANTED_TRIES),
            new SearchCity("Zamość", Voivodeship.LUBELSKIE),
//            new SearchCity("Biała Podlaska", Voivodeship.LUBELSKIE),
//            new SearchCity("Chełm", Voivodeship.LUBELSKIE),
//            new SearchCity("Puławy", Voivodeship.LUBELSKIE),
//            new SearchCity("Łuków", Voivodeship.LUBELSKIE),
//            new SearchCity("Kraśnik", Voivodeship.LUBELSKIE),
//            new SearchCity("Świdnik", Voivodeship.LUBELSKIE),
//            new SearchCity("Biłgoraj", Voivodeship.LUBELSKIE),
//            new SearchCity("Łuków", Voivodeship.LUBELSKIE),
//            new SearchCity("Łęczna", Voivodeship.LUBELSKIE),
//            new SearchCity("Hrubieszów", Voivodeship.LUBELSKIE),
//            new SearchCity("Radzyń Podlaski", Voivodeship.LUBELSKIE),


            new SearchCity("Zielona Góra", Voivodeship.LUBUSKIE, WANTED_TRIES),
            new SearchCity("Gorzów Wielkopolski", Voivodeship.LUBUSKIE, WANTED_TRIES),
//            new SearchCity("Nowa Sól", Voivodeship.LUBUSKIE),
//            new SearchCity("Świebodzin", Voivodeship.LUBUSKIE),
            //            new SearchCity("Żary", Voivodeship.LUBUSKIE),
//            new SearchCity("Kostrzyn nad Odrą", Voivodeship.LUBUSKIE),
//            new SearchCity("Gubin", Voivodeship.LUBUSKIE),
//            new SearchCity("Krosno Odrzańskie", Voivodeship.LUBUSKIE),
//            new SearchCity("Lubsko", Voivodeship.LUBUSKIE),


            new SearchCity("Łódź", Voivodeship.ŁÓDZKIE, WANTED_TRIES),
            new SearchCity("Piotrków Trybunalski", Voivodeship.ŁÓDZKIE),
//            new SearchCity("Łowicz", Voivodeship.ŁÓDZKIE),
//            new SearchCity("Skierniewice", Voivodeship.ŁÓDZKIE),
//            new SearchCity("Kutno", Voivodeship.ŁÓDZKIE),
//            new SearchCity("Pabianice", Voivodeship.ŁÓDZKIE),
//            new SearchCity("Zduńska Wola", Voivodeship.ŁÓDZKIE),
//            new SearchCity("Łęczyca", Voivodeship.ŁÓDZKIE),
//            new SearchCity("Zgierz", Voivodeship.ŁÓDZKIE),
//            new SearchCity("Radomsko", Voivodeship.ŁÓDZKIE),
//            new SearchCity("Tomaszów Mazowiecki", Voivodeship.ŁÓDZKIE),
//            new SearchCity("Wieluń", Voivodeship.ŁÓDZKIE),


            new SearchCity("Kraków", Voivodeship.MAŁOPOLSKIE, WANTED_TRIES),
            new SearchCity("Tarnów", Voivodeship.MAŁOPOLSKIE),
            new SearchCity("Nowy Targ", Voivodeship.MAŁOPOLSKIE),
//            new SearchCity("Chrzanów", Voivodeship.MAŁOPOLSKIE),
//            new SearchCity("Nowy Sącz", Voivodeship.MAŁOPOLSKIE),
//            new SearchCity("Wieliczka", Voivodeship.MAŁOPOLSKIE),
//            new SearchCity("Olkusz", Voivodeship.MAŁOPOLSKIE),
//            new SearchCity("Oświęcim", Voivodeship.MAŁOPOLSKIE),
//            new SearchCity("Limanowa", Voivodeship.MAŁOPOLSKIE),
//            new SearchCity("Miechów", Voivodeship.MAŁOPOLSKIE),
//            new SearchCity("Myślenice", Voivodeship.MAŁOPOLSKIE),

            new SearchCity("Warszawa", Voivodeship.MAZOWIECKIE, WANTED_TRIES),
            new SearchCity("Radom", Voivodeship.MAZOWIECKIE),
            new SearchCity("Płock", Voivodeship.MAZOWIECKIE),
//            new SearchCity("Ostrołęka", Voivodeship.MAZOWIECKIE),
//            new SearchCity("Siedlce", Voivodeship.MAZOWIECKIE),
//            new SearchCity("Wyszków", Voivodeship.MAZOWIECKIE),
//            new SearchCity("Płońsk", Voivodeship.MAZOWIECKIE),
//            new SearchCity("Nowy Dwór Mazowiecki", Voivodeship.MAZOWIECKIE),
//            new SearchCity("Mława", Voivodeship.MAZOWIECKIE),
//            new SearchCity("Ciechanów", Voivodeship.MAZOWIECKIE),
//            new SearchCity("Józefów", Voivodeship.MAZOWIECKIE),
//            new SearchCity("Mińsk Mazowiecki", Voivodeship.MAZOWIECKIE),
//            new SearchCity("Kozienice", Voivodeship.MAZOWIECKIE),


            new SearchCity("Opole", Voivodeship.OPOLSKIE, WANTED_TRIES),
            new SearchCity("Kędzierzyn-Koźle", Voivodeship.OPOLSKIE),
//            new SearchCity("Kluczbork", Voivodeship.OPOLSKIE),
//            new SearchCity("Krapkowice", Voivodeship.OPOLSKIE),
//            new SearchCity("Nysa", Voivodeship.OPOLSKIE),
//            new SearchCity("Kluczbork", Voivodeship.OPOLSKIE),
//            new SearchCity("Namysłów", Voivodeship.OPOLSKIE),
//            new SearchCity("Brzeg", Voivodeship.OPOLSKIE),
//            new SearchCity("Strzelce Opolskie", Voivodeship.OPOLSKIE),


            new SearchCity("Białystok", Voivodeship.PODLASKIE, WANTED_TRIES),
            new SearchCity("Białystok", Voivodeship.PODLASKIE, 1, UUID.fromString("48324f72-a003-438a-90a1-b5f4c887f2de")), // Broniewskiego 14
            new SearchCity("Łomża", Voivodeship.PODLASKIE),
//            new SearchCity("Suwałki", Voivodeship.PODLASKIE),
//            new SearchCity("Grajewo", Voivodeship.PODLASKIE),
//            new SearchCity("Wysokie Mazowieckie", Voivodeship.PODLASKIE),
//            new SearchCity("Augustów", Voivodeship.PODLASKIE),
//            new SearchCity("Zambrów", Voivodeship.PODLASKIE),
//            new SearchCity("Bielsk Podlaski", Voivodeship.PODLASKIE),
//            new SearchCity("Hajnówka", Voivodeship.PODLASKIE),


            new SearchCity("Rzeszów", Voivodeship.PODKARPACKIE, WANTED_TRIES),
            new SearchCity("Rzeszów", Voivodeship.PODKARPACKIE, 1, UUID.fromString("7760b351-dcd8-4919-b2de-745b9219f9f1")), // Graniczna 4b/2b
            new SearchCity("Mielec", Voivodeship.PODKARPACKIE),
            new SearchCity("Krosno", Voivodeship.PODKARPACKIE),
//            new SearchCity("Przemyśl", Voivodeship.PODKARPACKIE),
//            new SearchCity("Jasło", Voivodeship.PODKARPACKIE),
//            new SearchCity("Tarnobrzeg", Voivodeship.PODKARPACKIE),
//            new SearchCity("Boguchwała", Voivodeship.PODKARPACKIE),
//            new SearchCity("Ropczyce", Voivodeship.PODKARPACKIE),
//            new SearchCity("Wielopole Skrzyńskie", Voivodeship.PODKARPACKIE),
//            new SearchCity("Stalowa Wola", Voivodeship.PODKARPACKIE),
//            new SearchCity("Jarosław", Voivodeship.PODKARPACKIE),
//            new SearchCity("Sanok", Voivodeship.PODKARPACKIE),
//            new SearchCity("Łańcut", Voivodeship.PODKARPACKIE),



            new SearchCity("Gdańsk", Voivodeship.POMORSKIE, WANTED_TRIES),
            new SearchCity("Gdynia", Voivodeship.POMORSKIE, WANTED_TRIES),
//            new SearchCity("Słupsk", Voivodeship.POMORSKIE),
//            new SearchCity("Chojnice", Voivodeship.POMORSKIE),
//            new SearchCity("Wejherowo", Voivodeship.POMORSKIE),
//            new SearchCity("Hel", Voivodeship.POMORSKIE),
//            new SearchCity("Bytów", Voivodeship.POMORSKIE),
//            new SearchCity("Starogard Gdański", Voivodeship.POMORSKIE),
//            new SearchCity("Tczew", Voivodeship.POMORSKIE),
//            new SearchCity("Kościerzyna", Voivodeship.POMORSKIE),


            new SearchCity("Katowice", Voivodeship.ŚLĄSKIE, WANTED_TRIES),
            new SearchCity("Bielsko-Biała", Voivodeship.ŚLĄSKIE),
            new SearchCity("Częstochowa", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Ruda Śląska", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Zabrze", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Gliwice", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Sosnowiec", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Bytom", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Chorzów", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Jaworzno", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Pszczyna", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Tarnowskie Góry", Voivodeship.ŚLĄSKIE),
            new SearchCity("Tychy", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Mikołów", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Rybnik", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Dąbrowa Górnicza", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Wodzisław Śląski", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Mysłowice", Voivodeship.ŚLĄSKIE),
//            new SearchCity("Żory", Voivodeship.ŚLĄSKIE),

            new SearchCity("Kielce", Voivodeship.ŚWIĘTOKRZYSKIE, WANTED_TRIES),
            new SearchCity("Ostrowiec Świętokrzyski", Voivodeship.ŚWIĘTOKRZYSKIE),
//            new SearchCity("Skarżysko-Kamienna", Voivodeship.ŚWIĘTOKRZYSKIE),
//            new SearchCity("Jędrzejów", Voivodeship.ŚWIĘTOKRZYSKIE),
//            new SearchCity("Sandomierz", Voivodeship.ŚWIĘTOKRZYSKIE),
//            new SearchCity("Pińczów", Voivodeship.ŚWIĘTOKRZYSKIE),
//            new SearchCity("Końskie", Voivodeship.ŚWIĘTOKRZYSKIE),
//            new SearchCity("Opatów", Voivodeship.ŚWIĘTOKRZYSKIE),
//            new SearchCity("Starachowice", Voivodeship.ŚWIĘTOKRZYSKIE),


            new SearchCity("Olsztyn", Voivodeship.WARMIŃSKO_MAZURSKIE, WANTED_TRIES),
            new SearchCity("Elbląg", Voivodeship.WARMIŃSKO_MAZURSKIE),
//            new SearchCity("Ełk", Voivodeship.WARMIŃSKO_MAZURSKIE),
//            new SearchCity("Szczytno", Voivodeship.WARMIŃSKO_MAZURSKIE),
//            new SearchCity("Giżycko", Voivodeship.WARMIŃSKO_MAZURSKIE),
//            new SearchCity("Iława", Voivodeship.WARMIŃSKO_MAZURSKIE),
//            new SearchCity("Lidzbark Warmiński", Voivodeship.WARMIŃSKO_MAZURSKIE),
//            new SearchCity("Mrągowo", Voivodeship.WARMIŃSKO_MAZURSKIE),
//            new SearchCity("Nidzica", Voivodeship.WARMIŃSKO_MAZURSKIE),

            new SearchCity("Poznań", Voivodeship.WIELKOPOLSKIE, WANTED_TRIES),
            new SearchCity("Konin", Voivodeship.WIELKOPOLSKIE),
//            new SearchCity("Gniezno", Voivodeship.WIELKOPOLSKIE),
//            new SearchCity("Piła", Voivodeship.WIELKOPOLSKIE),
//            new SearchCity("Kalisz", Voivodeship.WIELKOPOLSKIE),
//            new SearchCity("Leszno", Voivodeship.WIELKOPOLSKIE),
//            new SearchCity("Pleszew", Voivodeship.WIELKOPOLSKIE),
//            new SearchCity("Krotoszyn", Voivodeship.WIELKOPOLSKIE),
//            new SearchCity("Gostyń", Voivodeship.WIELKOPOLSKIE),

            new SearchCity("Szczecin", Voivodeship.ZACHODNIOPOMORSKIE, WANTED_TRIES),
            new SearchCity("Koszalin", Voivodeship.ZACHODNIOPOMORSKIE)
//            new SearchCity("Szczecinek", Voivodeship.ZACHODNIOPOMORSKIE)
//            new SearchCity("Kołobrzeg", Voivodeship.ZACHODNIOPOMORSKIE),
//            new SearchCity("Stargard", Voivodeship.ZACHODNIOPOMORSKIE),
//            new SearchCity("Świnoujście", Voivodeship.ZACHODNIOPOMORSKIE),
//            new SearchCity("Białogard", Voivodeship.ZACHODNIOPOMORSKIE),
//            new SearchCity("Choszczno", Voivodeship.ZACHODNIOPOMORSKIE),
//            new SearchCity("Police", Voivodeship.ZACHODNIOPOMORSKIE)
        );
        LocalDateTime startDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endDate = startDate.plusWeeks(3).withHour(23).withMinute(59);
        Deque<SearchWithoutPrescription> input = new ConcurrentLinkedDeque<>(
            Stream.concat(findVoi.stream(), find.stream())
                .filter(s -> options.voivodeships.contains(s.voivodeship()))
                .flatMap(
                    s -> Arrays.stream(VaccineType.values()).map(
                        v -> new SearchWithoutPrescription(
                            new DateRange(startDate.toLocalDate(), endDate.toLocalDate()),
                            new TimeRange(
                                startDate.toLocalTime(),
                                endDate.toLocalTime()
                            ),
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
        AtomicInteger searchCount = new AtomicInteger(0);
        AtomicInteger retryCount = new AtomicInteger(0);
        Duration waitTime = Duration.ofMillis(6600);

        Queue<BasicSlotWithSearch> output = new ConcurrentLinkedQueue<>();
        start = System.currentTimeMillis();

        record SchedulerLatch(Creds creds, ScheduledExecutorService exec, CountDownLatch latch) {};
        List<SchedulerLatch> tasks = options.credentials.stream()
            .map(c -> new SchedulerLatch(c, Executors.newScheduledThreadPool(4), new CountDownLatch(1)))
            .toList();
        tasks.forEach(t ->
            t.exec().scheduleAtFixedRate(() -> queueSearch(t.creds(), client, mapper, input, output, searchCount, endDate, t.latch(), retryCount), new Random().nextInt((int) waitTime.toMillis()), waitTime.toMillis(), TimeUnit.MILLISECONDS)
        );

        tasks.forEach(
            t -> {
                try {
                    t.latch().await();
                    t.exec().shutdown();
                    boolean result = t.exec().awaitTermination(waitTime.toMillis(), TimeUnit.MILLISECONDS);
                    if (!result) {
                        LOG.error("Executor did not stop correctly");
                    }
                    t.exec().shutdownNow();
                } catch (InterruptedException e) {
                    LOG.error("Task interrupted", e);
                }
            }
        );
        LOG.info("*********** Finished search ***********");
        final long searchTime = System.currentTimeMillis() - start;
        STATS.info("Waited for search: {}", searchTime);

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
            //telegram("Error in formatter (%s)".formatted(ex.getMessage()), options.botKey, options.chatId);
        }
    }

    private static LocalDateTime startDateFromLastFoundDate(LocalDateTime endDate, Set<BasicSlotWithSearch> list) {
        LocalDateTime startDate;
        startDate = list.stream()
            .map(BasicSlotWithSearch::startAt)
            .map(s -> s.atZone(ZoneId.of("Europe/Warsaw")))
            .map(ZonedDateTime::toLocalDateTime)
            //.map(s -> s.plusDays(1))  // better?
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
                        "https://pacjent.erejestracja.ezdrowie.gov.pl/api/v2/calendarSlots/find"))
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
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    private static HttpRequest.Builder requestBuilder(Creds creds) {
        return HttpRequest.newBuilder()
            .header(USER_AGENT, CHROME)
            .header("Accept", "application/json, text/plain, */*")
//            .header("Accept-Encoding", "gzip") // doesn't help?
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

    private static void telegram(String msg, String botKey, String chatId) {
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
        Queue<SearchWithoutPrescription> input, Queue<BasicSlotWithSearch> output, AtomicInteger searchCount, LocalDateTime endDate,
        CountDownLatch end, AtomicInteger retryCount) {
        LOG.info("{} | Starting search...", creds.prescriptionId().substring(0, 2));
        SearchWithoutPrescription searchWithoutPrescription = input.poll();
        if (searchWithoutPrescription == null) {
            end.countDown();
            LOG.info("{} | ... finishing search, no more data", creds.prescriptionId().substring(0, 2));
            return;
        }
        try {
            Search search = searchWithoutPrescription.toSearch(creds.prescriptionId());
            String searchStr = mapper.writeValueAsString(search);
            Set<BasicSlotWithSearch> result = testWebSearch(creds, client, searchStr)
                .map(r -> convertSearchResult(mapper, search, r))
                .orElse(Set.of());
            LOG.info("{} | Found {} results", creds.prescriptionId().substring(0, 2), result.size());
            output.addAll(result);

            if (!result.isEmpty() && !unwantedVaccines(search.vaccineTypes()) && search.tries() < search.maxTries()) {
                LOG.info("{} | Doing {} retry", creds.prescriptionId().substring(0, 2), search.tries() + 1);
                LocalDateTime newStartDate = startDateFromLastFoundDate(endDate, result);
                input.offer(
                    new SearchWithoutPrescription(
                        new DateRange(newStartDate.toLocalDate(), search.dayRange().to()),
                        new TimeRange(
                            newStartDate.toLocalTime(),
                            search.hourRange().to()
                        ),
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
            LOG.warn("{} | Rate limit, readding search", creds.prescriptionId().substring(0, 2));
            input.offer(searchWithoutPrescription);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
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
            LOG.error("Problem deserializing from '{}'", body, e);
            return Set.of();
        }
    }

    private static Optional<String> testWebSearch(Creds creds, HttpClient client, String searchStr) {
        long now = System.currentTimeMillis();
        try {
            HttpResponse<String> out = client.send(
                requestBuilder(creds).uri(URI.create(
                    "https://pacjent.erejestracja.ezdrowie.gov.pl/api/v2/calendarSlots/find"))
                    .POST(HttpRequest.BodyPublishers.ofString(searchStr)).build(),
                MoreBodyHandlers.decoding(HttpResponse.BodyHandlers.ofString())
            );
            STATS.info("{} | time per search: {}, size: {}", creds.prescriptionId().substring(0, 2), System.currentTimeMillis() - now, out.body().length());
            if (out.body().contains("errorCode")) {
                if (out.body().contains("ERR_RATE_LIMITED")) {
//                    LOG.warn("*** rate limit ***");
                    throw new RateLimitException();
                } else {
                    LOG.error("{} | other error: {}", creds.prescriptionId().substring(0, 2), out.body());
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
