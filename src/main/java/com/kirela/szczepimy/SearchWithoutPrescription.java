package com.kirela.szczepimy;

import java.util.List;
import java.util.UUID;

public record SearchWithoutPrescription(
    DateRange dayRange,
    TimeRange hourRange,
    List<VaccineType> vaccineTypes, Voivodeship voiId, Gmina geoId, UUID servicePointId,
    List<String> mobilities,
    int tries, int maxTries
) {
    public Search toSearch(String newPrescriptionId) {
        return new Search(
            dayRange,
            hourRange,
            newPrescriptionId,
            vaccineTypes,
            voiId,
            geoId,
            servicePointId,
            mobilities,
            tries,
            maxTries
        );
    }
}
