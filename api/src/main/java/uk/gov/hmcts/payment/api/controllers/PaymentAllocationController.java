package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.dto.PaymentAllocationDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.service.PaymentAllocationService;

import javax.validation.Valid;

@RestController
@Api(tags = {"PaymentAllocation"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentAllocationController", description = "Payment Allocation REST API")})
public class PaymentAllocationController {

    private final PaymentAllocationService paymentAllocationService;

    private final PaymentDtoMapper paymentDtoMapper;

    @Autowired
    public PaymentAllocationController(PaymentAllocationService paymentAllocationService,
                                       PaymentDtoMapper paymentDtoMapper) {
        this.paymentAllocationService = paymentAllocationService;
        this.paymentDtoMapper = paymentDtoMapper;
    }


    @ApiOperation(value = "Add Payment Allocations", notes = "Add Payment Allocations")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment Allocation created"),
        @ApiResponse(code = 400, message = "Payment Allocation failed")
    })
    @PostMapping(value = "/payment-allocations")
    public ResponseEntity<PaymentAllocationDto> addNewFee(@Valid @RequestBody PaymentAllocationDto paymentAllocationDto) {


        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith()
            .paymentReference(paymentAllocationDto.getPaymentReference())
            .paymentGroupReference(paymentAllocationDto.getPaymentGroupReference())
            .paymentAllocationStatus(paymentAllocationDto.getPaymentAllocationStatus())
            .receivingEmailAddress(paymentAllocationDto.getReceivingEmailAddress())
            .sendingEmailAddress(paymentAllocationDto.getSendingEmailAddress())
            .receivingOffice(paymentAllocationDto.getReceivingOffice())
            .unidentifiedReason(paymentAllocationDto.getUnidentifiedReason())
            .build();

        PaymentAllocationDto allocationDto = paymentDtoMapper.toPaymentAllocationDto(
            paymentAllocationService.createAllocation(paymentAllocation));

        return new ResponseEntity<>(allocationDto, HttpStatus.CREATED);
    }
}
