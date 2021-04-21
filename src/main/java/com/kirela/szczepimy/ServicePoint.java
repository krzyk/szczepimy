package com.kirela.szczepimy;

public record ServicePoint(
    int id,
    int ordinalNumber,
    String facilityName,
    String terc,
    String address,
    String zipCode,
    Voivodeship voivodeship,
    String county,
    String community,
    String place,
    String lon,
    String lat
) {
}
