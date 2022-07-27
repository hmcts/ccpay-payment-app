package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusResponseMapper;
import uk.gov.hmcts.payment.api.exception.FailureReferenceNotFoundException;
import uk.gov.hmcts.payment.api.exception.InvalidPaymentFailureRequestException;
import uk.gov.hmcts.payment.api.model.PaymentFailures;
import uk.gov.hmcts.payment.api.service.PaymentStatusUpdateService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@Api(tags = {"Payment Status"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentStatusController", description = "Payment Status REST API")})
public class PaymentStatusController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentStatusController.class);

    @Autowired
    private PaymentStatusUpdateService paymentStatusUpdateService;

    @Autowired
    private PaymentStatusResponseMapper paymentStatusResponseMapper;

    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "successful operation"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 404, message = "No Payments available for the given Payment reference"),
        @ApiResponse(code = 429, message = "Request already received for this failure reference"),
        @ApiResponse(code = 500, message = "Internal Server Error")

    })
    @PaymentExternalAPI
    @PostMapping(path = "/payment-failures/bounced-cheque")
    public ResponseEntity<String> paymentStatusBouncedCheque(@Valid @RequestBody PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto){

        boolean psuLockFeature = featureToggler.getBooleanValue("payment-status-update-flag",false);
        LOG.info("feature toggler enable for  bounced-cheque : {}",psuLockFeature);

        if(psuLockFeature){
            return new ResponseEntity<>("service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }

        LOG.info("Received payment status request bounced-cheque : {}", paymentStatusBouncedChequeDto);
         PaymentFailures insertPaymentFailures =  paymentStatusUpdateService.insertBounceChequePaymentFailure(paymentStatusBouncedChequeDto);

          if(null != insertPaymentFailures.getId()){
              paymentStatusUpdateService.cancelFailurePaymentRefund(paymentStatusBouncedChequeDto.getPaymentReference());
            }

        return new ResponseEntity<>("successful operation", HttpStatus.OK);

    }

    @PaymentExternalAPI
    @PostMapping(path = "/payment-failures/chargeback")
    public ResponseEntity<String> paymentStatusChargeBack(@Valid @RequestBody PaymentStatusChargebackDto paymentStatusChargebackDto){

        boolean psuLockFeature = featureToggler.getBooleanValue("payment-status-update-flag",false);
        LOG.info("feature toggler enable for  chargeback : {}",psuLockFeature);

        if(psuLockFeature){
            return new ResponseEntity<>("service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }

        LOG.info("Received payment status request chargeback : {}", paymentStatusChargebackDto);

        PaymentFailures insertPaymentFailures = paymentStatusUpdateService.insertChargebackPaymentFailure(paymentStatusChargebackDto);

        if(null != insertPaymentFailures.getId()){
            paymentStatusUpdateService.cancelFailurePaymentRefund(paymentStatusChargebackDto.getPaymentReference());
        }
        return new ResponseEntity<>("successful operation", HttpStatus.OK);
    }

    @ApiOperation(value = "Get payment failure by payment reference", notes = "Get payment failure for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment failure retrieved"),
        @ApiResponse(code = 404, message = "No record found"),
        @ApiResponse(code = 500, message = "Internal Server Error")
    })
    @GetMapping("/payment-failures/{paymentReference}")
    public PaymentFailureResponse retrievePaymentFailure(@PathVariable("paymentReference") String paymentReference) {

        List<PaymentFailureResponseDto> payments = paymentStatusUpdateService
            .searchPaymentFailure(paymentReference)
            .stream()
            .map(paymentStatusResponseMapper::toPaymentFailure)
            .collect(Collectors.toList());

        return new PaymentFailureResponse(payments);
    }

    @ApiOperation(value = "Delete payment failure by failure reference for functional test", notes = "Delete payment details for supplied failure reference")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Payment status deleted successfully"),
        @ApiResponse(code = 404, message = "Payment status not found for the given reference")
    })
    @DeleteMapping(value = "/payment-status-delete/{failureReference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByFailureReference(@PathVariable("failureReference") String failureReference) {
        paymentStatusUpdateService.deleteByFailureReference(failureReference);
    }

    @PaymentExternalAPI
    @PatchMapping("/payment-failures/{failureReference}")
    public ResponseEntity<String> paymentStatusSecond(@PathVariable("failureReference") String failureReference,
                                                      @Valid @RequestBody PaymentStatusUpdateSecond paymentStatusUpdateSecondDto) {
        if (featureToggler.getBooleanValue("payment-status-update-flag",false)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        LOG.info("Received payment status update second ping request: {}", paymentStatusUpdateSecondDto);
        paymentStatusUpdateService.updatePaymentFailure(failureReference, paymentStatusUpdateSecondDto);
        return new ResponseEntity<>("Successful operation", HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(FailureReferenceNotFoundException.class)
    public String return429(FailureReferenceNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({InvalidPaymentFailureRequestException.class})
    public String return400(Exception ex) {
        return ex.getMessage();
    }

}
