package com.kirela.szczepimy;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaceFinderTest {

    @Test
    public void ignoresIncorrectSuffixesPrefixes() {
        final PlaceFinder placeFinder = new PlaceFinder();
        Assertions.assertThat(placeFinder.findInAddress("Grabowiecka 3 D,E, Ostrowiec św.", Voivodeship.ŚWIĘTOKRZYSKIE).name())
            .isEqualTo("Ostrowiec Świętokrzyski");
        Assertions.assertThat(placeFinder.findInAddress("Górna 7, Bełchatów - Szkoła w Dobrzelowie", Voivodeship.ŁÓDZKIE).name())
            .isEqualTo("Bełchatów");
        Assertions.assertThat(placeFinder.findInAddress("GRUNWALDZKA 82, GDAŃSK WRZESZCZ", Voivodeship.POMORSKIE).name())
            .isEqualTo("Gdańsk");
        Assertions.assertThat(placeFinder.findInAddress("GRUNWALDZKA 12, M. St. Warszawa", Voivodeship.MAZOWIECKIE).name())
            .isEqualTo("Warszawa");
        Assertions.assertThat(placeFinder.findInAddress("ul. Kościuszki 15, Lądek Zdrój", Voivodeship.DOLNOŚLĄSKIE).name())
            .isEqualTo("Lądek-Zdrój");
        Assertions.assertThat(placeFinder.findInAddress("Cieplarniana 25 D, Warszawa Wesoła", Voivodeship.MAZOWIECKIE).name())
            .isEqualTo("Warszawa");
        Assertions.assertThat(placeFinder.findInAddress("Zielona 23, Duszniki Zdrój", Voivodeship.DOLNOŚLĄSKIE).name())
            .isEqualTo("Duszniki-Zdrój");
        Assertions.assertThat(placeFinder.findInAddress("3 Maja 36D, Czerwionka- Leszczyny", Voivodeship.ŚLĄSKIE).name())
            .isEqualTo("Czerwionka-Leszczyny");
    }
}
