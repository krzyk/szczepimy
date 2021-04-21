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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServicePointFinder {
    private static final Logger LOG = LogManager.getLogger(ServicePointFinder.class);

    private final Map<String, List<ServicePoint>> grouped;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final Map<Integer, ExtendedServicePoint> extendedServicePoints = new HashMap<>();
    // https://www.gov.pl/api/data/covid-vaccination-point/246801

    public ServicePointFinder(ObjectMapper mapper) {

        this.mapper = mapper;
        client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        try {
            List<ServicePoint> points = mapper.readValue(
                Objects.requireNonNull(
                    GminaFinder.class.getResourceAsStream("service-points.json")
                ), new TypeReference<>() {});
            grouped = points.stream()
                .map(ServicePointFinder::correctNaAddresses)
                .collect(Collectors.groupingBy(point -> point.address().toLowerCase()));
        } catch (IOException e) {
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
            .trim();
    }

    private static ServicePoint correctNaAddresses(ServicePoint point) {
        return new ServicePoint(
            point.id(),
            point.ordinalNumber(),
            point.facilityName(),
            point.terc(),
            normalize(point.address())
                .trim()
                .replaceAll("^NA ([0-9])", point.community() + " $1")
            ,
            point.zipCode(),
            point.voivodeship(),
            point.county(),
            point.community(),
            point.place(),
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
                LOG.info("Doing request for {}", servicePoint.id());
                HttpResponse<String> out = client.send(
                    requestBuilder().uri(URI.create(
                        "https://www.gov.pl/api/data/covid-vaccination-point/%d".formatted(servicePoint.id())))
                        .GET().build(),

                    HttpResponse.BodyHandlers.ofString()
                );
                if (out.statusCode() == 200) {
                    extendedServicePoints.put(
                        servicePoint.id(),
                        mapper.readValue(out.body(), ExtendedServicePoint.class)
                    );
                    return extendedServicePoints.get(servicePoint.id());
                } else {
                    LOG.error("Issue with finding {}", servicePoint);
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

    private Optional<ServicePoint> findByAddressInternal(ExtendedResult.ServicePoint servicePoint) {
        String address = normalize(servicePoint.addressText()).toLowerCase();
        if (!grouped.containsKey(address)) {
            LOG.warn("Did not find address '{}' in {}", address, servicePoint.voivodeship());
            return Optional.empty();
        }
        List<ServicePoint> found = grouped.get(address).stream()
            .filter(p -> p.voivodeship() == servicePoint.voivodeship())
            .toList();
        if (found.isEmpty()) {
            LOG.warn("Did not find address in list for '{}' in {}", address, servicePoint.voivodeship());
            return Optional.empty();
        }
        if (found.size() > 1) {
            String facilityName = Optional.ofNullable(servicePoint.name()).map(String::toLowerCase).orElse("");
            Map<String, List<ServicePoint>> namedFind = found.stream()
                .filter(f -> f.facilityName().equalsIgnoreCase(facilityName.trim()))
                .collect(Collectors.groupingBy(ServicePoint::lat));
            if (namedFind.isEmpty()) {
                LOG.warn("Did not find address in placed list for '{}' in {}", address, servicePoint.voivodeship());
                return Optional.empty();
            }
            if (namedFind.size() > 1) {
                LOG.warn("Found too many addresses for placed find '{}' in {}: {}", address, servicePoint.voivodeship(), found);
                return Optional.empty();
            }
            return Optional.of(
                namedFind.values().stream().findFirst().get().get(0)
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
