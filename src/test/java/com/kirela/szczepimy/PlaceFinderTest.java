package com.kirela.szczepimy;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaceFinderTest {

    @Test
    public void ignoresIncorrectSuffixesPrefixes() {
        Assertions.assertThat(new PlaceFinder().findInAddress("GRUNWALDZKA 82, GDAŃSK WRZESZCZ", Voivodeship.POMORSKIE))
            .isEqualTo("Gdańsk");
        Assertions.assertThat(new PlaceFinder().findInAddress("GRUNWALDZKA 12, M. St. Warszawa", Voivodeship.MAZOWIECKIE))
            .isEqualTo("Warszawa");
        Assertions.assertThat(new PlaceFinder().findInAddress("ul. Kościuszki 15, Lądek Zdrój", Voivodeship.DOLNOŚLĄSKIE))
            .isEqualTo("Lądek-Zdrój");
        Assertions.assertThat(new PlaceFinder().findInAddress("Cieplarniana 25 D, Warszawa Wesoła", Voivodeship.MAZOWIECKIE))
            .isEqualTo("Warszawa");
        Assertions.assertThat(new PlaceFinder().findInAddress("Zielona 23, Duszniki Zdrój", Voivodeship.DOLNOŚLĄSKIE))
            .isEqualTo("Duszniki-Zdrój");
        Assertions.assertThat(new PlaceFinder().findInAddress("3 Maja 36D, Czerwionka- Leszczyny", Voivodeship.ŚLĄSKIE))
            .isEqualTo("Czerwionka-Leszczyny");
    }
}
