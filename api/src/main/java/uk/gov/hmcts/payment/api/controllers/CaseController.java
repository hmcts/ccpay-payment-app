package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.dto.mapper.CardPaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentService;


import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@Api(tags = {"CaseController"})
@SwaggerDefinition(tags = {@Tag(name = "CaseController", description = "Case Payments API")})
public class CaseController {

    private final PaymentService<PaymentFeeLink, String> paymentService;
    private final CardPaymentDtoMapper cardPaymentDtoMapper;

    @Autowired
    public CaseController(PaymentService<PaymentFeeLink, String> paymentService, CardPaymentDtoMapper cardPaymentDtoMapper) {
        this.paymentService = paymentService;
        this.cardPaymentDtoMapper = cardPaymentDtoMapper;
    }

    @ApiOperation(value = "Get payments for a case", notes = "Get payments for a case")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @RequestMapping(value = "/cases/{case}/payments", method = GET)
    @PaymentExternalAPI
    public PaymentsResponse retrieveCasePayments(@RequestParam(name = "case", required = false) String ccdCaseNumber) {

        return new PaymentsResponse(
            paymentService
                .search(null, null, null, null, ccdCaseNumber)
                .stream()
                .map(cardPaymentDtoMapper::toReconciliationResponseDto)
                .collect(Collectors.toList())
        );
    }
}
