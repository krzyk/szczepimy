package com.kirela.szczepimy;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.UUID;

public record ExtendedServicePoint(
    int id,
    UUID uuid,
    int ordinalNumber,
    String facilityName,
    String terc,
    String address,
    String zipCode,
    Voivodeship voivodeship,
    String county,
    String community,
    String facilityType,
    String place,
    String lon,
    String lat,
    String telephone,
    String fax,
    String site,
    String facilityDescription,
    String limitations,
    String additionalInformation,
    String updateDate
) {
}
