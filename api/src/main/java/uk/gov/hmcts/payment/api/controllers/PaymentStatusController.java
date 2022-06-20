package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.dto.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusChargebackDto;
import uk.gov.hmcts.payment.api.exception.FailureReferenceNotFoundException;
import uk.gov.hmcts.payment.api.exception.RefundServiceUnavailableException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFailures;
import uk.gov.hmcts.payment.api.service.PaymentStatusUpdateService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;
import java.util.Optional;


@RestController
@Api(tags = {"Payment Status"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentStatusController", description = "Payment Status REST API")})
public class PaymentStatusController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentStatusController.class);

    @Autowired
    private PaymentStatusUpdateService paymentStatusUpdateService;

    @Autowired
    private Payment2Repository paymentRepository;

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "successful operation"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 404, message = "No Payments available for the given Payment reference"),
        @ApiResponse(code = 429, message = "Request already received for this failure reference"),
        @ApiResponse(code = 500, message = "Internal Server Error")

    })
    @PaymentExternalAPI
    @PostMapping(path = "/payment-failures/bounced-cheque")
    public ResponseEntity<String> paymentStatusBouncedCheque(@Valid @RequestBody PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto) throws JsonProcessingException {

        LOG.info("Received payment status request bounced-cheque : {}", paymentStatusBouncedChequeDto);
        boolean refundStatusUpdate= false;
        Optional<Payment> payment = paymentRepository.findByReference(paymentStatusBouncedChequeDto.getPaymentReference());

        if(payment.isEmpty()){
            throw new PaymentNotFoundException("No Payments available for the given Payment reference");
        }

        Optional<PaymentFailures> paymentFailures = paymentStatusUpdateService.searchFailureReference(paymentStatusBouncedChequeDto.getFailureReference());

        if(paymentFailures.isPresent()){
            throw new FailureReferenceNotFoundException("Request already received for this failure reference");
        }

         PaymentFailures insertPaymentFailures =  paymentStatusUpdateService.insertBounceChequePaymentFailure(paymentStatusBouncedChequeDto);

          if(null != insertPaymentFailures.getId()){
              paymentStatusUpdateService.sendFailureMessageToServiceTopic(paymentStatusBouncedChequeDto.getPaymentReference(),paymentStatusBouncedChequeDto.getAmount());
              refundStatusUpdate = paymentStatusUpdateService.cancelFailurePaymentRefund(paymentStatusBouncedChequeDto.getPaymentReference());
        }
          if (refundStatusUpdate){
            return new ResponseEntity<>("successful operation", HttpStatus.OK);
        }

        return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @PaymentExternalAPI
    @PostMapping(path = "/payment-failures/chargeback")
    public ResponseEntity<String> paymentStatusChargeBack(@Valid @RequestBody PaymentStatusChargebackDto paymentStatusChargebackDto) throws JsonProcessingException {

        LOG.info("Received payment status request chargeback : {}", paymentStatusChargebackDto);
        boolean refundStatusUpdate= false;
        Optional<Payment> payment = paymentRepository.findByReference(paymentStatusChargebackDto.getPaymentReference());

        if(payment.isEmpty()){
            throw new PaymentNotFoundException("No Payments available for the given Payment reference");
        }

        Optional<PaymentFailures> paymentFailures = paymentStatusUpdateService.searchFailureReference(paymentStatusChargebackDto.getFailureReference());

        if(paymentFailures.isPresent()){
            throw new FailureReferenceNotFoundException("Request already received for this failure reference");
        }

        PaymentFailures insertPaymentFailures = paymentStatusUpdateService.insertChargebackPaymentFailure(paymentStatusChargebackDto);

        if(null != insertPaymentFailures.getId()){
            paymentStatusUpdateService.sendFailureMessageToServiceTopic(paymentStatusChargebackDto.getPaymentReference(),paymentStatusChargebackDto.getAmount());
            refundStatusUpdate = paymentStatusUpdateService.cancelFailurePaymentRefund(paymentStatusChargebackDto.getPaymentReference());
        }
        if (refundStatusUpdate){
            return new ResponseEntity<>("successful operation", HttpStatus.OK);
        }

        return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ApiOperation(value = "Get payment failure by failure reference for functional test", notes = "Get payment failure for supplied failure reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment failure retrieved")
    })
    @GetMapping("/payment-status/{failureReference}")
    public ResponseEntity<PaymentFailures>  retrievePaymentFailure(@PathVariable("failureReference") String failureReference) {
        return new ResponseEntity<>(paymentStatusUpdateService.searchPaymentFailure(failureReference), HttpStatus.OK);
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

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler( RefundServiceUnavailableException.class)
    public String return500( RefundServiceUnavailableException ex) {
        return ex.getMessage();
    }

}
