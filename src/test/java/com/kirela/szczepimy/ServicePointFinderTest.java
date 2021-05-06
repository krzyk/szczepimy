package com.kirela.szczepimy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ServicePointFinderTest {

    @Test
    @Disabled
    public void test() throws IOException, InterruptedException {
        HttpResponse<String> out = HttpClient.newBuilder().build().send(
            HttpRequest.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://www.gov.pl/web/szczepimysie/mapa-punktow-szczepien")
                .uri(
                    URI.create("https://www.gov.pl/api/data/covid-vaccination-point")
                ).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        if (out.statusCode() != 200) {
            throw new IllegalStateException("Can't read data for service points, got status %d".formatted(out.statusCode()));
        }
        List<ServicePoint> points = new ObjectMapper().readValue(out.body(), new TypeReference<>() {});

        int sum = 0;
        Map<Voivodeship, Integer> max = new HashMap<>();
        max.put(Voivodeship.DOLNOŚLĄSKIE, 9);
        max.put(Voivodeship.KUJAWSKO_POMORSKIE, 9);
        max.put(Voivodeship.WARMIŃSKO_MAZURSKIE, 5);
        max.put(Voivodeship.LUBELSKIE, 7);
        max.put(Voivodeship.ŁÓDZKIE, 8);
        max.put(Voivodeship.MAŁOPOLSKIE, 5);
        max.put(Voivodeship.MAZOWIECKIE, 6);
        max.put(Voivodeship.OPOLSKIE, 6);
        max.put(Voivodeship.PODKARPACKIE, 7);
        max.put(Voivodeship.PODLASKIE, 8);
        max.put(Voivodeship.POMORSKIE, 8);
        max.put(Voivodeship.ŚLĄSKIE, 15);
        max.put(Voivodeship.ŚWIĘTOKRZYSKIE, 5);
        max.put(Voivodeship.WIELKOPOLSKIE, 9);
        max.put(Voivodeship.ZACHODNIOPOMORSKIE, 10);
        max.put(Voivodeship.LUBUSKIE, 5);
        for (Voivodeship value : Voivodeship.values()) {
            System.out.println("---");
            System.out.println(value.readable());
            TreeMap<String, List<ServicePoint>> grouped = points.stream()
                .filter(s -> s.voivodeship() == value)
                .flatMap(s -> {
                    if (s.facilityType().equals("2")) {
                        return Stream.of(s, s);
                    } else {
                        return Stream.of(s);
                    }
                })
                .collect(Collectors.groupingBy(point -> point.place().toLowerCase(), TreeMap::new, Collectors.toList()));
            List<Map.Entry<String, Integer>> tr = grouped.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().size()))
                .filter(e -> e.getValue() > max.get(value))
                .toList();
                System.out.println("Count = " + tr.size());
                sum += tr.size();
            tr.stream()
                .sorted(Comparator.comparingInt((ToIntFunction<Map.Entry<String, Integer>>) Map.Entry::getValue).reversed())
                .forEach(e -> System.out.println(e.getKey() + " " + e.getValue()));
        }
        System.out.println("\nSUM = " + sum);
    }

    @Test
    public void aaa() {
        var mapper = Main.getMapper();
        var finder = new ServicePointFinder(mapper, Map.of());
        assertThat(finder.findByAddress(new ExtendedResult.ServicePoint(UUID.randomUUID(), "name", "WIDOK 1", "Winnica", Voivodeship.MAZOWIECKIE, null)))
            .isNotEmpty();
        assertThat(finder.findByAddress(new ExtendedResult.ServicePoint(UUID.randomUUID(), "name", "Al. 1000 Lecia 13", "Olkusz", Voivodeship.MAŁOPOLSKIE, null)))
            .isNotEmpty();
//        assertThat(finder.findByAddress("GRUNWALDZKA 12", null, Voivodeship.MAZOWIECKIE))
//            .isEmpty();
//        assertThat(finder.findByAddress("ul. Kościuszki 15", null, Voivodeship.DOLNOŚLĄSKIE))
//            .isNotEmpty();
//        assertThat(finder.findByAddress("Cieplarniana 25 D", null, Voivodeship.MAZOWIECKIE))
//            .isNotEmpty();
//        assertThat(finder.findByAddress("Zielona 23", null, Voivodeship.DOLNOŚLĄSKIE))
//            .isNotEmpty();
//        assertThat(finder.findByAddress("Wybrzeże Ojca Św. Jana Pawła II  2/2", null, Voivodeship.PODKARPACKIE))
//            .isNotEmpty();
//        assertThat(finder.findByAddress("KRAKOWSKA 16", "PUNKT SZCZEPIEŃ", Voivodeship.PODKARPACKIE))
//            .isNotEmpty();
    }

    @Test
    void test2() {
        Arrays.stream(Voivodeship.values())
            .forEach(v -> System.out.print(v.readable() + " "));
    }
}
