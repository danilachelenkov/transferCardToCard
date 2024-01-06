package ru.netology.cardtocardservice.exception;

public class DateInvalidException extends RuntimeException {
    private Integer id;

    public DateInvalidException(String msg, Integer id) {
        super(msg);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
