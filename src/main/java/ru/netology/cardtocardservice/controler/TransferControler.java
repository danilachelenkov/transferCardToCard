package ru.netology.cardtocardservice.controler;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.netology.cardtocardservice.domain.OperationInfo;
import ru.netology.cardtocardservice.domain.TransactionInfo;
import ru.netology.cardtocardservice.domain.TransferInfo;
import ru.netology.cardtocardservice.processor.ValidationProcessor;
import ru.netology.cardtocardservice.service.TransferService;

import java.text.ParseException;

@Slf4j
@Validated
@RestController
@RequestMapping("/")
public class TransferControler {
    private final TransferService transferService;

    public TransferControler(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> doTransfer(@Valid @RequestBody TransferInfo transferInfo) throws IllegalAccessException, ParseException {
        ValidationProcessor.validateTransferInfo(transferInfo);

        log.debug(transferInfo.toString());
        return new ResponseEntity<>(new TransactionInfo(transferService.doTransaction(transferInfo)), HttpStatus.OK);
    }

    @PostMapping("/confirmOperation")
    public ResponseEntity<?> commit(@Valid @RequestBody OperationInfo operationInfo) {

        log.debug(operationInfo.toString());
        return new ResponseEntity<>(new TransactionInfo(transferService.doConfirm(operationInfo)), HttpStatus.OK);
    }

}
