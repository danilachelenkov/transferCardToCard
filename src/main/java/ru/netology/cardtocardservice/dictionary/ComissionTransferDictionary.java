package ru.netology.cardtocardservice.dictionary;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс описывает справочник тарифов для переводов
 * С2С - перевод с карты на карту; тариф 1% от суммы перевода
 */
public class ComissionTransferDictionary {
    private static final Map<String,Integer> commisionList = Map.of("C2C",1);
    public static Map<String, Integer> getCommisionList() {
        return commisionList;
    }
}
