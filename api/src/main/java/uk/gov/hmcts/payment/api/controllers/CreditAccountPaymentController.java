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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.model.CreditAccountPaymentService;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
    public ResponseEntity<PaymentDto> createCreditAccountPayment(

                                                             @Valid @RequestBody CreditAccountPaymentRequest creditAccountPaymentRequest) throws CheckDigitException {
        String paymentGroupReference = PaymentReference.getInstance().getNext();

        Payment payment = Payment.paymentWith()
            .amount(creditAccountPaymentRequest.getAmount())
            .description(creditAccountPaymentRequest.getDescription())
            .ccdCaseNumber(creditAccountPaymentRequest.getCcdCaseNumber())
            .caseReference(creditAccountPaymentRequest.getCaseReference())
            .currency(creditAccountPaymentRequest.getCurrency().getCode())
            .serviceType(creditAccountPaymentRequest.getServiceName())
            .customerReference(creditAccountPaymentRequest.getCustomerReference())
            .organisationName(creditAccountPaymentRequest.getOrganisationName())
            .pbaNumber(creditAccountPaymentRequest.getAccountNumber())
            .siteId(creditAccountPaymentRequest.getSiteId())
            .build();

        List<Fee> fees = creditAccountPaymentRequest.getFees().stream()
            .map(f -> creditAccountDtoMapper.toFee(f))
            .collect(Collectors.toList());
        LOG.debug("Create credit account request for PaymentGroupRef:" + paymentGroupReference + " ,with Payment and " + fees.size() + " - Fees");

        PaymentFeeLink paymentFeeLink = creditAccountPaymentService.create(payment, fees, paymentGroupReference);

        return new ResponseEntity<>(creditAccountDtoMapper.toCreateCreditAccountPaymentResponse(paymentFeeLink), CREATED);
    }


    @ApiOperation(value = "Get credit account payment details by payment reference", notes = "Get payment details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/credit-account-payments/{paymentReference}", method = GET)
    public ResponseEntity<PaymentDto> retrieve(@PathVariable("paymentReference") String paymentReference) {
        PaymentFeeLink paymentFeeLink = creditAccountPaymentService.retrieveByPaymentReference(paymentReference);
        Payment payment = paymentFeeLink.getPayments().stream().filter(p -> p.getReference().equals(paymentReference))
            .findAny()
            .orElseThrow(PaymentNotFoundException::new);
        List<Fee> fees = paymentFeeLink.getFees();
        return new ResponseEntity<>(creditAccountDtoMapper.toRetrievePaymentResponse(payment, fees), OK);
    }

    @ExceptionHandler(value = {PaymentNotFoundException.class})
    public ResponseEntity httpClientErrorException() {
        return new ResponseEntity(NOT_FOUND);
    }

}
