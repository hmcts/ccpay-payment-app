package uk.gov.hmcts.payment.api.controllers;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.exception.LiberataServiceTimeoutException;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.casepaymentorders.client.exceptions.CpoInternalServerErrorException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RestErrorHandler.class);
    @ExceptionHandler(ValidationErrorException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ValidationErrorDTO validationException(ValidationErrorException ex) {
        LOG.debug("ValidationErrors are :{}", ex.getErrors(), ex);
        return ex.getErrors();
    }

    @ExceptionHandler({GovPayException.class, CpoInternalServerErrorException.class, CheckDigitException.class, PaymentException.class})
    public ResponseEntity return500(Exception ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({GatewayTimeoutException.class, AccountServiceUnavailableException.class,LiberataServiceTimeoutException.class})
    public ResponseEntity return504(Exception ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.GATEWAY_TIMEOUT);
    }
}
