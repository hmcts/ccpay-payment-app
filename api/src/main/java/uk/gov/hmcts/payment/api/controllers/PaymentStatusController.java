package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusResponseMapper;
import uk.gov.hmcts.payment.api.exception.FailureReferenceNotFoundException;
import uk.gov.hmcts.payment.api.exception.InvalidPaymentFailureRequestException;
import uk.gov.hmcts.payment.api.exception.LiberataServiceInaccessibleException;
import uk.gov.hmcts.payment.api.model.PaymentFailures;
import uk.gov.hmcts.payment.api.service.PaymentStatusUpdateService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.payment.api.util.DateUtil.atEndOfDay;
import static uk.gov.hmcts.payment.api.util.DateUtil.atStartOfDay;


@RestController
@Tag(name = "PaymentStatusController", description = "Payment Status REST API")
public class PaymentStatusController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentStatusController.class);
    private static final String PAYMENT_STATUS_UPDATE_FLAG = "payment-status-update-flag";
    private static final String SUCCESSFUL_OPERATION = "successful operation";

    @Autowired
    private PaymentStatusUpdateService paymentStatusUpdateService;

    @Autowired
    private PaymentStatusResponseMapper paymentStatusResponseMapper;

    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "successful operation"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "404", description = "No Payments available for the given Payment reference"),
        @ApiResponse(responseCode = "429", description = "Request already received for this failure reference"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PaymentExternalAPI
    @PostMapping(path = "/payment-failures/bounced-cheque")
    public ResponseEntity<String> paymentStatusBouncedCheque(@Valid @RequestBody PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto){

        if (featureToggler.getBooleanValue(PAYMENT_STATUS_UPDATE_FLAG,false)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        LOG.info("Received payment status request for bounced-cheque : {}", paymentStatusBouncedChequeDto);
         PaymentFailures insertPaymentFailures =  paymentStatusUpdateService.insertBounceChequePaymentFailure(paymentStatusBouncedChequeDto);

          if(null != insertPaymentFailures.getId()){
              paymentStatusUpdateService.cancelFailurePaymentRefund(paymentStatusBouncedChequeDto.getPaymentReference());
            }

        return new ResponseEntity<>(SUCCESSFUL_OPERATION, HttpStatus.OK);

    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "successful operation"),
    })
    @PaymentExternalAPI
    @PostMapping(path = "/payment-failures/chargeback")
    public ResponseEntity<String> paymentStatusChargeBack(@Valid @RequestBody PaymentStatusChargebackDto paymentStatusChargebackDto) throws JsonProcessingException {

        if (featureToggler.getBooleanValue(PAYMENT_STATUS_UPDATE_FLAG,false)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        String jsonString = new ObjectMapper().writeValueAsString(paymentStatusChargebackDto);
        LOG.info("Received payment status request for chargeback : {}", jsonString);

        PaymentFailures insertPaymentFailures = paymentStatusUpdateService.insertChargebackPaymentFailure(paymentStatusChargebackDto);

        if(null != insertPaymentFailures.getId()){
            paymentStatusUpdateService.cancelFailurePaymentRefund(paymentStatusChargebackDto.getPaymentReference());
        }
        return new ResponseEntity<>(SUCCESSFUL_OPERATION, HttpStatus.OK);
    }

    @PaymentExternalAPI
    @PostMapping(path = "/payment-failures/unprocessed-payment")
    public ResponseEntity<String> unprocessedPayment(@Valid @RequestBody UnprocessedPayment unprocessedPayment,
                                                     @RequestHeader(required = false) MultiValueMap<String, String> headers) {
        if (featureToggler.getBooleanValue(PAYMENT_STATUS_UPDATE_FLAG,false)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        LOG.info("Received payment status request for unprocessed payment : {}", unprocessedPayment);

        paymentStatusUpdateService.unprocessedPayment(unprocessedPayment, headers);

        return new ResponseEntity<>(SUCCESSFUL_OPERATION, HttpStatus.OK);
    }

    @Operation(summary = "Get payment failure by payment reference", description = "Get payment failure for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment failure retrieved"),
        @ApiResponse(responseCode = "404", description = "No record found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/payment-failures/{paymentReference}")
    public PaymentFailureResponse retrievePaymentFailure(@PathVariable("paymentReference") String paymentReference) {

        if (featureToggler.getBooleanValue(PAYMENT_STATUS_UPDATE_FLAG,false)) {
            throw new LiberataServiceInaccessibleException("service unavailable");
        }

        List<PaymentFailureResponseDto> payments = paymentStatusUpdateService
            .searchPaymentFailure(paymentReference)
            .stream()
            .map(paymentStatusResponseMapper::toPaymentFailure)
            .collect(Collectors.toList());

        return new PaymentFailureResponse(payments);
    }

    @Operation(summary = "Delete payment failure by failure reference for functional test", description = "Delete payment details for supplied failure reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Payment status deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Payment status not found for the given reference")
    })
    @DeleteMapping(value = "/payment-status-delete/{failureReference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByFailureReference(@PathVariable("failureReference") String failureReference) {
        paymentStatusUpdateService.deleteByFailureReference(failureReference);
    }

    @PaymentExternalAPI
    @PatchMapping("/payment-failures/{failureReference}")
    public ResponseEntity<String> paymentStatusSecond(@PathVariable("failureReference") String failureReference,
                                                      @RequestBody PaymentStatusUpdateSecond paymentStatusUpdateSecondDto) {
        if (featureToggler.getBooleanValue(PAYMENT_STATUS_UPDATE_FLAG,false)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        LOG.info("Received payment status update request for second ping: {}", paymentStatusUpdateSecondDto);
        paymentStatusUpdateService.updatePaymentFailure(failureReference, paymentStatusUpdateSecondDto);
        return new ResponseEntity<>(SUCCESSFUL_OPERATION, HttpStatus.OK);
    }

    @Operation(summary = "update payment reference for unprocessed payment", description = "update payment reference for unprocessed payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "payment reference updated successfully")
    })
    @PatchMapping(value = "/jobs/unprocessed-payment-update")
    public void updateUnprocessedPayment(){

        if (!featureToggler.getBooleanValue(PAYMENT_STATUS_UPDATE_FLAG,false)) {
            LOG.info("Received unprocessed payment update job request");

            paymentStatusUpdateService.updateUnprocessedPayment();
        } else{
            LOG.info(" flag for unprocessed payment update job request is enable");
        }

    }

    @Operation(summary = "API to generate report for payment failure ", description = "Get list of payments failures by providing date range. MM/dd/yyyy is  the supported date/time format.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report Generated"),
        @ApiResponse(responseCode = "404", description = "No Data found to generate Report"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/payment-failures/failure-report")
    public PaymentFailureReportResponse retrievePaymentFailureReport(@RequestParam("date_from") Date fromDate,
                                                                      @RequestParam("date_to") Date toDate,
                                                                      @RequestHeader(required = false) MultiValueMap<String, String> headers,
                                                                      @RequestHeader("Authorization") String authorization) {

        if (featureToggler.getBooleanValue(PAYMENT_STATUS_UPDATE_FLAG,false)) {
            throw new LiberataServiceInaccessibleException("service unavailable");
        }

        LOG.info("Received payment status report request");
       List<PaymentFailureReportDto> paymentFailureReportDto =  paymentStatusUpdateService.paymentFailureReport(atStartOfDay(fromDate), atEndOfDay(toDate),headers);
        return new PaymentFailureReportResponse(paymentFailureReportDto);
    }


    @Operation(summary = "API to generate report for telephony payments ", description = "Get list of telephony payments by providing date range. MM/dd/yyyy is  the supported date/time format.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report Generated"),
        @ApiResponse(responseCode = "404", description = "No Data found to generate Report"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/telephony-payments/telephony-payments-report")
    public TelephonyPaymentsReportResponse retrieveTelephonyPaymentsReport(@RequestParam("date_from") Date fromDate,
                                                                            @RequestParam("date_to") Date toDate,
                                                                            @RequestHeader(required = false) MultiValueMap<String, String> headers,
                                                                            @RequestHeader("Authorization") String authorization) {

        LOG.info("Received telephony payments report request");
        List<TelephonyPaymentsReportDto> telephonyPaymentsReportDto =  paymentStatusUpdateService.telephonyPaymentsReport(atStartOfDay(fromDate), atEndOfDay(toDate), headers);
        return new TelephonyPaymentsReportResponse(telephonyPaymentsReportDto);
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

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler({LiberataServiceInaccessibleException.class})
    public String return503(Exception ex) {
        return ex.getMessage();
    }

}
