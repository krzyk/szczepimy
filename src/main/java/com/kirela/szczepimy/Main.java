package com.kirela.szczepimy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);
    private static final Logger STATS = LogManager.getLogger("STATS");
    // https://www.gov.pl/api/data/covid-vaccination-point   -> service-points.json

    // punkty adresowe z https://eteryt.stat.gov.pl/eTeryt/rejestr_teryt/udostepnianie_danych/baza_teryt/uzytkownicy_indywidualni/pobieranie/pliki_pelne.aspx?contrast=default
    // albo z https://pacjent.erejestracja.ezdrowie.gov.pl/teryt/api/woj/06/simc?namePrefix=Krak


    // https://pacjent.erejestracja.ezdrowie.gov.pl/api/servicePoints/find
    // "{"voiID":"12","geoID":"1261011"}
    private static final ExecutorService exec = Executors.newFixedThreadPool(1);

//    public static record FoundAppointment(String id, LocalDateTime date, Specialization specialization, Clinic clinic, String  doctor) implements Comparable<FoundAppointment> {
//        @Override
//        public int compareTo(FoundAppointment other) {
//            return date.compareTo(other.date);
//        }
//    }


    record SlotWithVoivodeship(BasicSlotWithSearch slot, Voivodeship voivodeship) {};
    public static record DateRange(LocalDate from, LocalDate to) {}

    public static record TimeRange(
        @JsonSerialize(using = LocalTimeSerializer.class)
        LocalTime from,
        @JsonSerialize(using = LocalTimeSerializer.class)
        LocalTime to
    ) {}

    private static record Creds(String csrf, String sid, String prescriptionId) {}

    public static record Search(
        DateRange dayRange,
        TimeRange hourRange,
        String prescriptionId, List<VaccineType> vaccineTypes, Voivodeship voiId, Gmina geoId, UUID servicePointId,
        List<String> mobilities
    ) {
        public Search withNewDateRange(DateRange newDateRange) {
            return new Search(
                newDateRange,
                hourRange,
                prescriptionId,
                vaccineTypes,
                voiId,
                geoId,
                servicePointId,
                mobilities
            );
        }
    }

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
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

        ObjectMapper mapper = getMapper();

        //        mapper.setDefaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.AS_EMPTY));

//        for (City city : Set.of(City.KRAKÓW, City.RZESZÓW)) {

        Set<String> voiCities = Set.of(
            "Wrocław",
            "Bydgoszcz",
            "Toruń",
            "Lublin",
            "Zielona Góra",
            "Gorzów Wielkopolski",
            "Łódź",
            "Kraków",
            "Warszawa",
            "Opole",
            "Białystok",
            "Rzeszów",
            "Gdańsk",
            "Gdynia",
            "Katowice",
            "Kielce",
            "Olsztyn",
            "Poznań",
            "Szczecin"
        );
        List<SearchCity> findVoi = Arrays.stream(Voivodeship.values())
            .map(v -> new SearchCity(null, v, 10))
            .toList();

