package ru.netology.cardtocardservice.domain;

import lombok.Data;


public class TransactionInfo {
    private String operationId;

    public String getOperationId() {
        return operationId;
    }

    public TransactionInfo(String operationId) {
        this.operationId = operationId;
    }
}
