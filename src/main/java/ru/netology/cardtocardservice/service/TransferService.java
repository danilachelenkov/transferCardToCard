package ru.netology.cardtocardservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.netology.cardtocardservice.dictionary.ComissionTransferDictionary;
import ru.netology.cardtocardservice.domain.AccountTransaction;
import ru.netology.cardtocardservice.domain.ConfirmType;
import ru.netology.cardtocardservice.domain.OperationInfo;
import ru.netology.cardtocardservice.domain.TransferInfo;
import ru.netology.cardtocardservice.exception.AccountNotExist;
import ru.netology.cardtocardservice.exception.NegativeAccountState;
import ru.netology.cardtocardservice.exception.OperationNotExist;
import ru.netology.cardtocardservice.exception.UnknownAccountAction;
import ru.netology.cardtocardservice.repository.Storagable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
public class TransferService {
    private final Storagable transferRepository;

    public TransferService(Storagable transferRepository) {
        this.transferRepository = transferRepository;
    }

    /**
     * Метод регистрации транзакции
     *
     * @param transferData объект перевода
     * @return возвращает operationId - идентификатор зарегистрированной транзакции
     */
    public String doTransaction(TransferInfo transferData) {
        Map<String, AccountTransaction> transactions = transferRepository.getTransactions();
        Map<String, Integer> accountRest = transferRepository.getAccountRest();

        //Проверим существование счетов в плане счетов, перед созданием транзакции
        checkTransferAccounts(transferData, accountRest);

        //Рассчитаем комиссию за перевод. Добавим значение в объект перевода
        transferData.setCommissionAmount(transferData.getAmount().getValue() * ComissionTransferDictionary.getCommisionList().get("C2C") / 100);

        if (isPositiveBalance(getAmount(transferData.getCardFromNumber(), accountRest), getTotalTransactionSum(transferData, transactions))) {
            transferData.setTransactionRegistrationTime(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));

            String operationId = transferRepository.createTransaction(transferData);
            log.debug(String.format("Transaction {%s} was created. operationId = {%s} ", transferData, operationId));

            return operationId;
        } else {
            String msg = String.format("Account {%s} rest can become negative. Transaction can not be registered. Fill the balance and try it later", transferData.getCardFromNumber());
            log.error(msg);
            throw new NegativeAccountState(msg, 101);
        }
    }


    public String doConfirm(OperationInfo operationInfo) {
        String msg = "";
        Map<String, AccountTransaction> transactions = transferRepository.getTransactions();
        Map<String, Integer> accountRest = transferRepository.getAccountRest();

        if (!transactions.containsKey(operationInfo.getOperationId())) {
            msg = String.format("Transaction {%s} is not exists in transaction table", operationInfo.getOperationId());
            log.error(msg);
            throw new OperationNotExist(msg, 103);
        }

        AccountTransaction transaction = transactions.get(operationInfo.getOperationId());

        if (transaction.getCommitCode().equals(ConfirmType.COMMITED)) {
            msg = String.format("Transaction {%s} is already commited", operationInfo.getOperationId());
            log.error(msg);
            throw new OperationNotExist(msg, 104);
        }
        if (transaction.getCommitCode().equals(ConfirmType.ROLLBACK)) {
            msg = String.format("Transaction {%s} was already rollback", operationInfo.getOperationId());
            log.error(msg);
            throw new OperationNotExist(msg, 105);
        }

        switch (operationInfo.getCode().toUpperCase()) {
            case "0000"://COMMIT

                if (isPositiveBalance(getAmount(transaction.getCardFromNumber(), accountRest),
                        transaction.getAmount().getValue() + transaction.getCommissionAmount())) {

                    String operationId = transferRepository.commitTransaction(transaction);
                    log.debug(String.format("Transaction {%s} is commited. Detail transaction: {%s}", operationId, transaction.toString()));
                    return operationId;

                } else {
                    String operationId = transferRepository.rollbackTransaction(operationInfo.getOperationId());
                    msg = String.format("The account PAN {%s} status may receive a negative balance, " +
                            "operation does not possible. The transaction was rejected (ROLLBACK)", transaction.getCardFromNumber());

                    log.error(String.format("Transaction {%s} was rollback. Detail transaction: {%s} ", operationId, transaction) + msg);
                    throw new NegativeAccountState(msg, 102);
                }

            case "0001"://ROLLBACK
                String operationId = transferRepository.rollbackTransaction(operationInfo.getOperationId());
                log.debug(String.format("Transaction {%s} was rollback. Detail transaction: {%s} ", operationId, transaction));
                return operationId;

            default:
                msg = String.format("Unknown action {%s} for transaction processing", operationInfo.getCode());
                log.error(msg);
                throw new UnknownAccountAction(msg, 106);
        }
    }

    /**
     * Метод возвращает перспективу отстака по счету с учетом текущего перевода и необработанных транзакций  по счету Дебета
     *
     * @param currentAccountAmount текущий остаток на счете
     * @param totalTransactionSum  итоговая сумма списания со счета
     * @return возвращает true - если перспективный остаток счета положительный, иначе false
     */
    private boolean isPositiveBalance(Integer currentAccountAmount, Integer totalTransactionSum) {
        return currentAccountAmount - totalTransactionSum > 0;
    }

    private Integer getTotalTransactionSum(TransferInfo transferData, Map<String, AccountTransaction> transactions) {
        //Сумма перевода и комиссии по необработанным проводкам
        Integer unknownTransactionSum = getUnknownTotalTransactSum(transferData.getCardFromNumber(), transactions);

        //Сумма указанная в переводе
        Integer transferAmount = transferData.getAmount().getValue();

        //Итоговая пердрасчитанная сумма: текущий перевод + комиссия + сумма переводов и комиссий по всем необработанным транзакциям счета
        Integer totalAmount = unknownTransactionSum + (transferAmount + transferData.getCommissionAmount());

        log.debug(String.format("Total transactions with unknown status on debet account = {%s} " +
                        "Transfer amount on debet account  = {%s} " +
                        "Total amount on debet account = {%s} " +
                        "Details: [%s]",
                unknownTransactionSum,
                transferAmount,
                totalAmount,
                transferData)
        );
        return totalAmount;
    }

    /**
     * Метод возвращает общую сумму неподтвержденных транзакций по переданному номеру карты
     * Общая сумма рассчитывается из суммы перевода и суммы комиссии неподтвержденных транзакций
     *
     * @param account значение номера счета (карты)
     * @return сумма неподтвержденных транзакций
     */
    private Integer getUnknownTotalTransactSum(String account, Map<String, AccountTransaction> transactions) {
        Integer result = 0;

        result = transactions.values().stream()
                .filter(x -> x.getCommitCode().equals(ConfirmType.UNKNOWN))
                .filter(y -> y.getCardFromNumber().equals(account))
                .map(x -> new TransactAmountInfo(
                        x.getAmount().getValue(),
                        x.getCommissionAmount()))
                .reduce(0, (x, y) -> x + y.getTotalSum(), Integer::sum);

        log.debug(String.format("Total amount UNKNOWN transaction: " + result + " for account {%s}", account));
        return result;
    }

    /**
     * Метод возвращает текущий остаток по номеру карты
     *
     * @param account PAN - номер карты
     * @return возвращает размер остатка карты
     */
    private Integer getAmount(String account, Map<String, Integer> accountRest) {
        Integer result = 0;
        result = accountRest.get(account);
        return result;
    }

    private void checkTransferAccounts(TransferInfo transferInfo, Map<String, Integer> accountRest) {

        if (!accountRest.containsKey(transferInfo.getCardFromNumber())) {
            log.error(String.format("Debet account PAN {%s} not exists in rest table", transferInfo.getCardFromNumber()));
            throw new AccountNotExist(String.format("Account {%s} not exists in rest table", transferInfo.getCardFromNumber()), 99);
        }

        if (!accountRest.containsKey(transferInfo.getCardToNumber())) {
            log.error(String.format("Credit account PAN {%s} not exists in rest table", transferInfo.getCardToNumber()));
            throw new AccountNotExist(String.format("Account {%s} not exists in rest table", transferInfo.getCardToNumber()), 100);
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
