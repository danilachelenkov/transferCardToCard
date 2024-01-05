package ru.netology.cardtocardservice.controler;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.netology.cardtocardservice.domain.OperationInfo;
import ru.netology.cardtocardservice.domain.TransactionInfo;
import ru.netology.cardtocardservice.domain.TransferInfo;
import ru.netology.cardtocardservice.service.TransferService;

//todo обеспечить логирование
@Validated
//https://www.bezkoder.com/spring-boot-validate-request-body/
@RestController
@RequestMapping("/")
public class TransferControler {
    private final TransferService transferService;

    public TransferControler(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> doTransfer(@Valid @RequestBody TransferInfo transferInfo) {
        System.out.println(transferInfo);
        return new ResponseEntity<>(new TransactionInfo(transferService.doTransaction(transferInfo)), HttpStatus.OK);
    }

    @PostMapping("/confirmOperation")
    public ResponseEntity<?> commit(@Valid @RequestBody OperationInfo operationInfo) {
        System.out.println(operationInfo);
        return new ResponseEntity<>(new TransactionInfo(transferService.doComfirm(operationInfo)), HttpStatus.OK);
    }

}
