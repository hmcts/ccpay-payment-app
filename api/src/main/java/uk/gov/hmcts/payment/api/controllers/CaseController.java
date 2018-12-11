package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;


import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@Api(tags = {"Case"}, description = "Case REST API")
@SwaggerDefinition(tags = {@Tag(name = "CaseController", description = "Case Payments API")})
public class CaseController {

    private final PaymentService<PaymentFeeLink, String> paymentService;
    private final PaymentDtoMapper paymentDtoMapper;

    @Autowired
    public CaseController(PaymentService<PaymentFeeLink, String> paymentService, PaymentDtoMapper paymentDtoMapper) {
        this.paymentService = paymentService;
        this.paymentDtoMapper = paymentDtoMapper;
    }

    @ApiOperation(value = "Get payments for a case", notes = "Get payments for a case")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @RequestMapping(value = "/cases/{case}/payments", method = GET)
    @PaymentExternalAPI
    public PaymentsResponse retrieveCasePayments(@PathVariable(name = "case") String ccdCaseNumber) {

        List<PaymentDto> payments = paymentService
            .search(null, null, null, null, ccdCaseNumber, null)
            .stream()
            .map(paymentDtoMapper::toReconciliationResponseDto)
            .collect(Collectors.toList());

        if(payments == null || payments.isEmpty()) {
            throw new PaymentNotFoundException();
        }

        return new PaymentsResponse(payments);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentException.class)
    public String notFound(PaymentException ex) {
        return ex.getMessage();
    }

}
