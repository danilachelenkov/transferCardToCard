package ru.netology.cardtocardservice.exception;

public class AccountNotExist extends RuntimeException {
    private Integer id;

    public AccountNotExist(String msg, Integer id) {
        super(msg);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
