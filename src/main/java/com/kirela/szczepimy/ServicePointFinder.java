package com.kirela.szczepimy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServicePointFinder {
    private static final Logger LOG = LogManager.getLogger(ServicePointFinder.class);

    private final NavigableMap<String, List<ServicePoint>> grouped;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final Map<Integer, ExtendedServicePoint> extendedServicePoints = new HashMap<>();
    // https://www.gov.pl/api/data/covid-vaccination-point/246801

    private final Map<Integer, String> phoneCorrections = Map.ofEntries(
        Map.entry(273956, "486797157"),
        Map.entry(273238, "566100488"),
        Map.entry(283722, "616771011"),
        Map.entry(281259, "222992406"),
        Map.entry(288437, "126372791"),
        Map.entry(289055, "517194743 178652000"),
//        Map.entry(287920, "")
        // Tauron Arena 123491500
        Map.entry(288509, "509842442 690694775 690694999")
    );

    public ServicePointFinder(ObjectMapper mapper) {

        this.mapper = mapper;
        client = HttpClient.newBuilder()
//            .version(HttpClient.Version.HTTP_1_1)
            .build();
        try {
            HttpResponse<String> out = client.send(
                requestBuilder().uri(
                    URI.create("https://www.gov.pl/api/data/covid-vaccination-point")
                ).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            if (out.statusCode() != 200) {
                throw new IllegalStateException("Can't read data for service points, got status %d".formatted(out.statusCode()));
            }
            List<ServicePoint> points = mapper.readValue(out.body(), new TypeReference<>() {});
            grouped = points.stream()
                .map(ServicePointFinder::correctData)
                .collect(Collectors.groupingBy(point -> point.address().toLowerCase(), TreeMap::new, Collectors.toList()));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalize(String name) {
        return name
            .replaceAll(" +", " ")
            .trim()
            .replace("ul. ", "")
            .replace("Ul. ", "")
            .replace("UL. ", "")
            .replace("AL. ", "")
            .replace("Al. ", "")
            .replace("al. ", "")
            .replace("Aleja ", "")
            .replace("ALEJA ", "")
            .replace("-", " ")
            .trim();
    }

    private static ServicePoint correctData(ServicePoint point) {
        String address = normalize(point.address())
            .trim()
            .replaceAll("^NA ([0-9])", point.place() + " $1");
        String place = point.place()
            .replaceAll("DZIEGOWICE", "DZIERGOWICE");
        if (point.place().equals("SĘDZISZÓW MŁP.") && point.address().equals("3-GO MAJA 2")) {
            address = point.address();
            place = "Sędziszów Małopolski";
        }
        return new ServicePoint(
            point.id(),
            point.ordinalNumber(),
            point.facilityName(),
            point.terc(),
            address,
            point.zipCode(),
            point.voivodeship(),
            point.county(),
            point.community(),
            point.facilityType(),
            place,
            point.lon(),
            point.lat()
        );
    }

    public Optional<ExtendedServicePoint> findByAddress(ExtendedResult.ServicePoint servicePoint) {
        Optional<ServicePoint> maybe = findByAddressInternal(servicePoint);
        return maybe.map(this::upgrade);
    }

    private ExtendedServicePoint upgrade(ServicePoint servicePoint) {
        if (extendedServicePoints.containsKey(servicePoint.id())) {
            return extendedServicePoints.get(servicePoint.id());
        } else {
            try {
                LOG.debug("Doing request for {}", servicePoint.id());
                HttpResponse<String> out = client.send(
                    requestBuilder().uri(URI.create(
                        "https://www.gov.pl/api/data/covid-vaccination-point/%d".formatted(servicePoint.id())))
                        .GET().build(),

                    HttpResponse.BodyHandlers.ofString()
                );
                if (out.statusCode() == 200) {
                    extendedServicePoints.put(
                        servicePoint.id(),
                        correctPhones(mapper.readValue(out.body(), ExtendedServicePoint.class))
                    );
                    return extendedServicePoints.get(servicePoint.id());
                } else {
                    LOG.error("Issue with finding {}, got status {}", servicePoint.id(), out.statusCode());
                    return new ExtendedServicePoint(
                        servicePoint.id(),
                        servicePoint.ordinalNumber(),
                        servicePoint.facilityName(),
                        servicePoint.terc(),
                        servicePoint.address(),
                        servicePoint.zipCode(),
                        servicePoint.voivodeship(),
                        servicePoint.county(),
                        servicePoint.community(),
                        servicePoint.facilityType(),
                        servicePoint.place(),
                        servicePoint.lon(),
                        servicePoint.lat(),
                        null, null, null, null, null, null, null);
                }
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private ExtendedServicePoint correctPhones(ExtendedServicePoint value) {
        return new ExtendedServicePoint(
            value.id(),
            value.ordinalNumber(),
            value.facilityName(),
            value.terc(),
            value.address(),
            value.zipCode(),
            value.voivodeship(),
            value.county(),
            value.community(),
            value.facilityType(),
            value.place(),
            value.lon(),
            value.lat(),
            phoneCorrections.getOrDefault(value.id(), value.telephone()),
            value.fax(),
            value.site(),
            value.facilityDescription(),
            value.limitations(),
            value.additionalInformation(),
            value.updateDate()
        );
    }

    private Optional<ServicePoint> findByAddressInternal(ExtendedResult.ServicePoint servicePoint) {
        String address = normalize(servicePoint.addressText()).toLowerCase();
        if (!grouped.containsKey(address)) {
            LOG.warn("Did not find address '{}' '{}' ({}) in {}", address, servicePoint.place(), servicePoint.name(), servicePoint.voivodeship());
            return Optional.empty();
        }
        List<ServicePoint> found = grouped.get(address).stream()
            .filter(p -> p.voivodeship() == servicePoint.voivodeship())
            .toList();
        if (found.isEmpty()) {
            LOG.warn("Did not find address in list for '{}' '{}' in {}", address, servicePoint.place(), servicePoint.voivodeship());
            return Optional.empty();
        }
        if (found.size() > 1) {
            String place = Optional.ofNullable(servicePoint.place()).map(String::toLowerCase).orElse("").trim();
            Map<String, List<ServicePoint>> placeFind = found.stream()
                .filter(f -> f.place().equalsIgnoreCase(place))
                .collect(Collectors.groupingBy(ServicePoint::lat));
            if (placeFind.isEmpty()) {
                LOG.warn("Did not find address in placed list for '{}' '{}' in {}", address, servicePoint.place(), servicePoint.voivodeship());
                return Optional.empty();
            }
            if (placeFind.size() > 1) {
                String facilityName = Optional.ofNullable(servicePoint.name()).map(String::toLowerCase).orElse("").trim();
                Map<String, List<ServicePoint>> facilityFind = found.stream()
                    .filter(f -> f.facilityName().equalsIgnoreCase(facilityName))
                    .collect(Collectors.groupingBy(ServicePoint::lat));
                if (facilityFind.size() == 1) {
                    return Optional.of(facilityFind.values().stream().findFirst().get().get(0));
                }
                if (facilityFind.isEmpty()) {
                    LOG.warn("Did not find address in facility list for '{}' '{}' in {}", address, servicePoint.place(), servicePoint.voivodeship());
                    return Optional.empty();
                }
                if (found.size() > 1) {
                    LOG.warn("Too many addresses in facility list for '{}' '{}' in {}", address, servicePoint.place(), servicePoint.voivodeship());
                    return Optional.empty();
                }
                return Optional.empty();
            }
            return Optional.of(
                placeFind.values().stream().findFirst().get().get(0)
            );
        }
        return Optional.of(found.get(0));
    }

    private static HttpRequest.Builder requestBuilder() {
        return HttpRequest.newBuilder()
            .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36")
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "https://www.gov.pl/web/szczepimysie/mapa-punktow-szczepien");
    }

}
