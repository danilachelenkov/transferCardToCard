package ru.netology.cardtocardservice.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.netology.cardtocardservice.domain.ExceptionInfo;
import ru.netology.cardtocardservice.exception.DateInvalidException;
import ru.netology.cardtocardservice.exception.UnknownValidTypeException;


@RestControllerAdvice
public class ValidationExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> processValidationMessage(MethodArgumentNotValidException e) {

/*
* Можно выводить списком все сообщения, которые сформированы в процессе валидации
        List<String> errorMessages = new ArrayList<>();
        e.getAllErrors().forEach(error -> errorMessages.add(error.getDefaultMessage()));
        Map<String, List<String>> mapError = new HashMap<>();
        mapError.put("id", new ArrayList<>());
        mapError.put("message", errorMessages);
*/
        ExceptionInfo exceptionInfo = new ExceptionInfo(e.getAllErrors().get(0).getDefaultMessage(), 107);
        return new ResponseEntity<>(exceptionInfo, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DateInvalidException.class)
    public ResponseEntity<?> responseEntityUnknownAccountAction(DateInvalidException e) {
        return new ResponseEntity<>(new ExceptionInfo(e.getMessage(), e.getId()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnknownValidTypeException.class)
    public ResponseEntity<?> responseEntityRuntimeException(UnknownValidTypeException e) {
        return new ResponseEntity<>(new ExceptionInfo(e.getMessage(), e.getId()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
