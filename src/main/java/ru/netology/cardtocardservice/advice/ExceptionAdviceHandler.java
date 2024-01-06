package ru.netology.cardtocardservice.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.netology.cardtocardservice.domain.ExceptionInfo;
import ru.netology.cardtocardservice.exception.*;

@RestControllerAdvice
public class ExceptionAdviceHandler {

    @ExceptionHandler(AccountNotExist.class)
    public ResponseEntity<?> responseEntityAccountNotExist(AccountNotExist e) {
        return new ResponseEntity<>(new ExceptionInfo(e.getMessage(), e.getId()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NegativeAccountState.class)
    public ResponseEntity<?> responseEntityNegativeAccountState(NegativeAccountState e) {
        return new ResponseEntity<>(new ExceptionInfo(e.getMessage(),e.getId()), HttpStatus.METHOD_NOT_ALLOWED
        );
    }

    @ExceptionHandler(OperationNotExist.class)
    public ResponseEntity<?> responseEntityOperationNotExist(OperationNotExist e) {
        return new ResponseEntity<>(new ExceptionInfo(e.getMessage(), e.getId()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnknownAccountAction.class)
    public ResponseEntity<?> responseEntityUnknownAccountAction(UnknownAccountAction e) {
        return new ResponseEntity<>(new ExceptionInfo(e.getMessage(), e.getId()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> responseEntityRuntimeException(RuntimeException e){
        return new ResponseEntity<>(new ExceptionInfo(e.getMessage(),500),HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
