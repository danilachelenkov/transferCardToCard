package ru.netology.cardtocardservice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.netology.cardtocardservice.domain.*;
import ru.netology.cardtocardservice.repository.Storagable;
import ru.netology.cardtocardservice.service.TransferService;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    @Mock
    Storagable transferRepository;

    @InjectMocks
    TransferService transferService;

    @Test
    void doTransaction_ReturnOperationIdString() {
        //given
        TransferInfo transferInfo = getTransferObj();
        Mockito.doReturn(Map.of("4548987854653322", 1000,
                        "4548987854653311", 50,
                        "7060100000000001", 0)
                ).when(this.transferRepository).getAccountRest();

        Mockito.doReturn(new HashMap<>()).when(this.transferRepository).getTransactions();
        Mockito.doReturn("11234567890").when(this.transferRepository).createTransaction(transferInfo);

        //when
        String operationId = this.transferService.doTransaction(transferInfo);

        //then
        Assertions.assertNotNull(operationId);
    }

    @Test
    void doConfirm_CommitedAction_ReturnOperationIdValue() {

        //given
        String expected = "54321";

        OperationInfo operationInfo = getOperationObj("0000");
        TransferInfo transferInfo = getTransferObj();
        Mockito.doReturn(Map.of("4548987854653322", 1000,
                "4548987854653311", 50,
                "7060100000000001", 0)
        ).when(this.transferRepository).getAccountRest();

        Map<String, AccountTransaction> transactions = getTransaction(transferInfo);

        Mockito.doReturn(transactions).when(this.transferRepository).getTransactions();
        Mockito.doReturn("54321").when(this.transferRepository).commitTransaction(transactions.get("7777"));

        //when
        String result = this.transferService.doConfirm(operationInfo);

        //then
        Assertions.assertEquals(expected, result);
    }

    @Test
    void doConfirm_RollbackAction_ReturnOperationIdValue() {
        //given
        OperationInfo operationInfo = getOperationObj("0001");
        TransferInfo transferInfo = getTransferObj();

        Mockito.doReturn(Map.of("4548987854653322", 1000)).when(this.transferRepository).getAccountRest();

        Map<String, AccountTransaction> transactions = getTransaction(transferInfo);

        Mockito.doReturn(transactions).when(this.transferRepository).getTransactions();
        Mockito.doReturn("54321").when(this.transferRepository).rollbackTransaction("7777");

        String expected = "54321";

        //when
        String result = this.transferService.doConfirm(operationInfo);

        //then
        Assertions.assertEquals(expected, result);
    }

    private OperationInfo getOperationObj(String typeConfim) {
        OperationInfo operationInfo = new OperationInfo();
        operationInfo.setOperationId("7777");
        operationInfo.setCode(typeConfim);
        return operationInfo;
    }

    private TransferInfo getTransferObj() {
        TransferAmount transferAmount = new TransferAmount();
        transferAmount.setValue(100);
        transferAmount.setCurrency("RUR");

        TransferInfo transferInfo = new TransferInfo();
        transferInfo.setCardFromNumber("4548987854653322");
        transferInfo.setCardToNumber("4548987854653311");
        transferInfo.setCardFromCVV("956");
        transferInfo.setCardFromValidTill("08/24");
        transferInfo.setAmount(transferAmount);
        return transferInfo;
    }

    private Map<String, AccountTransaction> getTransaction(TransferInfo transferInfo) {
        AccountTransaction accountTransaction = new AccountTransaction(transferInfo);
        accountTransaction.setCommitCode(ConfirmType.UNKNOWN);
        accountTransaction.setOperationId("7777");
        accountTransaction.setCommissionAmount(1);
        Map<String, AccountTransaction> transactions = Map.of("7777", accountTransaction);

        return transactions;
    }
}
