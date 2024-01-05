package ru.netology.cardtocardservice.service;

import org.springframework.stereotype.Service;
import ru.netology.cardtocardservice.dictionary.ComissionTransferDictionary;
import ru.netology.cardtocardservice.domain.OperationInfo;
import ru.netology.cardtocardservice.domain.TransferInfo;
import ru.netology.cardtocardservice.exception.NegativeAccountState;
import ru.netology.cardtocardservice.exception.UnknownAccountAction;
import ru.netology.cardtocardservice.repository.TransferRepository;


@Service
public class TransferService {
    private final TransferRepository transferRepository;

    public TransferService(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }


    //todo добавлять проводку с комиссией сразу при регистрации. комиссия берется сверху суммы от остатка
    public String doTransaction(TransferInfo transferData) {

        Integer currentAccountAmount = transferRepository.getAmount(transferData.getCardFromNumber());
        Integer unknownTransactionSum = transferRepository.getUnknownTotalTransactSum(transferData.getCardFromNumber());

        Integer transferAmount = transferData.getAmount().getValue();
        Integer commissionAmount = transferAmount * ComissionTransferDictionary.getCommisionList().get("C2C") / 100;

        Integer totalTransactionSum = unknownTransactionSum + (transferAmount + commissionAmount);

        if (transferRepository.isPositiveBalance(currentAccountAmount, totalTransactionSum)) {
            /*Создаем 1 проводку для перевода с дополнительным полем "комиссия". Сумма комиссии может быть любой, в зависимости от настройки ComissionTransferDictionary.
              Вариант: возможно создать две проводки, так было бы правильно. Выполнять обработку для группы. Проводки: перевод и комиссия соединить связью PK:id - FK:parent_id, если комиссия
              не рассчитана, то проводка комиссии не создется. Но пока так делать не будем, обойдемся одной проводкой.
             */
            return transferRepository.createTransaction(transferData);
        } else {
            throw new NegativeAccountState(
                    String.format("Account {%s} rest can become negative. Transaction can not be registered. Fill the balance and try it later", transferData.getCardFromNumber()),
                    101);
        }
    }

    public String doCommit(OperationInfo operationInfo) {
        switch (operationInfo.getCode().toUpperCase()) {
            case "COMMIT":
                return transferRepository.commitTransaction(operationInfo.getOperationId());
            case "ROLLBACK":
                return transferRepository.rollbackTransaction(operationInfo.getOperationId());
            default:
                throw new UnknownAccountAction(String.format("Unknown action {%s} for transaction processing", operationInfo.getCode()), 106);
        }
    }


}
