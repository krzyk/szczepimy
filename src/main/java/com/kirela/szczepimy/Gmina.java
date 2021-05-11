package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = GminaSerializer.class)
public record Gmina(String name, String terc, int population) {
    // duze miasta mają 011 na końcu, tj. 1261 - Kraków (+ 011)

//    BIAŁA_PODLASKA("0661011"),
//    BIAŁYSTOK("2061011"),
//    BIELSKO_BIAŁA("2461011"),
//    BYDGOSZCZ("0461011"),
//    BYTOM("2462011"),
//    CHEŁM("0662011"),
//    CHORZÓW("2463011"),
//    CZĘSTOCHOWA("2464011"),
//    DĄBROWA_GÓRNICZA("2465011"),
//    ELBLĄG("2861011"),
//    GDAŃSK("2261011"),
//    GDYNIA("2262011"),
//    GLIWICE("2466011"),
//    GORZÓW_WIELKOPOLSKI("0861011"),
//    GRUDZIĄDZ("0462011"),
//    JASTRZĘBIE_ZDRÓJ("2467011"),
//    JAWORZNO("2468011"),
//    JELENIA_GÓRA("0261011"),
//    KALISZ("3061011"),
//    KATOWICE("2469011"),
//    KIELCE("2661011"),
//    KONIN("3062011"),
//    KOSZALIN("3261011"),
//    KRAKÓW("1261011"),
//    KROSNO("1861011"),
//    LEGNICA("0262011"),
//    LESZNO("3063011"),
//    ŁÓDŹ("1061011"),
//    ŁOMŻA("2062011"),
//    LUBLIN("0663011"),
//    MYSŁOWICE("2470011"),
//    NOWY_SĄCZ("1262011"),
//    OLSZTYN("2862011"),
//    OPOLE("1661011"),
//    OSTROŁĘKA("1461011"),
//    PIEKARY_ŚLĄSKIE("2471011"),
//    PIOTRKÓW_TRYBUNALSKI("1062011"),
//    PŁOCK("1462011"),
//    POZNAŃ("3064011"),
//    PRZEMYŚL("1862011"),
//    RADOM("1463011"),
//    RUDA_ŚLĄSKA("2472011"),
//    RYBNIK("2473011"),
//    RZESZÓW("1863011"),
//    SIEDLCE("1464011"),
//    SIEMIANOWICE_ŚLĄSKIE("2474011"),
//    SKIERNIEWICE("1063011"),
//    SŁUPSK("2263011"),
//    SOPOT("2264011"),
//    SOSNOWIEC("2475011"),
//    SUWAŁKI("2063011"),
//    ŚWIĘTOCHŁOWICE("2476011"),
//    ŚWINOUJŚCIE("3263011"),
//    SZCZECIN("3262011"),
//    TARNOBRZEG("1864011"),
//    TARNÓW("1263011"),
//    TORUŃ("0463011"),
//    TYCHY("2477011"),
//    WAŁBRZYCH("0265011"),
//    WARSZAWA("1465011"),
//    WŁOCŁAWEK("0464011"),
//    WROCŁAW("0264011"),
//    ZABRZE("2478011"),
//    ZAMOŚĆ("0664011"),
//    ZIELONA_GÓRA("0862011"),
//    ŻORY("2479011");
//
//    private final String terc;
//
//    City(String terc) {
//        this.terc = terc;
//    }

    public Voivodeship voivodeship() {
        return Voivodeship.from(Integer.parseInt(terc.substring(0, 2)));
    }

    public String normalize() {
        return normalize(name());
    }

    public static String normalize(String text) {
        return text.toLowerCase()
            .replace('ą', 'a')
            .replace('ę', 'e')
            .replace('ć', 'c')
            .replace('ń', 'n')
            .replace('ó', 'o')
            .replace('ś', 's')
            .replace('ź', 'z')
            .replace('ż', 'z')
            .replace('ł', 'l');
    }

    public boolean matches(String other) {
        return normalize(other).equals(name());
    }
}
