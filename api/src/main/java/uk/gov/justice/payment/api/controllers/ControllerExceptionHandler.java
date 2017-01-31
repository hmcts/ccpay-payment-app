package uk.gov.justice.payment.api.controllers;


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
import uk.gov.justice.payment.api.parameters.serviceid.UnknownServiceIdException;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class ControllerExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @ExceptionHandler(value = {Exception.class})
    @ResponseStatus(code = INTERNAL_SERVER_ERROR)
    public void unknownException(Exception e) {
        LOG.error("Unknown error has occurred", e);
    }

    @ExceptionHandler(value = {UnknownServiceIdException.class})
    @ResponseStatus(code = UNPROCESSABLE_ENTITY, reason = "Unknown service id provided")
    public void unknownServiceIdException(UnknownServiceIdException e) {
        LOG.warn("Unknown service id provided", e);
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
