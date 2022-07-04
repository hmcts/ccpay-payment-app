package uk.gov.hmcts.payment.api.v1.controllers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class ControllerExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @ExceptionHandler(value = {RuntimeException.class})
    @ResponseStatus(code = INTERNAL_SERVER_ERROR)
    public void unknownException(RuntimeException e) {
        LOG.error("Unknown error has occurred with errorMessage: " + e.getMessage(), e);
    }

    @ExceptionHandler(value = {HttpMessageNotReadableException.class})
    @ResponseStatus(code = BAD_REQUEST)
    public void invalidRequestException(HttpMessageNotReadableException e) {
        LOG.debug("Invalid request", e);
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<String> validationException(MethodArgumentNotValidException e) {
        LOG.debug("Validation error", e);
        FieldError fieldError = e.getBindingResult().getFieldError();
        return new ResponseEntity<>(fieldError.getField() + ": " + fieldError.getDefaultMessage(), UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(value = {DataIntegrityViolationException.class})
    @ResponseStatus(code = CONFLICT)
    public void dataIntegrityViolationException(DataIntegrityViolationException e) {
        LOG.warn("Data integrity violation", e);
    }
}
