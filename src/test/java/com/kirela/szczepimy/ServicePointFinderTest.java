package com.kirela.szczepimy;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ServicePointFinderTest {

    @Test
    public void test() {
        var mapper = Main.getMapper();
        var finder = new ServicePointFinder(mapper);
//        assertThat(finder.findByAddress("GRUNWALDZKA 82", null, Voivodeship.POMORSKIE))
//            .isNotEmpty();
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
}
