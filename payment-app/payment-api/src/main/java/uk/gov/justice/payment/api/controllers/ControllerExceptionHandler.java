package uk.gov.justice.payment.api.controllers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.gov.justice.payment.api.parameters.serviceid.UnknownServiceIdException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

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
}
