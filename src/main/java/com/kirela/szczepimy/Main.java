package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import picocli.CommandLine;

public class Main {

    // https://www.gov.pl/api/data/covid-vaccination-point/246801
    // https://www.gov.pl/api/data/covid-vaccination-point   -> service-points.json

    // punkty adresowe z https://eteryt.stat.gov.pl/eTeryt/rejestr_teryt/udostepnianie_danych/baza_teryt/uzytkownicy_indywidualni/pobieranie/pliki_pelne.aspx?contrast=default
    // albo z https://pacjent.erejestracja.ezdrowie.gov.pl/teryt/api/woj/06/simc?namePrefix=Krak
    private static final ExecutorService exec = Executors.newFixedThreadPool(1);

//    public static record FoundAppointment(String id, LocalDateTime date, Specialization specialization, Clinic clinic, String  doctor) implements Comparable<FoundAppointment> {
//        @Override
//        public int compareTo(FoundAppointment other) {
//            return date.compareTo(other.date);
//        }
//    }


    public static record Range(String from, String to) {}

    @JsonDeserialize(using = VaccineTypeDeserializer.class)
    public enum VaccineType {
        PFIZER("cov19.pfizer"),
        MODERNA("cov19.moderna"),
        AZ("cov19.astra_zeneca"),
        JJ("cov19.johnson_and_johnson");

        private final String name;

        VaccineType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static record Creds(String csrf, String sid, String prescriptionId) {}
    public static record Search(Range dayRange, Range hourRange, String prescriptionId, List<String> vaccineTypes, String voiId, String geoId) {}

    public static record Result(String geoDepth, List<Result.Slot> list) {
        public static record Slot(UUID id, Instant startAt, String duration, ServicePoint servicePoint, VaccineType vaccineType, int dose, String status) {}
        public static record ServicePoint(UUID id, String name, String addressText) {}
    }

    private static final String CHROME = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36";
    private static final String USER_AGENT = "User-Agent";

    public static void main(String[] args) throws IOException, InterruptedException {

        Options options = CommandLine.populateCommand(new Options(), args);
        if (options.usageHelpRequested) {
            CommandLine.usage(options, System.out);
            return;
        }
//        final CookieManager cookies = new CookieManager();
//        cookies.getCookieStore().add(URI.create("https://pacjent.erejestracja.ezdrowie.gov.pl"), new HttpCookie("patient_sid", creds.sid()));

        var creds = new Creds(options.csrf, options.sid, options.prescriptionId);
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        HttpResponse<String> out = client.send(
            requestBuilder(creds).uri(URI.create("https://pacjent.erejestracja.ezdrowie.gov.pl/api/calendarSlots/find"))
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"dayRange":{"from":"2021-04-15","to":"2021-05-30"},"prescriptionId":"%s","vaccineTypes":["cov19.pfizer"],"voiId":"18"}
                        """.formatted(creds.prescriptionId())
                )).build(),

            HttpResponse.BodyHandlers.ofString()
        );
        System.out.println(mapper.readValue(out.body(), Result.class));
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
}
