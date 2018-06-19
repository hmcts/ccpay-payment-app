package uk.gov.hmcts.payment.api.controllers;


import io.swagger.annotations.*;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentRecordDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentRecordService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import javax.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@Api(tags = {"PaymentRecordController"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentRecordController", description = "API for Recording different types of payments")})
public class PaymentRecordController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentRecordController.class);

    private static final String DEFAULT_CURRENCY = "GBP";

    private final PaymentRecordService<PaymentFeeLink, String> paymentRecordService;
    private final PaymentRecordDtoMapper paymentRecordDtoMapper;

    @Autowired
    public PaymentRecordController(PaymentRecordService<PaymentFeeLink, String> paymentRecordService,
                                   PaymentRecordDtoMapper paymentRecordDtoMapper) {
        this.paymentRecordService = paymentRecordService;
        this.paymentRecordDtoMapper = paymentRecordDtoMapper;
    }


    @ApiOperation(value = "Record a payment", notes = "Record a payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/payment-records", method = POST)
    @ResponseBody
    public ResponseEntity<PaymentDto> recordPayment(@Valid @RequestBody PaymentRecordRequest paymentRecordRequest) throws CheckDigitException {
        String paymentGroupReference = PaymentReference.getInstance().getNext();

        Payment payment = Payment.paymentWith()
            .amount(paymentRecordRequest.getAmount())
            .ccdCaseNumber(paymentRecordRequest.getCcdCaseNumber())
            .caseReference(paymentRecordRequest.getCaseReference())
            .currency(paymentRecordRequest.getCurrency().getCode())
            .serviceType(paymentRecordRequest.getService().getName())
            .siteId(paymentRecordRequest.getSiteId())
            .externalReference(paymentRecordRequest.getChequeNo())
            .giroSlipNo(paymentRecordRequest.getGiroSlipNo())
            .build();

        List<PaymentFee> fees = paymentRecordRequest.getFees().stream()
            .map(f -> paymentRecordDtoMapper.toFee(f, payment))
            .collect(Collectors.toList());

        LOG.debug("Record payment for PaymentGroupRef:" + paymentGroupReference + " ,with Payment and " + fees.size() + " - Fees");

        PaymentFeeLink paymentFeeLink = paymentRecordService.recordPayment(payment, fees, paymentGroupReference);

        return new ResponseEntity<>(paymentRecordDtoMapper.toCreateRecordPaymentResponse(paymentFeeLink), HttpStatus.CREATED);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }
}

