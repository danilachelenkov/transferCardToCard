package ru.netology.cardtocardservice.exception;

public class UnknownValidTypeException extends RuntimeException {
    private Integer id;

    public UnknownValidTypeException(String msg, Integer id) {
        super(msg);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
