package ru.netology.cardtocardservice.repository;

import org.springframework.stereotype.Repository;
import ru.netology.cardtocardservice.domain.AccountTransaction;
import ru.netology.cardtocardservice.domain.ConfirmType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Хранилище обеспечивает хранение in-memory:
 * Остатки по счетам
 * История транзакций по счетам + состояния
 */
@Repository
public class TransferRepository {
    private final Map<String, Integer> accountRest = new HashMap<>();
    private final Map<String, AccountTransaction> transactions = new HashMap<>();

    public TransferRepository() {
        //для теста
        this.accountRest.put("4548987854653322", 10000);
        this.accountRest.put("4548987854653311", 50);

        //добавим счет комиссии
        this.accountRest.put("7470000000000001", 0);
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
     * @param transaction объект транзакции текущего перевода
     * @return возвращает operationId идентификатор, под которым была сохранена транзакция в таблицу проводок
     */
    public String createTransaction(AccountTransaction transaction) {

        //Сохраняем в табилцу транзакций
        transactions.put(transaction.getOperationId(), transaction);

        System.out.println("Transaction " + transaction.getOperationId() + " is created");

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
            doDebet(transaction.getCardFromNumber(), transaction.getCommissionAmount());
            doCredit("7470000000000001", transaction.getCommissionAmount());
        }

        //Списываем сумму перевода со счета по Дебету, пополняем этой же суммой счет по Кредиту на другой карте
        doDebet(transaction.getCardFromNumber(), transaction.getAmount().getValue());
        doCredit(transaction.getCardToNumber(), transaction.getAmount().getValue());

        //Подтверждаем транзакцию
        updateTransaction(transaction.getOperationId(), ConfirmType.COMMITED);
        System.out.println(String.format("Transaction {%s} is commited", transaction.getOperationId()));

        System.out.println(String.format("Card {%s} amount is {%s} and Card {%s} amount is {%s}",
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
        System.out.println(String.format("Transaction {%s} is rejected", operationId));
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
