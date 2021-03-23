package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.domain.model.OrderBo;
import uk.gov.hmcts.payment.api.domain.model.OrderPaymentBo;
import uk.gov.hmcts.payment.api.domain.service.OrderDomainService;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.dto.order.OrderDto;
import uk.gov.hmcts.payment.api.dto.order.OrderPaymentDto;
import uk.gov.hmcts.payment.api.v1.model.exceptions.*;

import javax.validation.Valid;

@RestController
@Api(tags = {"Order"})
@SwaggerDefinition(tags = {@Tag(name = "OrderController", description = "Order REST API")})
public class OrderController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentGroupController.class);

    private final OrderDomainService orderDomainService;

    @Autowired
    private CreditAccountDtoMapper creditAccountDtoMapper;

    @Autowired
    public OrderController(OrderDomainService orderDomainService){
        this.orderDomainService = orderDomainService;
    }

    @ApiOperation(value = "Add Order with Fees", notes = "Add Order with Fees")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment group with fee(s) created"),
        @ApiResponse(code = 400, message = "Payment group creation failed")
    })
    @PostMapping(value = "/order")
    @Transactional
    public ResponseEntity<OrderBo> create(@Valid @RequestBody OrderDto orderDto) {
        return new ResponseEntity<>(orderDomainService.create(orderDto), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Create credit account payment", notes = "Create credit account payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 403, message = "Payment failed due to insufficient funds or the account being on hold"),
        @ApiResponse(code = 404, message = "Account information could not be found"),
        @ApiResponse(code = 504, message = "Unable to retrieve account information, please try again later"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/order/{order-reference}/credit-account-payment")
    @ResponseBody
    @Transactional
    public ResponseEntity<OrderPaymentBo> createCreditAccountPayment(@PathVariable("order-reference") String orderReference,
                                                                     @Valid @RequestBody OrderPaymentDto orderPaymentDto) throws CheckDigitException {
        // TODO: 12/03/2021 Payment Idempotence Check
        /*
        if(orderDomainService.isDuplicate(orderReference)) {
            throw new DuplicatePaymentException("duplicate payment");
        }
         */
        return new ResponseEntity<>(orderDomainService.addPayments(orderDomainService.find(orderReference), orderPaymentDto), HttpStatus.CREATED);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(InvalidPaymentGroupReferenceException.class)
    public String return403(InvalidPaymentGroupReferenceException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String return400(PaymentNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidFeeRequestException.class)
    public String return400(InvalidFeeRequestException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DuplicatePaymentException.class)
    public String return400DuplicatePaymentException(DuplicatePaymentException ex) {
        return ex.getMessage();
    }
}
