package com.kirela.szczepimy;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class GminaFinderTest {

    @Test
    public void correctlyFindsSmallCity() {
        Assertions.assertThat(new GminaFinder().find("Czechowice-Dziedzice", Voivodeship.ŚLĄSKIE))
            .isEqualTo(new Gmina("Czechowice-Dziedzice", "2402044", 0));
    }

}
