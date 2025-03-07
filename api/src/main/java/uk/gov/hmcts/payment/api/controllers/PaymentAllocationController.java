package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.PaymentAllocationDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@Tag(name = "PaymentAllocationController", description = "Payment Allocation REST API")
public class PaymentAllocationController {

    private final PaymentDtoMapper paymentDtoMapper;

    private final PaymentService<PaymentFeeLink, String> paymentService;

    private final Payment2Repository paymentRepository;

    @Autowired
    public PaymentAllocationController(PaymentDtoMapper paymentDtoMapper,PaymentService<PaymentFeeLink, String> paymentService,Payment2Repository paymentRepository) {
        this.paymentDtoMapper = paymentDtoMapper;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }


    @Operation(summary = "Add Payment Allocations", description = "Add Payment Allocations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Payment Allocation created"),
        @ApiResponse(responseCode = "400", description = "Payment Allocation failed")
    })
    @PostMapping(value = "/payment-allocations")
    public ResponseEntity<PaymentAllocationDto> addNewFee(@Valid @RequestBody PaymentAllocationDto paymentAllocationDto) {

        PaymentFeeLink paymentFeeLink = paymentService.retrievePayment(paymentAllocationDto.getPaymentReference());
        Optional<Payment> payment = paymentFeeLink.getPayments().stream()
            .filter(p -> p.getReference().equals(paymentAllocationDto.getPaymentReference())).findAny();
        if (payment.isPresent()) {
            List<PaymentAllocation> paymentAllocationList = new ArrayList<PaymentAllocation>();
            PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith()
                .paymentReference(paymentAllocationDto.getPaymentReference())
                .paymentGroupReference(paymentAllocationDto.getPaymentGroupReference())
                .paymentAllocationStatus(paymentAllocationDto.getPaymentAllocationStatus())
                .reason(paymentAllocationDto.getReason())
                .explanation(paymentAllocationDto.getExplanation())
                .userName(paymentAllocationDto.getUserName())
                .receivingOffice(paymentAllocationDto.getReceivingOffice())
                .unidentifiedReason(paymentAllocationDto.getUnidentifiedReason())
                .build();


            paymentAllocationList.add(paymentAllocation);
            payment.get().setPaymentAllocation(paymentAllocationList);
            Payment paymentResponse = paymentRepository.save(payment.get());
            PaymentAllocationDto allocationDto = new PaymentAllocationDto();
            for(PaymentAllocation allocation: paymentResponse.getPaymentAllocation())
            {
                allocationDto = paymentDtoMapper.toPaymentAllocationDto(allocation);

            }


            return new ResponseEntity<>(allocationDto, HttpStatus.CREATED);
        }
        return ResponseEntity.notFound().build();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return "Payment Record not found----"+ex.getMessage();
    }
}
