package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.domain.model.OrderPaymentBo;
import uk.gov.hmcts.payment.api.domain.service.OrderDomainService;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.dto.order.OrderDto;
import uk.gov.hmcts.payment.api.dto.order.OrderPaymentDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.exception.LiberataServiceTimeoutException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@RestController
@Api(tags = {"Order"})
@SwaggerDefinition(tags = {@Tag(name = "OrderController", description = "Order REST API")})
public class OrderController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentGroupController.class);

    private final OrderDomainService orderDomainService;

    @Autowired
    private CreditAccountDtoMapper creditAccountDtoMapper;

    @Autowired
    private IdempotencyKeysRepository idempotencyKeysRepository;

    @Autowired
    private Payment2Repository payment2Repository;

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
    public ResponseEntity<String> create(@Valid @RequestBody OrderDto orderDto, @RequestHeader(required = false) MultiValueMap<String, String> headers) {
        return new ResponseEntity<>(orderDomainService.create(orderDto,headers), HttpStatus.CREATED);
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
    public ResponseEntity<OrderPaymentBo> createCreditAccountPayment(@RequestHeader(value = "idempotency_key") String idempotencyKey,
                                                                     @PathVariable("order-reference") String orderReference,
                                                                     @Valid @RequestBody OrderPaymentDto orderPaymentDto) throws CheckDigitException, InterruptedException, JsonProcessingException, JsonMappingException {

        ObjectMapper objectMapper = new ObjectMapper();
        Function<String, Optional<IdempotencyKeys>> getIdempotencyKey = (idempotencyKeyToCheck) -> idempotencyKeysRepository.findByIdempotencyKey(idempotencyKeyToCheck);

        Function<IdempotencyKeys, ResponseEntity> validateHashcodeForRequest = (idempotencyKeys) -> {

            OrderPaymentBo responseBO;
            try {
                if (!idempotencyKeys.getRequest_hashcode().equals(orderPaymentDto.hashCodeWithOrderReference(orderReference))) {
                    return new ResponseEntity("Payment already present for idempotency key with different payment details", HttpStatus.CONFLICT); // 409 if hashcode not matched
                }
                if (idempotencyKeys.getResponseCode() >= 500) {
                    return new ResponseEntity(idempotencyKeys.getResponseBody(), HttpStatus.valueOf(idempotencyKeys.getResponseCode()));
                }
                responseBO = objectMapper.readValue(idempotencyKeys.getResponseBody(), OrderPaymentBo.class);
            } catch (JsonProcessingException e) {
                return new ResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity(responseBO, HttpStatus.valueOf(idempotencyKeys.getResponseCode())); // if hashcode matched
        };

        //Idempotency Check
        Optional<IdempotencyKeys> idempotencyKeysRow = getIdempotencyKey.apply(idempotencyKey);
        if (idempotencyKeysRow.isPresent()) {
            ResponseEntity responseEntity = validateHashcodeForRequest.apply(idempotencyKeysRow.get());
            return responseEntity;
        }

        //business validations for order
        PaymentFeeLink order = businessValidationForOrders(orderDomainService.find(orderReference), orderPaymentDto);

        //PBA Payment
        OrderPaymentBo orderPaymentBo = null;
        ResponseEntity responseEntity;
        String responseJson;
        try {
            orderPaymentBo = orderDomainService.addPayments(order, orderPaymentDto);
            responseEntity = new ResponseEntity<>(orderPaymentBo, HttpStatus.CREATED);
            responseJson = objectMapper.writeValueAsString(orderPaymentBo);
        } catch (LiberataServiceTimeoutException liberataServiceTimeoutException) {
            responseEntity = new ResponseEntity<>(liberataServiceTimeoutException.getMessage(), HttpStatus.GATEWAY_TIMEOUT);
            responseJson = liberataServiceTimeoutException.getMessage();
        }

        //Create Idempotency Record
        return createIdempotencyRecord(objectMapper, idempotencyKey, orderReference, responseJson, responseEntity, orderPaymentDto);
    }

    private PaymentFeeLink businessValidationForOrders(PaymentFeeLink order, OrderPaymentDto orderPaymentDto) {
        //Business validation for amount
        BigDecimal totalCalculatedAmount = order.getFees().stream().map(paymentFee -> paymentFee.getCalculatedAmount()).reduce(BigDecimal::add).get();
        if (!(totalCalculatedAmount.compareTo(orderPaymentDto.getAmount()) == 0)) {
            throw new OrderException("Payment amount not matching with fees");
        }

        //Business validation for amount due for fees
        Optional<List<Payment>> orderPaymentsOptional =  payment2Repository.findByPaymentLinkId(order.getId());

        if(orderPaymentsOptional.isPresent() && orderPaymentsOptional.get().size() > 0) {
            BigDecimal totalPaymentAmount = orderPaymentsOptional.get().stream()
                .filter(payment -> payment.getPaymentStatus().getName().equalsIgnoreCase("success"))
                .map(payment -> payment.getAmount()).reduce(BigDecimal::add).get();


            if (totalPaymentAmount.compareTo(totalCalculatedAmount) == 0) {
                throw new OrderException("No amount due for payment for this Order");
            }
        }

        return order;
    }


    private ResponseEntity createIdempotencyRecord(ObjectMapper objectMapper, String idempotencyKey, String orderReference,
                                                   String responseJson, ResponseEntity responseEntity, OrderPaymentDto orderPaymentDto) throws JsonProcessingException, InterruptedException {
        String requestJson = objectMapper.writeValueAsString(orderPaymentDto);
        int requestHashCode = orderPaymentDto.hashCodeWithOrderReference(orderReference);

        IdempotencyKeys idempotencyRecord = IdempotencyKeys
            .idempotencyKeysWith()
            .idempotencyKey(idempotencyKey)
            .requestBody(requestJson)
            .request_hashcode(requestHashCode)   //save the hashcode
            .responseBody(responseJson)
            .responseCode(responseEntity.getStatusCodeValue())
            .build();

        try {
            Optional<IdempotencyKeys> idempotencyKeysRecord = idempotencyKeysRepository.findById(IdempotencyKeysPK.idempotencyKeysPKWith().idempotencyKey(idempotencyKey).request_hashcode(requestHashCode).build());
            if (idempotencyKeysRecord.isPresent()) {
                return new ResponseEntity(objectMapper.readValue(idempotencyKeysRecord.get().getResponseBody(), OrderPaymentBo.class), HttpStatus.valueOf(idempotencyKeysRecord.get().getResponseCode()));
            }
            idempotencyKeysRepository.save(idempotencyRecord);

        } catch (DataIntegrityViolationException exception) {
            responseEntity = new ResponseEntity<>("First PBA Payment record currently in progress", HttpStatus.TOO_EARLY);
        }

        return responseEntity;
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

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(OrderException.class)
    public String return400(OrderException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(AccountNotFoundException.class)
    public String return404(AccountNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    @ExceptionHandler(AccountServiceUnavailableException.class)
    public String return504(AccountServiceUnavailableException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    @ExceptionHandler(LiberataServiceTimeoutException.class)
    public String return504(LiberataServiceTimeoutException ex) {
        return ex.getMessage();
    }
}
