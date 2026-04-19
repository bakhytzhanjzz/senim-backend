package com.senim.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Static catalog of Kazakhstan banks and their mortgage programs.
 * Used by the deal creation / edit forms for autocomplete dropdowns.
 */
@RestController
@RequestMapping("/api/v1/banks")
public class BankController {

    public record MortgageProgram(String code, String name, String description, double minRate) {}
    public record Bank(String name, String shortName, List<MortgageProgram> programs) {}

    private static final List<Bank> CATALOG = List.of(
            new Bank("Halyk Bank", "Halyk", List.of(
                    new MortgageProgram("baspana_hit", "Баспана Хит",
                            "Ипотека на первичное и вторичное жильё", 16.9),
                    new MortgageProgram("7_20_25", "7-20-25",
                            "Государственная программа, ставка 7%", 7.0),
                    new MortgageProgram("orda", "Орда", "Молодым семьям", 14.5)
            )),
            new Bank("Kaspi Bank", "Kaspi", List.of(
                    new MortgageProgram("kaspi_home", "Kaspi Home",
                            "Ипотека в мобильном приложении", 18.9),
                    new MortgageProgram("kaspi_red_ipoteka", "Kaspi Red Ипотека",
                            "С рассрочкой первого взноса", 17.5)
            )),
            new Bank("Otbasy Bank", "Otbasy", List.of(
                    new MortgageProgram("nurly_zher", "Нурлы жер",
                            "Государственная программа жилищного строительства", 5.0),
                    new MortgageProgram("standard", "Стандарт",
                            "Классическая жилищная ипотека", 9.5)
            )),
            new Bank("Jusan Bank", "Jusan", List.of(
                    new MortgageProgram("jusan_home", "Jusan Home",
                            "Ипотека с пониженной ставкой", 15.9),
                    new MortgageProgram("primary_market", "Первичный рынок",
                            "Для новостроек от аккредитованных застройщиков", 14.5)
            )),
            new Bank("ForteBank", "Forte", List.of(
                    new MortgageProgram("forte_home", "Forte Home",
                            "Классическая жилищная ипотека", 16.5),
                    new MortgageProgram("forte_7_20_25", "7-20-25",
                            "Государственная программа, ставка 7%", 7.0)
            )),
            new Bank("Freedom Bank", "Freedom", List.of(
                    new MortgageProgram("freedom_ipoteka", "Freedom Ипотека",
                            "Онлайн-оформление за 1 день", 15.0)
            )),
            new Bank("Bereke Bank", "Bereke", List.of(
                    new MortgageProgram("family_ipoteka", "Семейная ипотека",
                            "Льготные условия для семей с детьми", 12.0)
            )),
            new Bank("Bank CenterCredit", "BCC", List.of(
                    new MortgageProgram("bcc_ipoteka", "BCC Ипотека",
                            "Универсальная программа", 17.0)
            ))
    );

    @GetMapping
    public ResponseEntity<List<Bank>> getBanks() {
        return ResponseEntity.ok(CATALOG);
    }
}
