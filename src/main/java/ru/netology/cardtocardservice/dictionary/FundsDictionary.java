package ru.netology.cardtocardservice.dictionary;

import java.util.Map;

/**
 * Класс описывает справочник видов валют и курсов.
 * Может быть использован для конверсионных переводов
 */
public class FundsDictionary {
    private static Map<String, Double> fundsTax;

    private FundsDictionary() {
        fundsTax = Map.of(
                "RUB", 1.0,
                "USD", 78.35,
                "EUR", 90.21
        );
    }

    public static Map<String, Double> getFundsTax() {
        return fundsTax;
    }
}
