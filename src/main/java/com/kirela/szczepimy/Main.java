package com.kirela.szczepimy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
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


    record SlotWithVoivodeship(Result.BasicSlot slot, Voivodeship voivodeship) {};
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
        String prescriptionId, List<VaccineType> vaccineTypes, Voivodeship voiId, String geoId, UUID servicePointId
    ) {
        public Search withNewDateRange(DateRange newDateRange) {
            return new Search(
                newDateRange,
                hourRange,
                prescriptionId,
                vaccineTypes,
                voiId,
                geoId,
                servicePointId
            );
        }
    }

    private static final String CHROME = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36";
    private static final String USER_AGENT = "User-Agent";

    public static void main(String[] args) throws IOException, InterruptedException {

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
//            new SearchCity("Mińsk Mazowiecki", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Otwock", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Serock", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Legionowo", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Lublin", Voivodeship.LUBELSKIE, 7),
//            new SearchCity("Zamość", Voivodeship.LUBELSKIE, 7),
//            new SearchCity("Szczecin", Voivodeship.ZACHODNIOPOMORSKIE, 7),
//            new SearchCity("Koszalin", Voivodeship.ZACHODNIOPOMORSKIE, 7),
//            new SearchCity("Opole", Voivodeship.OPOLSKIE, 7),
//            new SearchCity("Kluczbork", Voivodeship.OPOLSKIE, 7),
//            new SearchCity("Wrocław", Voivodeship.DOLNOŚLĄSKIE, 7),
//            new SearchCity("Kłodzko", Voivodeship.DOLNOŚLĄSKIE, 7),
//            new SearchCity("Katowice", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Częstochowa", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Gliwice", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Dąbrowa Górnicza", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Sosnowiec", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Bielsko-Biała", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Pszczyna", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Chorzów", Voivodeship.ŚLĄSKIE, 7),
//            new SearchCity("Kielce", Voivodeship.ŚWIĘTOKRZYSKIE, 7),
//            new SearchCity("Bydgoszcz", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Toruń", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Włocławek", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Grudziądz", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Ciechocinek", Voivodeship.KUJAWSKO_POMORSKIE, 7),
//            new SearchCity("Zielona Góra", Voivodeship.LUBUSKIE, 7),
//            new SearchCity("Gorzów Wielkopolski", Voivodeship.LUBUSKIE, 7),
//            new SearchCity("Łódź", Voivodeship.ŁÓDZKIE, 7),
//            new SearchCity("Piotrków Trybunalski", Voivodeship.ŁÓDZKIE, 7),
//            new SearchCity("Białystok", Voivodeship.PODLASKIE, 7),
//            new SearchCity("Łomża", Voivodeship.PODLASKIE, 7),
//            new SearchCity("Jedwabne", Voivodeship.PODLASKIE, 7),
//            new SearchCity("Piątnica", Voivodeship.PODLASKIE, 7),
//            new SearchCity("Gdańsk", Voivodeship.POMORSKIE, 7),
//            new SearchCity("Gdynia", Voivodeship.POMORSKIE, 7),
//            new SearchCity("Tczew", Voivodeship.POMORSKIE, 7),
//            new SearchCity("Olsztyn", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
//            new SearchCity("Elbląg", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
//            new SearchCity("Giżycko", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
//            new SearchCity("Poznań", Voivodeship.WIELKOPOLSKIE, 7),
//            new SearchCity("Jarocin", Voivodeship.WIELKOPOLSKIE, 55),
//            new SearchCity("Ostrów Wielkopolski", Voivodeship.WIELKOPOLSKIE, 55),
//            new SearchCity("Gniezno", Voivodeship.WIELKOPOLSKIE, 7),
//            new SearchCity("Kalisz", Voivodeship.WIELKOPOLSKIE, 28)

            //
            new SearchCity("Wrocław", Voivodeship.DOLNOŚLĄSKIE, 7),
            new SearchCity("Jelenia Góra", Voivodeship.DOLNOŚLĄSKIE, 7),
            new SearchCity("Wałbrzych", Voivodeship.DOLNOŚLĄSKIE, 7),
            new SearchCity("Głogów", Voivodeship.DOLNOŚLĄSKIE, 7),

            new SearchCity("Bydgoszcz", Voivodeship.KUJAWSKO_POMORSKIE, 7),
            new SearchCity("Toruń", Voivodeship.KUJAWSKO_POMORSKIE, 7),
            new SearchCity("Grudziądz", Voivodeship.KUJAWSKO_POMORSKIE, 7),
            new SearchCity("Włocławek", Voivodeship.KUJAWSKO_POMORSKIE, 7),


            new SearchCity("Lublin", Voivodeship.LUBELSKIE, 7),
            new SearchCity("Zamość", Voivodeship.LUBELSKIE, 7),
            new SearchCity("Biała Podlaska", Voivodeship.LUBELSKIE, 7),
            new SearchCity("Łuków", Voivodeship.LUBELSKIE, 7),
//            new SearchCity("Chełm", Voivodeship.LUBELSKIE, 7),


            new SearchCity("Zielona Góra", Voivodeship.LUBUSKIE, 7),
            new SearchCity("Gorzów Wielkopolski", Voivodeship.LUBUSKIE, 7),
            new SearchCity("Nowa Sól", Voivodeship.LUBUSKIE, 7),
            new SearchCity("Żary", Voivodeship.LUBUSKIE, 7),
//            new SearchCity("Świebodzin", Voivodeship.LUBUSKIE, 7),


            new SearchCity("Łódź", Voivodeship.ŁÓDZKIE, 7),
            new SearchCity("Piotrków Trybunalski", Voivodeship.ŁÓDZKIE, 7),
            new SearchCity("Łowicz", Voivodeship.ŁÓDZKIE, 7),
            new SearchCity("Wieluń", Voivodeship.ŁÓDZKIE, 7),


            new SearchCity("Kraków", Voivodeship.MAŁOPOLSKIE, 7),
            new SearchCity("Tarnów", Voivodeship.MAŁOPOLSKIE, 28),
            new SearchCity("Chrzanów", Voivodeship.MAŁOPOLSKIE, 55),
            new SearchCity("Nowy Targ", Voivodeship.MAŁOPOLSKIE, 55),

            new SearchCity("Warszawa", Voivodeship.MAZOWIECKIE, 7),
            new SearchCity("Radom", Voivodeship.MAZOWIECKIE, 7),
            new SearchCity("Płock", Voivodeship.MAZOWIECKIE, 7),
            new SearchCity("Ostrołęka", Voivodeship.MAZOWIECKIE, 7),
//            new SearchCity("Siedlce", Voivodeship.MAZOWIECKIE, 7),


            new SearchCity("Opole", Voivodeship.OPOLSKIE, 7),
            new SearchCity("Kędzierzyn-Koźle", Voivodeship.OPOLSKIE, 7),
            new SearchCity("Olesno", Voivodeship.OPOLSKIE, 7),
            new SearchCity("Nysa", Voivodeship.OPOLSKIE, 7),
            new SearchCity("Kluczbork", Voivodeship.OPOLSKIE, 7),


            new SearchCity("Białystok", Voivodeship.PODLASKIE, 7),
            new SearchCity("Łomża", Voivodeship.PODLASKIE, 7),
            new SearchCity("Suwałki", Voivodeship.PODLASKIE, 7),
            new SearchCity("Grajewo", Voivodeship.PODLASKIE, 7),
            new SearchCity("Piątnica", Voivodeship.PODLASKIE, 7),
            new SearchCity("Jedwabne", Voivodeship.PODLASKIE, 7),

            new SearchCity("Rzeszów", Voivodeship.PODKARPACKIE, 7),
            new SearchCity("Mielec", Voivodeship.PODKARPACKIE, 7),
            new SearchCity("Tarnobrzeg", Voivodeship.PODKARPACKIE, 7),
            new SearchCity("Przemyśl", Voivodeship.PODKARPACKIE, 7),
//            new SearchCity("Krosno", Voivodeship.PODKARPACKIE, 7),


            new SearchCity("Gdańsk", Voivodeship.POMORSKIE, 7),
            new SearchCity("Gdynia", Voivodeship.POMORSKIE, 7),
            new SearchCity("Słupsk", Voivodeship.POMORSKIE, 7),
            new SearchCity("Wejherowo", Voivodeship.POMORSKIE, 7),
//            new SearchCity("Chojnice", Voivodeship.POMORSKIE, 7),


            new SearchCity("Katowice", Voivodeship.ŚLĄSKIE, 7),
            new SearchCity("Bielsko-Biała", Voivodeship.ŚLĄSKIE, 7),
            new SearchCity("Częstochowa", Voivodeship.ŚLĄSKIE, 7),
            new SearchCity("Zabrze", Voivodeship.ŚLĄSKIE, 7),
            new SearchCity("Ruda Śląska", Voivodeship.ŚLĄSKIE, 7),
            new SearchCity("Gliwice", Voivodeship.ŚLĄSKIE, 7),
            new SearchCity("Sosnowiec", Voivodeship.ŚLĄSKIE, 7),
            new SearchCity("Bytom", Voivodeship.ŚLĄSKIE, 7),
            new SearchCity("Chorzów", Voivodeship.ŚLĄSKIE, 7),
            new SearchCity("Pszczyna", Voivodeship.ŚLĄSKIE, 7),

            new SearchCity("Kielce", Voivodeship.ŚWIĘTOKRZYSKIE, 7),
            new SearchCity("Ostrowiec Świętokrzyski", Voivodeship.ŚWIĘTOKRZYSKIE, 7),
            new SearchCity("Sandomierz", Voivodeship.ŚWIĘTOKRZYSKIE, 7),
//            new SearchCity("Skarżysko-Kamienna", Voivodeship.ŚWIĘTOKRZYSKIE, 7),
//            new SearchCity("Jędrzejów", Voivodeship.ŚWIĘTOKRZYSKIE, 7),


            new SearchCity("Olsztyn", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
            new SearchCity("Elbląg", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
            new SearchCity("Ełk", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),
//            new SearchCity("Szczytno", Voivodeship.WARMIŃSKO_MAZURSKIE, 7),

            new SearchCity("Poznań", Voivodeship.WIELKOPOLSKIE, 7),
            new SearchCity("Konin", Voivodeship.WIELKOPOLSKIE, 7),
            new SearchCity("Kalisz", Voivodeship.WIELKOPOLSKIE, 7),
            new SearchCity("Gniezno", Voivodeship.WIELKOPOLSKIE, 7),

            new SearchCity("Szczecin", Voivodeship.ZACHODNIOPOMORSKIE, 7),
            new SearchCity("Koszalin", Voivodeship.ZACHODNIOPOMORSKIE, 7),
            new SearchCity("Szczecinek", Voivodeship.ZACHODNIOPOMORSKIE, 7),
            new SearchCity("Stargard", Voivodeship.ZACHODNIOPOMORSKIE, 7)
        );
        Set<SlotWithVoivodeship> results = new HashSet<>();

        try {
            for (SearchCity searchCity : Stream.concat(findVoi.stream(), find.stream())
                .filter(s -> options.voivodeships.contains(s.voivodeship()))
                .toList()) {
                LOG.info("Processing {}", searchCity);
                Optional<Gmina> gmina = Optional.ofNullable(searchCity.name())
                    .map(n -> gminaFinder.find(n, searchCity.voivodeship));
                for (VaccineType vaccine : Set.of(
                    VaccineType.PFIZER,
                    VaccineType.MODERNA,
                    VaccineType.AZ,
                    VaccineType.JJ
                )) {
                    for (int weeks = 2; weeks <= 2; weeks += 1) {
                        Thread.sleep(2000 + (int)(Math.random() * 1000));
                        var search = new Search(
                            new DateRange(LocalDate.now(), LocalDate.now().plusWeeks(weeks)),
                            new TimeRange(
                                LocalTime.of(6, 0),
                                LocalTime.of(23, 59)
                            ),
                            creds.prescriptionId(),
                            List.of(vaccine),
                            searchCity.voivodeship(),
                            gmina.map(Gmina::terc).orElse(null),
                            null
                        );
                        final Set<Result.BasicSlot> list =
                            webSearch(options, creds, client, mapper, searchCity, vaccine, search);
                        LOG.info("Found ({} weeks) for {}, {}: {} slots", weeks, searchCity.name, vaccine, list.size());
                        results.addAll(
                            list.stream()
                                .map(s -> new SlotWithVoivodeship(s, searchCity.voivodeship()))
                                .collect(Collectors.toSet())
                        );
                        if (list.size() > 2 || vaccine != VaccineType.PFIZER) {
                            break;
                        }
                        LOG.debug("+n weeks");
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error("Exception", ex);
            telegram("Prawdopodobnie wygasła sesja");
        }

        new TableFormatter(options.output, mapper, find, Instant.now()).store(placeFinder, results);
    }

    private static Set<Result.BasicSlot> webSearch(Options options, Creds creds, HttpClient client, ObjectMapper mapper,
        SearchCity searchCity, VaccineType vaccine, Search search) throws IOException, InterruptedException {
        String searchStr = mapper.writeValueAsString(search);

        HttpResponse<String> out = client.send(
            requestBuilder(creds).uri(URI.create(
                "https://pacjent.erejestracja.ezdrowie.gov.pl/api/calendarSlots/find"))
                .POST(HttpRequest.BodyPublishers.ofString(searchStr)).build(),

            HttpResponse.BodyHandlers.ofString()
        );
        if (options.storeLogs) {
            Paths.get(options.output, "logs").toFile().mkdirs();
            Files.writeString(
                Paths.get(options.output, "logs", "%s_%s.%s.json".formatted(searchCity.name(), vaccine, Instant.now())),
                out.body(),
                StandardOpenOption.CREATE_NEW
            );
        }
        return new HashSet<>(
            Optional.ofNullable(mapper.readValue(out.body(), Result.class).list()).orElse(List.of())
        );
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

    record SearchCity(String name, Voivodeship voivodeship, int days) {}
}
