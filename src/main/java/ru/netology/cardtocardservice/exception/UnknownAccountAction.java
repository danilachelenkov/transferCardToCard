package ru.netology.cardtocardservice.exception;

public class UnknownAccountAction extends RuntimeException {
    private Integer id;

    public UnknownAccountAction(String msg, Integer id) {
        super(msg);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
