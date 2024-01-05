package ru.netology.cardtocardservice.domain;

import lombok.Data;

@Data
public class ExceptionInfo {
    private String message;
    private int id;


    public ExceptionInfo(String message, Integer id) {
        this.message = message;
        this.id = id;
    }
}
