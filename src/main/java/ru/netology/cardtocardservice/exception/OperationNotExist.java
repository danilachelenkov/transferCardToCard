package ru.netology.cardtocardservice.exception;

public class OperationNotExist extends RuntimeException {
    private Integer id;

    public OperationNotExist(String msg, Integer id) {
        super(msg);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
