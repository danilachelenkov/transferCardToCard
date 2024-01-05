package ru.netology.cardtocardservice.service;

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
import ru.netology.cardtocardservice.repository.TransferRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;


@Service
public class TransferService {
    private final TransferRepository transferRepository;

    public TransferService(TransferRepository transferRepository) {
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

        synchronized (transactions) {
            synchronized (accountRest) {
                //Рассчитаем комиссию за перевод. Добавим значение в объект первода
                transferData.setCommissionAmount(transferData.getAmount().getValue() * ComissionTransferDictionary.getCommisionList().get("C2C") / 100);

                if (isPositiveBalance(getAmount(transferData.getCardFromNumber(), accountRest), getTotalTransactionSum(transferData, transactions))) {
                    transferData.setTransactionRegistrationTime(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));

                    //Создаем объект транзакции
                    AccountTransaction transaction = new AccountTransaction(transferData);
                    transaction.setOperationId(UUID.randomUUID().toString());
                    transaction.setCommitCode(ConfirmType.UNKNOWN);

                    return transferRepository.createTransaction(transaction);
                } else {
                    throw new NegativeAccountState(
                            String.format("Account {%s} rest can become negative. Transaction can not be registered. Fill the balance and try it later", transferData.getCardFromNumber()),
                            101);
                }
            }
        }
    }


    public String doComfirm(OperationInfo operationInfo) {

        Map<String, AccountTransaction> transactions = transferRepository.getTransactions();
        Map<String, Integer> accountRest = transferRepository.getAccountRest();

        synchronized (transactions) {
            synchronized (accountRest) {
                switch (operationInfo.getCode().toUpperCase()) {
                    case "COMMIT":

                        if (!transactions.containsKey(operationInfo.getOperationId())) {
                            throw new OperationNotExist(String.format("Operation {%s} not exists in transaction table", operationInfo.getOperationId()), 103);
                        }

                        AccountTransaction transaction = transactions.get(operationInfo.getOperationId());

                        if (transaction.getCommitCode().equals(ConfirmType.COMMITED)) {
                            System.out.println(String.format("Transaction {%s} is already commited", operationInfo.getOperationId()));
                            throw new OperationNotExist(String.format("Transaction {%s} is already commited", operationInfo.getOperationId()), 104);
                        }
                        if (transaction.getCommitCode().equals(ConfirmType.ROLLBACK)) {
                            System.out.println(String.format("Transaction {%s} was rollback", operationInfo.getOperationId()));
                            throw new OperationNotExist(String.format("Transaction {%s} was rollback", operationInfo.getOperationId()), 105);
                        }

                        if (isPositiveBalance(getAmount(transaction.getCardFromNumber(), accountRest),
                                transaction.getAmount().getValue() + transaction.getCommissionAmount())) {
                            return transferRepository.commitTransaction(transaction);

                        } else {
                            transferRepository.rollbackTransaction(operationInfo.getOperationId());
                            throw new NegativeAccountState(String.format("The account PAN {%s} status may receive a negative balance, " +
                                    "operation does not possible. The transaction was rejected (ROLLBACK)", transaction.getCardFromNumber()),
                                    102);
                        }

                    case "ROLLBACK":

                        if (!transactions.containsKey(operationInfo.getOperationId())) {
                            System.out.println(String.format("Operation {%s} not exists in transaction table", operationInfo.getOperationId()));
                            throw new OperationNotExist(String.format("Operation {%s} not exists in transaction table", operationInfo.getOperationId()), 103);
                        }
                        return transferRepository.rollbackTransaction(operationInfo.getOperationId());

                    default:
                        throw new UnknownAccountAction(String.format("Unknown action {%s} for transaction processing", operationInfo.getCode()), 106);
                }
            }
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
        Integer total = unknownTransactionSum + (transferAmount + transferData.getCommissionAmount());
        return total;
    }


    /**
     * Метод возвращает общую сумму неподтвержденных транзакций по переданному номеру карты
     * Общая сумма рассчитывается из суммы перевода и суммы комиссии
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


        System.out.println("Total amount transaction: " + result);
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

        if (!accountRest.containsKey(account)) {
            System.out.println(String.format("Account PAN {%s} not exists in rest table", account));
            throw new AccountNotExist(String.format("Account {%s} not exists in rest table", account), 100);
        }
        result = accountRest.get(account);

        return result;
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
