package ru.netology.cardtocardservice.repository;

import org.springframework.stereotype.Repository;
import ru.netology.cardtocardservice.domain.AccountTransaction;
import ru.netology.cardtocardservice.domain.ConfirmType;
import ru.netology.cardtocardservice.domain.TransferInfo;
import ru.netology.cardtocardservice.exception.AccountNotExist;
import ru.netology.cardtocardservice.exception.NegativeAccountState;
import ru.netology.cardtocardservice.exception.OperationNotExist;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//todo перенести не свойстенную логику в TransferService

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
        //for test
        this.accountRest.put("4548987854653322", 10000);
        this.accountRest.put("4548987854653311", 50);
    }

    /**
     * Метод создает свободную (без состояния) транзакцию в таблице проводок по переданному счету
     *
     * @param transfer Объект перевода, хранит всю необходимую информацию о реквизитах перевода
     * @return возвращает operationId идентификатор, под которым была сохранена транзакция в таблицу проводок
     */
    public String createTransaction(TransferInfo transfer) {
        String operationId = UUID.randomUUID().toString();

        transfer.setTransactionRegistrationTime(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));

        AccountTransaction accountTransaction = new AccountTransaction(transfer);
        accountTransaction.setCommitCode(ConfirmType.UNKNOWN);

        synchronized (transactions) {
            transactions.put(operationId, accountTransaction);
        }

        System.out.println("Transaction " + operationId + " is created");
        return operationId;
    }

    /**
     * Метод выполняет подтверждение свободной транзакции
     *
     * @param operationId идентификатор транзакции для подтверждения
     * @return возвращает идентификатор operationId подтвержденной транзакции
     */
    public synchronized String commitTransaction(String operationId) {

        if (!transactions.containsKey(operationId)) {
            throw new OperationNotExist(String.format("Operation {%s} not exists in transaction table", operationId), 103);
        }

        AccountTransaction transaction = transactions.get(operationId);

        if (transaction.getCommitCode().equals(ConfirmType.COMMITED)) {
            System.out.println(String.format("Transaction {%s} is already commited", operationId));
            throw new OperationNotExist(String.format("Transaction {%s} is already commited", operationId), 104);
        }
        if (transaction.getCommitCode().equals(ConfirmType.ROLLBACK)) {
            System.out.println(String.format("Transaction {%s} was rollback", operationId));
            throw new OperationNotExist(String.format("Transaction {%s} was rollback", operationId), 105);
        }

        if (isPositiveBalance(accountRest.get(transaction.getCardFromNumber()),
                transaction.getAmount().getValue() + transaction.getCommissionAmount())) {

            if (transaction.getCommissionAmount() > 0) {
                doDebet(transaction.getCardFromNumber(), transaction.getCommissionAmount());
            }

            doDebet(transaction.getCardFromNumber(), transaction.getAmount().getValue());
            doCredit(transaction.getCardToNumber(), transaction.getAmount().getValue());
            updateTransaction(operationId, ConfirmType.COMMITED);

            System.out.println(String.format("Transaction {%s} is commited", operationId));

        } else {
            updateTransaction(operationId, ConfirmType.ROLLBACK);
            throw new NegativeAccountState(String.format("The account PAN {%s} status may receive a negative balance, " +
                    "operation does not possible. The transaction was rejected (ROLLBACK)", transaction.getCardFromNumber()),
                    102);
        }

        return operationId;
    }

    /**
     * Метод выполняет откат свободной транзакции
     *
     * @param operationId идентификатор транзакции для отмены транзакции
     * @return возвращает идентификатор operationId отмененной транзакции
     */
    public String rollbackTransaction(String operationId) {
        synchronized (transactions) {
            if (!transactions.containsKey(operationId)) {
                System.out.println(String.format("Operation {%s} not exists in transaction table", operationId));
                throw new OperationNotExist(String.format("Operation {%s} not exists in transaction table", operationId), 103);
            }
        }
        updateTransaction(operationId, ConfirmType.ROLLBACK);
        return operationId;
    }

    /**
     * Метод возвращает текущий остаток на счете карты
     *
     * @param account PAN - номер карты
     * @return возвращает размер остатка карты
     */
    public Integer getAmount(String account) {
        Integer result = 0;
        synchronized (accountRest) {
            if (!accountRest.containsKey(account)) {
                System.out.println(String.format("Account PAN {%s} not exists in rest table", account));
                throw new AccountNotExist(String.format("Account {%s} not exists in rest table", account), 100);
            }
            result = accountRest.get(account);
        }
        return result;
    }

    /**
     * Метод возвращает общую сумму неподтвержденных транзакций по переданному номеру счета
     * Общая сумма рассчитывается из суммы перевода и суммы комиссии
     *
     * @param account значение номера счета (карты)
     * @return сумма неподтвержденных транзакций
     */
    public Integer getUnknownTotalTransactSum(String account) {
        Integer result = 0;

        synchronized (transactions) {
            result = transactions.values().stream()
                    .filter(x -> x.getCommitCode().equals(ConfirmType.UNKNOWN))
                    .filter(y -> y.getCardFromNumber().equals(account))
                    .map(x -> new TransactAmountInfo(
                            x.getAmount().getValue(),
                            x.getCommissionAmount()))
                    .reduce(0, (x, y) -> x + y.getTotalSum(), Integer::sum);
        }

        System.out.println("Total amount transaction: " + result);
        return result;
    }

    /**
     * Выполняет проверку состояние остатка счета на положительное значение
     *
     * @param debCurrentRest текущий остаток по счету списания
     * @param totalAmount    плановая сумма списания
     * @return возвращает результат разности сумм
     */
    public boolean isPositiveBalance(Integer debCurrentRest, Integer totalAmount) {
        return debCurrentRest - totalAmount > 0;
    }


    /**
     * Метод выполняет списание с остатка (дебетование счета)
     */
    private synchronized void doDebet(String accountDebet, Integer amount) {
        accountRest.put(accountDebet, accountRest.get(accountDebet) - amount);
    }

    /**
     * Метод выполняет пополнение остатка счета (кредитование счета)
     */
    private synchronized void doCredit(String accountCredit, Integer amount) {
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

    /**
     * Класс внутренний. Добавлен для удобства рассчета общей суммы: перевод + комиссия
     */
    class TransactAmountInfo {
        private Integer transactAmount;
        private Integer transactCommision;

        public TransactAmountInfo(Integer transactAmount, Integer transactCommision) {
            this.transactAmount = transactAmount;
            this.transactCommision = transactCommision;
        }

        public Integer getTransactAmount() {
            return transactAmount;
        }

        public Integer getTransactCommision() {
            return transactCommision;
        }

        public Integer getTotalSum() {
            return transactAmount + transactCommision;
        }
    }
}
