package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;

@RestController
@Api(tags = {"Payment group"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentGroupController", description = "Payment group REST API")})
public class PaymentGroupController {

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;

    private final PaymentGroupDtoMapper paymentGroupDtoMapper;


    @Autowired
    public PaymentGroupController(PaymentFeeLinkRepository paymentFeeLinkRepository, PaymentGroupDtoMapper paymentGroupDtoMapper) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.paymentGroupDtoMapper = paymentGroupDtoMapper;
    }

    @ApiOperation(value = "Get payment details by payment group reference", notes = "Get payment details for supplied payment group reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 403, message = "Payment info forbidden"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @GetMapping(value = "/payment-group/{payment-group-reference}")
    public ResponseEntity<PaymentGroupDto> retrieve(@PathVariable("payment-group-reference") String paymentGroupReference) {
        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference).orElseThrow(InvalidPaymentGroupReferenceException::new);

        return new ResponseEntity<>(paymentGroupDtoMapper.toPaymentGroupDto(paymentFeeLink), HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(InvalidPaymentGroupReferenceException.class)
    public String return403(InvalidPaymentGroupReferenceException ex) {
        return ex.getMessage();
    }
}
