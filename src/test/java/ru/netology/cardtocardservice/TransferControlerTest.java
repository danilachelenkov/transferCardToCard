package ru.netology.cardtocardservice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.netology.cardtocardservice.controler.TransferControler;
import ru.netology.cardtocardservice.domain.OperationInfo;
import ru.netology.cardtocardservice.domain.TransactionInfo;
import ru.netology.cardtocardservice.domain.TransferAmount;
import ru.netology.cardtocardservice.domain.TransferInfo;
import ru.netology.cardtocardservice.service.TransferService;

import java.text.ParseException;

@ExtendWith(MockitoExtension.class)
public class TransferControlerTest {
    @Mock
    TransferService transferService;

    @InjectMocks
    TransferControler transferControler;

    @Test
    void doTransfer_ReturnsValidResponseEntity() throws ParseException, IllegalAccessException {

        //given
        TransferInfo transferInfo = getTransferObj();
        Mockito.doReturn("78900987").when(this.transferService).doTransaction(transferInfo);

        String expected = "78900987";

        //when
        var responseEntity = this.transferControler.doTransfer(transferInfo);

        //then
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        if (responseEntity.getBody() instanceof TransactionInfo transaction) {
            Assertions.assertNotNull(transaction.getOperationId());
            Assertions.assertEquals(expected, transaction.getOperationId());
        }
    }

    @Test
    void commit_ReturnsValidResponseEntity() {
        //given
        String expected = "1234567890";

        OperationInfo operationInfo = getOperationObj(expected);
        TransferInfo transferInfo = getTransferObj();

        Mockito.doReturn(expected).when(this.transferService).doConfirm(operationInfo);

        //when
        var responseEntity = this.transferControler.commit(operationInfo);

        //then
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        if (responseEntity.getBody() instanceof TransactionInfo transaction) {
            Assertions.assertEquals(expected, transaction.getOperationId());
        }
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

    private OperationInfo getOperationObj(String expected) {
        OperationInfo operationInfo = new OperationInfo();
        operationInfo.setOperationId(expected);
        operationInfo.setCode("0000");
        return operationInfo;
    }
}
