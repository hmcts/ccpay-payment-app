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
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.exception.BulkScanningServiceUnavailableException;
import uk.gov.hmcts.payment.api.exception.InvalidServiceRequestCancellationException;
import uk.gov.hmcts.payment.api.exception.LiberataServiceTimeoutException;
import uk.gov.hmcts.payment.api.exceptions.ServiceRequestCancellationException;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.exceptions.ServiceRequestReferenceNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.*;
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

    @ExceptionHandler({CpoInternalServerErrorException.class, CheckDigitException.class})
    public ResponseEntity return500(Exception ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({GatewayTimeoutException.class, AccountServiceUnavailableException.class, LiberataServiceTimeoutException.class,
        BulkScanningServiceUnavailableException.class})
    public ResponseEntity return504(Exception ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.GATEWAY_TIMEOUT);
    }
    @ExceptionHandler(ServiceRequestExceptionForNoMatchingAmount.class)
    public ResponseEntity return417(ServiceRequestExceptionForNoMatchingAmount ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.EXPECTATION_FAILED);
    }

    @ExceptionHandler(ServiceRequestExceptionForNoAmountDue.class)
    public ResponseEntity return412(ServiceRequestExceptionForNoAmountDue ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.PRECONDITION_FAILED);
    }

    @ExceptionHandler(value = {NoServiceFoundException.class, ServiceRequestReferenceNotFoundException.class, AccountNotFoundException.class})
    public ResponseEntity return404(Exception ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // The cancellation errors below return the message as a plain String, like every other handler in this class and
    // most @ExceptionHandler methods in the controllers. No content type is set, deliberately, so Spring chooses one
    // from the request's Accept header: application/json when the client asks for JSON, otherwise text/plain. Either
    // way the body is the unquoted message, so it is not valid JSON even when labelled application/json. Setting
    // text/plain here alone would make these endpoints differ from the rest of the API; changing every handler would
    // change what existing clients receive, so it belongs in its own change. See Appendix A of the Service Request
    // Cancellation LLD.
    @ExceptionHandler(InvalidServiceRequestCancellationException.class)
    public ResponseEntity<String> invalidServiceRequestCancellation(InvalidServiceRequestCancellationException ex) {
        LOG.warn("Service request cancellation refused as invalid: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServiceRequestCancellationException.class)
    public ResponseEntity<String> serviceRequestCancellationConflict(ServiceRequestCancellationException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({PaymentNotFoundException.class, InvalidFeeRequestException.class, PaymentException.class,
        ServiceRequestException.class})
    public ResponseEntity return400(Exception ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
