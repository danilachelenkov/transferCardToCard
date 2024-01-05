package ru.netology.cardtocardservice.exception;

public class NegativeAccountState extends RuntimeException {
    private Integer id;
    public NegativeAccountState(String msg, Integer id) {
        super(msg);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
