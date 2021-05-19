package com.kirela.szczepimy;

import java.util.List;
import java.util.UUID;

public record Search(
    DateRange dayRange,
    TimeRange hourRange,
    String prescriptionId, List<VaccineType> vaccineTypes, Voivodeship voiId, Gmina geoId, UUID servicePointId,
    List<String> mobilities,
    int tries, int maxTries
) {
}
