package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.PaymentGroupFeeRequest;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupFeeDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;

import javax.validation.Valid;

@RestController
@Api(tags = {"Payment group"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentGroupController", description = "Payment group REST API")})
public class PaymentGroupController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentGroupController.class);

    private final PaymentGroupService<PaymentFeeLink, String> paymentGroupService;

    private final PaymentGroupDtoMapper paymentGroupDtoMapper;


    @Autowired
    public PaymentGroupController(PaymentGroupService paymentGroupService, PaymentGroupDtoMapper paymentGroupDtoMapper) {
        this.paymentGroupService = paymentGroupService;
        this.paymentGroupDtoMapper = paymentGroupDtoMapper;
    }

    @ApiOperation(value = "Get payments/remissions/fees details by payment group reference", notes = "Get payments/remissions/fees details for supplied payment group reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 403, message = "Payment info forbidden"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @GetMapping(value = "/payment-groups/{payment-group-reference}")
    public ResponseEntity<PaymentGroupDto> retrievePayment(@PathVariable("payment-group-reference") String paymentGroupReference) {
        PaymentFeeLink paymentFeeLink = paymentGroupService.findByPaymentGroupReference(paymentGroupReference);

        return new ResponseEntity<>(paymentGroupDtoMapper.toPaymentGroupDto(paymentFeeLink), HttpStatus.OK);
    }

    @ApiOperation(value = "Add new Fee to Payment Group", notes = "Add new Fee to Payment Group")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment group with fee created"),
        @ApiResponse(code = 403, message = "Payment group info forbidden"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @PostMapping(value = "/payment-groups")
    public ResponseEntity<PaymentGroupFeeDto> addNewFee(@Valid @RequestBody PaymentGroupFeeRequest paymentGroupFeeRequest) {

        String paymentGroupReference = PaymentReference.getInstance().getNext();

        PaymentFee fee = paymentGroupDtoMapper.buildFee(paymentGroupFeeRequest);

        PaymentFeeLink paymentFeeLink = paymentGroupService.addNewFeeWithPaymentGroup(fee, paymentGroupReference);

        return new ResponseEntity<>(paymentGroupDtoMapper.toPaymentGroupFeeDto(paymentFeeLink), HttpStatus.CREATED);
    }


    @ApiOperation(value = "Add new Fee to existing Payment Group", notes = "Add new Fee to existing Payment Group")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Fee added to Payment Group"),
        @ApiResponse(code = 403, message = "Payment group info forbidden"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @PutMapping(value = "/payment-groups/{payment-group-reference}")
    public ResponseEntity<PaymentGroupFeeDto> addNewFeetoPaymentGroup(@PathVariable("payment-group-reference") String paymentGroupReference, @Valid @RequestBody PaymentGroupFeeRequest paymentGroupFeeRequest) {

        PaymentFee fee = paymentGroupDtoMapper.buildFee(paymentGroupFeeRequest);

        PaymentFeeLink paymentFeeLink = paymentGroupService.addNewFeetoExistingPaymentGroup(fee, paymentGroupReference);

        return new ResponseEntity<>(paymentGroupDtoMapper.toPaymentGroupFeeDto(paymentFeeLink), HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(InvalidPaymentGroupReferenceException.class)
    public String return403(InvalidPaymentGroupReferenceException ex) {
        return ex.getMessage();
    }
}
