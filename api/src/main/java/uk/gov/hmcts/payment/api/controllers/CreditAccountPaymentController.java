package uk.gov.hmcts.payment.api.controllers;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentGroupDto;
import uk.gov.hmcts.payment.api.model.CreditAccountPaymentService;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@Api(value = "CreditAccountPaymentController", description = "Credit account payment REST API")
public class CreditAccountPaymentController {

    private static final Logger LOG = LoggerFactory.getLogger(CreditAccountPaymentController.class);

    private static final String DEFAULT_CURRENCY = "GBP";

    private final CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService;
    private final CreditAccountDtoMapper creditAccountDtoMapper;

    @Autowired
    public CreditAccountPaymentController(@Qualifier("loggingCreditAccountPaymentService") CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService,
                                          CreditAccountDtoMapper creditAccountDtoMapper) {
        this.creditAccountPaymentService = creditAccountPaymentService;
        this.creditAccountDtoMapper = creditAccountDtoMapper;
    }

    @ApiOperation(value = "Create credit account payment", notes = "Create credit account payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/credit-account-payments", method = POST)
    public ResponseEntity<PaymentGroupDto> createCardPayment(@RequestHeader(value = "user-id") String userId,
                                                             @RequestHeader(value = "return-url") String returnURL,
                                                             @Valid @RequestBody CreditAccountPaymentRequest creditAccountPaymentRequest) throws CheckDigitException {
        String paymentGroupReference = PaymentReference.getInstance().getNext();

        List<Payment> payments = creditAccountPaymentRequest.getPayments().stream()
            .map(p -> creditAccountDtoMapper.toPaymentRequest(p))
            .collect(Collectors.toList());

        List<Fee> fees = creditAccountPaymentRequest.getFee().stream()
            .map(f -> creditAccountDtoMapper.toFee(f))
            .collect(Collectors.toList());
        LOG.debug("Create credit account request for PaymentGroupRef:" + paymentGroupReference + " ,with " + payments.size() + " - Payments and " + fees.size() + " - Fees");

        PaymentFeeLink paymentFeeLink = creditAccountPaymentService.create(payments, fees, paymentGroupReference);

        return new ResponseEntity<>(creditAccountDtoMapper.toCreditAccountPaymentDto(paymentFeeLink), CREATED);
    }


    @ApiOperation(value = "Get credit account payment details by payment group reference", notes = "Get payment details for supplied payment group reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/credit-account/payments/{paymentGroupReference}", method = GET)
    public ResponseEntity<PaymentGroupDto> retrievePaymentByGroupReference(@PathVariable("paymentGroupReference") String paymentGroupReference) {
        PaymentFeeLink paymentFeeLink = creditAccountPaymentService.retrieveByPaymentGroupReference(paymentGroupReference);
        return new ResponseEntity<>(creditAccountDtoMapper.toRetrievePaymentGroupReferenceResponse(paymentFeeLink), OK);
    }


    @ApiOperation(value = "Get credit account payment details by payment reference", notes = "Get payment details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/credit-account/payment/{paymentReference}", method = GET)
    public ResponseEntity<PaymentDto> retrievePaymentReference(@PathVariable("paymentReference") String paymentReference) {
        PaymentFeeLink paymentFeeLink = creditAccountPaymentService.retrieveByPaymentReference(paymentReference);
        Payment payment = paymentFeeLink.getPayments().stream().filter(p -> p.getReference().equals(paymentReference))
            .findAny()
            .orElseThrow(PaymentNotFoundException::new);
        return new ResponseEntity<>(creditAccountDtoMapper.toRetrievePaymentResponse(payment), OK);
    }


}
