package ru.netology.cardtocardservice.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.netology.cardtocardservice.domain.AccountTransaction;
import ru.netology.cardtocardservice.domain.ConfirmType;
import ru.netology.cardtocardservice.domain.TransferInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Хранилище обеспечивает хранение in-memory:
 * Остатки по счетам
 * История транзакций по счетам + состояния
 */
@Repository
@Slf4j
public class TransferRepository {
    private final Map<String, Integer> accountRest = new HashMap<>();
    private final Map<String, AccountTransaction> transactions = new HashMap<>();

    public TransferRepository() {
        //для теста
        this.accountRest.put("4548987854653322", 10000);
        this.accountRest.put("4548987854653311", 50);

        //добавим счет комиссии
        this.accountRest.put("7060100000000001", 0);
    }

    public Map<String, Integer> getAccountRest() {
        return accountRest;
    }

    public Map<String, AccountTransaction> getTransactions() {
        return transactions;
    }

    /**
     * Метод создает свободную (без состояния) транзакцию в таблице проводок по переданному счету
     *
     * @param transferInfo объект испольняемого перевода
     * @return возвращает operationId идентификатор, под которым была сохранена транзакция в таблицу проводок
     */
    public String createTransaction(TransferInfo transferInfo) {

        //Создаем объект транзакции
        AccountTransaction transaction = new AccountTransaction(transferInfo);
        transaction.setOperationId(UUID.randomUUID().toString());
        transaction.setCommitCode(ConfirmType.UNKNOWN);

        //Сохраняем в таблицу транзакций
        transactions.put(transaction.getOperationId(), transaction);

        log.debug("Transaction " + transaction.getOperationId() + " is created");

        return transaction.getOperationId();
    }

    /**
     * Метод выполняет подтверждение свободной транзакции
     *
     * @param transaction транзакция для подтверждения
     * @return возвращает идентификатор operationId подтвержденной транзакции
     */
    public String commitTransaction(AccountTransaction transaction) {
        //Списываем комиссию, если она есть на счет комиссий
        if (transaction.getCommissionAmount() > 0) {
            log.debug(String.format("Transfer has a commission ={%s}", transaction.getCommissionAmount()));
            doDebet(transaction.getCardFromNumber(), transaction.getCommissionAmount());
            doCredit("7060100000000001", transaction.getCommissionAmount());

            log.debug(String.format("Account PAN {%s} is debiting commission amount = {%s} and crediting account {7060100000000001}",
                    transaction.getCardFromNumber(),
                    transaction.getCommissionAmount()));
        }

        //Списываем сумму перевода по счету Дебета, пополняем этой же суммой счет Кредита
        doDebet(transaction.getCardFromNumber(), transaction.getAmount().getValue());
        doCredit(transaction.getCardToNumber(), transaction.getAmount().getValue());

        log.debug(String.format("Account PAN {%s} is debiting amount = {%s} and crediting amount {%s} to account {%s}",
                transaction.getCardFromNumber(),
                transaction.getAmount().getValue(),
                transaction.getCardToNumber(),
                transaction.getAmount().getValue()));

        //Подтверждаем транзакцию
        updateTransaction(transaction.getOperationId(), ConfirmType.COMMITED);

        log.debug(String.format("Transaction {%s} is committed", transaction.getOperationId()));

        log.debug(String.format("Card (debet) {%s} amount is {%s} and Card (credit) {%s} amount is {%s}",
                transaction.getCardFromNumber(),
                accountRest.get(transaction.getCardFromNumber()),
                transaction.getCardToNumber(),
                accountRest.get(transaction.getCardToNumber())));

        return transaction.getOperationId();
    }

    /**
     * Метод выполняет откат свободной транзакции
     *
     * @param operationId идентификатор транзакции для отмены транзакции
     * @return возвращает идентификатор operationId отмененной транзакции
     */
    public String rollbackTransaction(String operationId) {
        updateTransaction(operationId, ConfirmType.ROLLBACK);
        log.debug(String.format("Transaction {%s} is rejected", operationId));

        return operationId;
    }

    /**
     * Метод выполняет списание с остатка (дебетование счета)
     */
    private void doDebet(String accountDebet, Integer amount) {
        accountRest.put(accountDebet, accountRest.get(accountDebet) - amount);
    }

    /**
     * Метод выполняет пополнение остатка счета (кредитование счета)
     */
    private void doCredit(String accountCredit, Integer amount) {
        accountRest.put(accountCredit, accountRest.get(accountCredit) + amount);
    }

    /**
     * Метод выполняет изменение состояние operationID транзакции в таблице транзакций
     *
     * @param operationID идентифакатор транзакции
     * @param confirmType тип действия с транзакцией
     */
    private void updateTransaction(String operationID, ConfirmType confirmType) {
        synchronized (transactions) {
            AccountTransaction transaction = transactions.get(operationID);
            transaction.setTransactionProcessedTime(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));
            transaction.setCommitCode(confirmType);
        }
    }


}
