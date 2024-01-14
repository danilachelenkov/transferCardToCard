package ru.netology.cardtocardservice.repository;

import ru.netology.cardtocardservice.domain.AccountTransaction;
import ru.netology.cardtocardservice.domain.TransferInfo;

import java.util.Map;

public interface Storagable {
    String createTransaction(TransferInfo transferInfo);

    String commitTransaction(AccountTransaction transaction);

    String rollbackTransaction(String operationId);

    Map<String, Integer> getAccountRest();

    Map<String, AccountTransaction> getTransactions();
}