//        List<SearchCity> findVoi = List.of();
        List<SearchCity> find = List.of(
//            new SearchCity("Rzeszów", Voivodeship.PODKARPACKIE, 7),
//            new SearchCity("Mielec", Voivodeship.PODKARPACKIE, 7),
//            new SearchCity("Tarnobrzeg", Voivodeship.PODKARPACKIE, 7),
//            new SearchCity("Przemyśl", Voivodeship.PODKARPACKIE, 7),
//            new SearchCity("Krosno", Voivodeship.PODKARPACKIE, 7),
//            new SearchCity("Kraków", Voivodeship.MAŁOPOLSKIE, 7),
//            new SearchCity("Tarnów", Voivodeship.MAŁOPOLSKIE, 28),
//            new SearchCity("Nowy Sącz", Voivodeship.MAŁOPOLSKIE, 55),
//            new SearchCity("Wadowice", Voivodeship.MAŁOPOLSKIE, 55),
//            new SearchCity("Rabka-Zdrój", Voivodeship.MAŁOPOLSKIE, 55),
//            new SearchCity("Krzeszowice", Voivodeship.MAŁOPOLSKIE, 55),
//            new SearchCity("Warszawa", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Radom", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Ciechanów", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Piaseczno", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Konstancin-Jeziorna", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Solec nad Wisłą", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Mińsk Mazowiecki", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Otwock", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Serock", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Legionowo", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Pułtusk", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Lublin", Voivodeship.LUBELSKIE, 7),
//            new SearchCity("Zamość", Voivodeship.LUBELSKIE, 7),
//            new SearchCity("Świdnik", Voivodeship.LUBELSKIE, 7),
//            new SearchCity("Łuków", Voivodeship.LUBELSKIE, 7),
//            new SearchCity("Biłgoraj", Voivodeship.LUBELSKIE, 7),
//            new SearchCity("Szczecin", Voivodeship.ZACHODNIOPOMORSKIE, 7),
//            new SearchCity("Koszalin", Voivodeship.ZACHODNIOPOMORSKIE, 7),
//            new SearchCity("Opole", Voivodeship.OPOLSKIE, 7),
//            new SearchCity("Olesno", Voivodeship.OPOLSKIE, 7),
//            new SearchCity("Kluczbork", Voivodeship.OPOLSKIE, 7),
//            new SearchCity("Wrocław", Voivodeship.DOLNOŚLĄSKIE, 7),
//            new SearchCity("Kłodzko", Voivodeship.DOLNOŚLĄSKIE, 7),
//            new SearchCity("Katowice", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Częstochowa", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Gliwice", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Czechowice-Dziedzice", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Dąbrowa Górnicza", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Sosnowiec", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Bielsko-Biała", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Pszczyna", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Chorzów", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Kielce", Voivodeship.ŚWIĘTOKRZYSKIE, 7),
//            new SearchCity("Bydgoszcz", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Toruń", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Inowrocław", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Włocławek", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Grudziądz", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Świecie", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Wąbrzeźno", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Ciechocinek", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Kowalewo Pomorskie", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Chełmża", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Chełmno", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Zielona Góra", Voivodeship.LUBUSKIE, 7),
//            new SearchCity("Gorzów Wielkopolski", Voivodeship.LUBUSKIE, 7),
//            new SearchCity("Łódź", Voivodeship.ŁÓDZKIE, 7),
//            new SearchCity("Piotrków Trybunalski", Voivodeship.ŁÓDZKIE, 7),
//            new SearchCity("Białystok", Voivodeship.PODLASKIE, 7),
//            new SearchCity("Łomża", Voivodeship.PODLASKIE, 7),
//            new SearchCity("Jedwabne", Voivodeship.PODLASKIE, 7),
//            new SearchCity("Piątnica", Voivodeship.PODLASKIE, 7),
//            new SearchCity("Gdańsk", Voivodeship.POMORSKIE, 7),
//            new SearchCity("Sopot", Voivodeship.POMORSKIE, 7),
//            new SearchCity("Gdynia", Voivodeship.POMORSKIE, 7),
//            new SearchCity("Tczew", Voivodeship.POMORSKIE, 7),
//            new SearchCity("Olsztyn", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
//            new SearchCity("Elbląg", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
//            new SearchCity("Dobre Miasto", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
//            new SearchCity("Giżycko", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
//            new SearchCity("Poznań", Voivodeship.WIELKOPOLSKIE, 7),
//            new SearchCity("Jarocin", Voivodeship.WIELKOPOLSKIE, 55),
//            new SearchCity("Ostrów Wielkopolski", Voivodeship.WIELKOPOLSKIE, 55),
//            new SearchCity("Oborniki", Voivodeship.WIELKOPOLSKIE, 7),
//            new SearchCity("Gniezno", Voivodeship.WIELKOPOLSKIE, 7),
//            new SearchCity("Kalisz", Voivodeship.WIELKOPOLSKIE, 28)
//        );

            //            new SearchCity("Piaseczno", Voivodeship.MAZOWIECKIE, 7),
            //            new SearchCity("Konstancin-Jeziorna", Voivodeship.MAZOWIECKIE, 7),
            //            new SearchCity("Solec nad Wisłą", Voivodeship.MAZOWIECKIE, 7),
            //            new SearchCity("Świdnik", Voivodeship.LUBELSKIE, 7),
            //            new SearchCity("Łuków", Voivodeship.LUBELSKIE, 7),
            //            new SearchCity("Biłgoraj", Voivodeship.LUBELSKIE, 7),
            //            new SearchCity("Czechowice-Dziedzice", Voivodeship.ŚLĄSKIE, 7),
            //            new SearchCity("Inowrocław", Voivodeship.KUJAWSKO_POMORSKIE, 7),
            //            new SearchCity("Kowalewo Pomorskie", Voivodeship.KUJAWSKO_POMORSKIE, 7),
            //            new SearchCity("Chełmża", Voivodeship.KUJAWSKO_POMORSKIE, 7),
            //            new SearchCity("Chełmno", Voivodeship.KUJAWSKO_POMORSKIE, 7),
            //            new SearchCity("Sopot", Voivodeship.POMORSKIE, 7),
            //            new SearchCity("Dobre Miasto", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
            //            new SearchCity("Oborniki", Voivodeship.WIELKOPOLSKIE, 7),
            //            new SearchCity("Świecie", Voivodeship.KUJAWSKO_POMORSKIE, 7),
            //            new SearchCity("Wąbrzeźno", Voivodeship.KUJAWSKO_POMORSKIE, 7),
            //            new SearchCity("Olesno", Voivodeship.OPOLSKIE, 7),
            //            new SearchCity("Pułtusk", Voivodeship.MAZOWIECKIE, 7),


            // old list:

            //
            new SearchCity("Wrocław", Voivodeship.DOLNOŚLĄSKIE, 1),
            new SearchCity("Jelenia Góra", Voivodeship.DOLNOŚLĄSKIE, 1),
            new SearchCity("Lubin", Voivodeship.DOLNOŚLĄSKIE, 1),
            new SearchCity("Wałbrzych", Voivodeship.DOLNOŚLĄSKIE, 1),
            new SearchCity("Głogów", Voivodeship.DOLNOŚLĄSKIE, 1),

            new SearchCity("Bydgoszcz", Voivodeship.KUJAWSKO_POMORSKIE, 1),
            new SearchCity("Toruń", Voivodeship.KUJAWSKO_POMORSKIE, 1),
            new SearchCity("Inowrocław", Voivodeship.KUJAWSKO_POMORSKIE, 1),
            new SearchCity("Grudziądz", Voivodeship.KUJAWSKO_POMORSKIE, 1),
            new SearchCity("Włocławek", Voivodeship.KUJAWSKO_POMORSKIE, 1),
            new SearchCity("Żnin", Voivodeship.KUJAWSKO_POMORSKIE, 1),

            new SearchCity("Kcynia", Voivodeship.KUJAWSKO_POMORSKIE, 1),


            new SearchCity("Lublin", Voivodeship.LUBELSKIE, 1),
            new SearchCity("Zamość", Voivodeship.LUBELSKIE, 1),
            new SearchCity("Biała Podlaska", Voivodeship.LUBELSKIE, 1),
            new SearchCity("Chełm", Voivodeship.LUBELSKIE, 1),
            new SearchCity("Puławy", Voivodeship.LUBELSKIE, 1),
            new SearchCity("Łuków", Voivodeship.LUBELSKIE, 1),
            new SearchCity("Kraśnik", Voivodeship.LUBELSKIE, 1),
            new SearchCity("Świdnik", Voivodeship.LUBELSKIE, 1),
            new SearchCity("Biłgoraj", Voivodeship.LUBELSKIE, 1),
            new SearchCity("Łęczna", Voivodeship.LUBELSKIE, 1),


            new SearchCity("Zielona Góra", Voivodeship.LUBUSKIE, 1),
            new SearchCity("Gorzów Wielkopolski", Voivodeship.LUBUSKIE, 1),
            new SearchCity("Nowa Sól", Voivodeship.LUBUSKIE, 1),
            new SearchCity("Świebodzin", Voivodeship.LUBUSKIE, 1),
            //            new SearchCity("Żary", Voivodeship.LUBUSKIE, 1),


            new SearchCity("Łódź", Voivodeship.ŁÓDZKIE, 1),
            new SearchCity("Piotrków Trybunalski", Voivodeship.ŁÓDZKIE, 1),
            new SearchCity("Łowicz", Voivodeship.ŁÓDZKIE, 1),
            new SearchCity("Skierniewice", Voivodeship.ŁÓDZKIE, 1),
            new SearchCity("Kutno", Voivodeship.ŁÓDZKIE, 1),
            new SearchCity("Pabianice", Voivodeship.ŁÓDZKIE, 1),
            new SearchCity("Zduńska Wola", Voivodeship.ŁÓDZKIE, 1),


            new SearchCity("Kraków", Voivodeship.MAŁOPOLSKIE, 1),
            new SearchCity("Tarnów", Voivodeship.MAŁOPOLSKIE, 28),
            new SearchCity("Nowy Targ", Voivodeship.MAŁOPOLSKIE, 55),
            new SearchCity("Chrzanów", Voivodeship.MAŁOPOLSKIE, 55),
            new SearchCity("Nowy Sącz", Voivodeship.MAŁOPOLSKIE, 55),
            new SearchCity("Wieliczka", Voivodeship.MAŁOPOLSKIE, 1),
            new SearchCity("Olkusz", Voivodeship.MAŁOPOLSKIE, 1),

            new SearchCity("Warszawa", Voivodeship.MAZOWIECKIE, 1),
            new SearchCity("Radom", Voivodeship.MAZOWIECKIE, 1),
            new SearchCity("Płock", Voivodeship.MAZOWIECKIE, 1),
            new SearchCity("Ostrołęka", Voivodeship.MAZOWIECKIE, 1),
            new SearchCity("Siedlce", Voivodeship.MAZOWIECKIE, 1),
            new SearchCity("Wyszków", Voivodeship.MAZOWIECKIE, 1),
            new SearchCity("Płońsk", Voivodeship.MAZOWIECKIE, 1),
            new SearchCity("Nowy Dwór Mazowiecki", Voivodeship.MAZOWIECKIE, 1),


            new SearchCity("Opole", Voivodeship.OPOLSKIE, 1),
            new SearchCity("Kędzierzyn-Koźle", Voivodeship.OPOLSKIE, 1),
            new SearchCity("Kluczbork", Voivodeship.OPOLSKIE, 1),
            new SearchCity("Krapkowice", Voivodeship.OPOLSKIE, 1),
            new SearchCity("Nysa", Voivodeship.OPOLSKIE, 1),


            new SearchCity("Białystok", Voivodeship.PODLASKIE, 1),
            new SearchCity("Białystok", Voivodeship.PODLASKIE, 1, UUID.fromString("48324f72-a003-438a-90a1-b5f4c887f2de")), // Broniewskiego 14
            new SearchCity("Łomża", Voivodeship.PODLASKIE, 1),
            new SearchCity("Suwałki", Voivodeship.PODLASKIE, 1),
            new SearchCity("Grajewo", Voivodeship.PODLASKIE, 1),
            new SearchCity("Wysokie Mazowieckie", Voivodeship.PODLASKIE, 1),


            new SearchCity("Rzeszów", Voivodeship.PODKARPACKIE, 1),
            new SearchCity("Rzeszów", Voivodeship.PODKARPACKIE, 1, UUID.fromString("7760b351-dcd8-4919-b2de-745b9219f9f1")), // Graniczna 4b/2b
            new SearchCity("Mielec", Voivodeship.PODKARPACKIE, 1),
            new SearchCity("Krosno", Voivodeship.PODKARPACKIE, 1),
            new SearchCity("Przemyśl", Voivodeship.PODKARPACKIE, 1),
            new SearchCity("Jasło", Voivodeship.PODKARPACKIE, 1),
            new SearchCity("Tarnobrzeg", Voivodeship.PODKARPACKIE, 1),
            new SearchCity("Boguchwała", Voivodeship.PODKARPACKIE, 1),


            new SearchCity("Gdańsk", Voivodeship.POMORSKIE, 1),
            new SearchCity("Gdynia", Voivodeship.POMORSKIE, 1),
            new SearchCity("Słupsk", Voivodeship.POMORSKIE, 1),
            new SearchCity("Chojnice", Voivodeship.POMORSKIE, 1),
            new SearchCity("Wejherowo", Voivodeship.POMORSKIE, 1),
            new SearchCity("Hel", Voivodeship.POMORSKIE, 1),


            new SearchCity("Katowice", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Bielsko-Biała", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Częstochowa", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Ruda Śląska", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Zabrze", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Gliwice", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Sosnowiec", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Bytom", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Chorzów", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Jaworzno", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Pszczyna", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Tarnowskie Góry", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Tychy", Voivodeship.ŚLĄSKIE, 1),
            new SearchCity("Mikołów", Voivodeship.ŚLĄSKIE, 1),

            new SearchCity("Kielce", Voivodeship.ŚWIĘTOKRZYSKIE, 1),
            new SearchCity("Ostrowiec Świętokrzyski", Voivodeship.ŚWIĘTOKRZYSKIE, 1),
            new SearchCity("Skarżysko-Kamienna", Voivodeship.ŚWIĘTOKRZYSKIE, 1),
            new SearchCity("Jędrzejów", Voivodeship.ŚWIĘTOKRZYSKIE, 1),
            new SearchCity("Sandomierz", Voivodeship.ŚWIĘTOKRZYSKIE, 1),


            new SearchCity("Olsztyn", Voivodeship.WARMIŃSKO_MAZURSKIE, 1),
            new SearchCity("Elbląg", Voivodeship.WARMIŃSKO_MAZURSKIE, 1),
            new SearchCity("Ełk", Voivodeship.WARMIŃSKO_MAZURSKIE, 1),
            new SearchCity("Szczytno", Voivodeship.WARMIŃSKO_MAZURSKIE, 1),
            new SearchCity("Giżycko", Voivodeship.WARMIŃSKO_MAZURSKIE, 1),

            new SearchCity("Poznań", Voivodeship.WIELKOPOLSKIE, 1),
            new SearchCity("Konin", Voivodeship.WIELKOPOLSKIE, 1),
            new SearchCity("Gniezno", Voivodeship.WIELKOPOLSKIE, 1),
            new SearchCity("Piła", Voivodeship.WIELKOPOLSKIE, 1),
            new SearchCity("Kalisz", Voivodeship.WIELKOPOLSKIE, 1),

            new SearchCity("Szczecin", Voivodeship.ZACHODNIOPOMORSKIE, 1),
            new SearchCity("Koszalin", Voivodeship.ZACHODNIOPOMORSKIE, 1),
            new SearchCity("Szczecinek", Voivodeship.ZACHODNIOPOMORSKIE, 1),
            new SearchCity("Kołobrzeg", Voivodeship.ZACHODNIOPOMORSKIE, 1),
            new SearchCity("Stargard", Voivodeship.ZACHODNIOPOMORSKIE, 1)
        );
        Set<SlotWithVoivodeship> results = new HashSet<>();


        STATS.info("Preparation time: {}", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        int searchCount = 0;
        try {
            start = System.currentTimeMillis();
            for (SearchCity searchCity : Stream.concat(findVoi.stream(), find.stream())
                .filter(s -> options.voivodeships.contains(s.voivodeship()))
                .toList()) {
                LOG.info("Processing {}", searchCity);
                Optional<Gmina> gmina = Optional.ofNullable(searchCity.name())
                    .map(n -> gminaFinder.find(n, searchCity.voivodeship));
                for (List<VaccineType> vaccines : vaccineSets(options)) {
                    LocalDateTime startDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
                    final LocalDateTime endDate = startDate.plusWeeks(4).withHour(23).withMinute(59);
//                    final LocalDateTime endDate = LocalDateTime.of(2021, 5, 31, 23, 59);
                    int tries = 0;
                    while (startDate.isBefore(endDate)) {
                        LOG.info("city={}, vaccine={}: try={}, start={}, end={}, pointId={}", searchCity.name(), vaccines, tries, startDate, endDate, searchCity.servicePointId());
                        var search = new Search(
                            new DateRange(startDate.toLocalDate(), endDate.toLocalDate()),
                            new TimeRange(
                                startDate.toLocalTime(),
                                endDate.toLocalTime()
                            ),
                            creds.prescriptionId(),
                            vaccines,
                            searchCity.voivodeship(),
                            gmina.orElse(null),
                            searchCity.servicePointId(),
                            null
                        );
                        searchCount++;
                        final Set<BasicSlotWithSearch> list =
                            webSearch(options, creds, client, mapper, searchCity, vaccines, search);
                        LOG.info("Found for {}, {}: {} slots", searchCity.name, vaccines, list.size());
                        startDate = list.stream()
                            .map(BasicSlotWithSearch::startAt)
                            .map(s -> s.atZone(ZoneId.of("Europe/Warsaw")))
                            .map(ZonedDateTime::toLocalDateTime)
                            //.map(s -> s.plusDays(1))
                            .map(s -> s.plusMinutes(1))
                            .distinct()
                            .max(Comparator.naturalOrder())
                            .orElse(endDate);

                        final Set<SlotWithVoivodeship> lastResults = list.stream()
                            .map(s -> new SlotWithVoivodeship(s, searchCity.voivodeship()))
                            .collect(Collectors.toSet());
                        results.addAll(lastResults);
                        tries++;
                        if (lastResults.isEmpty() || tries >= 10 || unwantedVaccines(vaccines) || (searchCity.name() != null && !voiCities.contains(searchCity.name()))) {
                            if (!unwantedVaccines(vaccines) && tries > 1 && !lastResults.isEmpty()) {
                                LOG.info(
                                    "More data exists for {}, {}, {}, last startDate={}",
                                    searchCity.voivodeship(),
                                    searchCity.name(),
                                    vaccines,
                                    startDate
                                );
                            }
                            break;
                        }
                    }
                }
            }
            STATS.info("Search time: {}, time/search: {}", System.currentTimeMillis() - start, (System.currentTimeMillis() - start)/searchCount);
        } catch (Exception ex) {
            LOG.error("Exception", ex);
            StringWriter writer = new StringWriter();
            ex.printStackTrace(new PrintWriter(writer));
            telegram("Prawdopodobnie wygasła sesja (%s): \n ```\n%s\n```".formatted(ex.getMessage(), writer.toString()));
        } finally {
            LOG.info("Search count = {}, results = {}, results/count = {}", searchCount, results.size(), results.size()/searchCount);
        }
        try {
            start = System.currentTimeMillis();
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

    private static boolean unwantedVaccines(List<VaccineType> vaccines) {
//        return vaccines.equals(List.of(VaccineType.AZ));
        return false;
    }

    private static Set<List<VaccineType>> vaccineSets(Options options) {
        return options.vaccineTypes.stream()
            .map(List::of)
            .collect(Collectors.toSet());
    }

    private static Set<BasicSlotWithSearch> webSearch(Options options, Creds creds, HttpClient client, ObjectMapper mapper,
        SearchCity searchCity, List<VaccineType> vaccines, Search search) throws IOException, InterruptedException {
        String searchStr = mapper.writeValueAsString(search);
        int retryCount = 0;
        Thread.sleep(options.wait);
        try {
            for (int i = 0; i < options.retries; i++) {
                HttpResponse<String> out = client.send(
                    requestBuilder(creds).uri(URI.create(
                        "https://pacjent.erejestracja.ezdrowie.gov.pl/api/calendarSlots/find"))
                        .POST(HttpRequest.BodyPublishers.ofString(searchStr)).build(),

                    HttpResponse.BodyHandlers.ofString()
                );
                if (options.storeLogs) {
                    Paths.get(options.output, "logs").toFile().mkdirs();
                    Files.writeString(
                        Paths.get(
                            options.output,
                            "logs",
                            "%s_%s.%s.json".formatted(searchCity.name(), vaccines, Instant.now())
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

                return Optional.ofNullable(mapper.readValue(out.body(), Result.class).list()).orElse(List.of()).stream()
                    .map(s -> new BasicSlotWithSearch(s, search)).collect(Collectors.toSet());
            }

            throw new IllegalArgumentException("Exceeded retry count");
        } finally {
            if (retryCount > 0) {
                LOG.error("Retries count = {}", retryCount);
            }
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
            .header("Referer", "https://pacjent.erejestracja.ezdrowie.gov.pl/wizyty")
            .header("Cookie", "patient_sid=%s".formatted(creds.sid()))
            .header("X-Csrf-Token", creds.csrf())
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
            .header("TE", "Trailers")
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

    record SearchCity(String name, Voivodeship voivodeship, int days, Set<VaccineType> vaccines, UUID servicePointId) {
        public SearchCity(String name, Voivodeship voivodeship, int days) {
            this(name, voivodeship, days, Arrays.stream(VaccineType.values()).collect(Collectors.toSet()), null);
        }

        public SearchCity(String name, Voivodeship voivodeship, int days, UUID servicePointId) {
            this(name, voivodeship, days, Arrays.stream(VaccineType.values()).collect(Collectors.toSet()), servicePointId);
        }
    }
}
