package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.ff4j.FF4j;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.UpdatePaymentRequest;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentEvent;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentNoUpdate;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.model.StatusHistoryNoUpdate;
import uk.gov.hmcts.payment.api.service.CallbackService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Api(tags = {"Payment"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentController", description = "Payment REST API")})
public class PaymentController {

    private final PaymentService<PaymentFeeLink, String> paymentService;
    private final CallbackService callbackService;
    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentDtoMapper paymentDtoMapper;
    private final PaymentValidator validator;
    private final FF4j ff4j;

    private final DateUtil dateUtil;
    private final DateTimeFormatter formatter;

    @Autowired
    public PaymentController(PaymentService<PaymentFeeLink, String> paymentService,
                             PaymentStatusRepository paymentStatusRepository, CallbackService callbackService,
                             PaymentDtoMapper paymentDtoMapper, PaymentValidator paymentValidator, FF4j ff4j,
                             DateUtil dateUtil) {
        this.paymentService = paymentService;
        this.callbackService = callbackService;
        this.paymentStatusRepository = paymentStatusRepository;
        this.paymentDtoMapper = paymentDtoMapper;
        this.validator = paymentValidator;
        this.ff4j = ff4j;
        this.dateUtil = dateUtil;
        this.formatter = dateUtil.getIsoDateTimeFormatter();
    }

    @ApiOperation(value = "Update case reference by payment reference", notes = "Update case reference by payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "No content"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @PatchMapping(value = "/payments/{reference}")
    @Transactional
    public ResponseEntity updateCaseReference(@PathVariable("reference") String reference,
                                              @RequestBody @Validated UpdatePaymentRequest request) {
        Optional<PaymentNoUpdate> payment = getPaymentNoUpdateByReference(reference);

        if (payment.isPresent()) {
            if (request.getCaseReference() != null) {
                payment.get().setCaseReference(request.getCaseReference());
            }
            if (request.getCcdCaseNumber() != null) {
                payment.get().setCcdCaseNumber(request.getCcdCaseNumber());
            }

            if (payment.get().getStatusHistories() != null) {
                payment.get().getStatusHistories().add(StatusHistoryNoUpdate.statusHistoryWith().payment(payment.get()).eventName(PaymentEvent.CASE_REF_UPDATE).build());
            } else {
                payment.get().setStatusHistories(Collections.singletonList(StatusHistoryNoUpdate.statusHistoryWith().eventName(PaymentEvent.CASE_REF_UPDATE).build()));
            }

            payment.get()

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @ApiOperation(value = "Get payments for between dates", notes = "Get list of payments. You can optionally provide start date and end dates which can include times as well. Following are the supported date/time formats. These are yyyy-MM-dd, dd-MM-yyyy," +
        "yyyy-MM-dd HH:mm:ss, dd-MM-yyyy HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss, dd-MM-yyyy'T'HH:mm:ss")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping(value = "/payments")
    @PaymentExternalAPI
    public PaymentsResponse retrievePayments(@RequestParam(name = "start_date", required = false) Optional<String> startDateTimeString,
                                             @RequestParam(name = "end_date", required = false) Optional<String> endDateTimeString,
                                             @RequestParam(name = "payment_method", required = false) Optional<String> paymentMethodType,
                                             @RequestParam(name = "service_name", required = false) Optional<String> serviceType,
                                             @RequestParam(name = "ccd_case_number", required = false) String ccdCaseNumber,
                                             @RequestParam(name = "pba_number", required = false) String pbaNumber
    ) {

        if (!ff4j.check("payment-search")) {
            throw new PaymentException("Payment search feature is not available for usage.");
        }

        validator.validate(paymentMethodType, serviceType, startDateTimeString, endDateTimeString);

        Date fromDateTime = Optional.ofNullable(startDateTimeString.map(formatter::parseLocalDateTime).orElse(null))
            .map(LocalDateTime::toDate)
            .orElse(null);

        Date toDateTime = Optional.ofNullable(endDateTimeString.map(formatter::parseLocalDateTime).orElse(null))
            .map(s -> fromDateTime != null && s.getHourOfDay() == 0 ? s.plusDays(1).minusSeconds(1).toDate() : s.toDate())
            .orElse(null);

        List<PaymentFeeLink> paymentFeeLinks = paymentService
            .search(
                PaymentSearchCriteria
                    .searchCriteriaWith()
                    .startDate(fromDateTime)
                    .endDate(toDateTime)
                    .ccdCaseNumber(ccdCaseNumber)
                    .pbaNumber(pbaNumber)
                    .paymentMethod(paymentMethodType.map(value -> PaymentMethodType.valueOf(value.toUpperCase()).getType()).orElse(null))
                    .serviceType(serviceType.map(value -> Service.valueOf(value.toUpperCase()).getName()).orElse(null))
                    .build()
            );

        List<PaymentDto> paymentDto = paymentFeeLinks.stream()
            .map(paymentDtoMapper::toReconciliationResponseDto).collect(Collectors.toList());

        return new PaymentsResponse(paymentDto);

    }

    @ApiOperation(value = "Update payment status by payment reference", notes = "Update payment status by payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No content"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @PaymentExternalAPI
    @PatchMapping("/payments/{reference}/status/{status}")
    @Transactional
    public ResponseEntity updatePaymentStatus(@PathVariable(value = "reference", required = true) String reference,
                                              @PathVariable(value = "status", required = true) String status) {
        Optional<Payment> payment = getPaymentByReference(reference);

        if (payment.isPresent()) {
            payment.get().setPaymentStatus(paymentStatusRepository.findByNameOrThrow(status));

            if (payment.get().getStatusHistories() != null) {
                payment.get().getStatusHistories().add(StatusHistory.statusHistoryWith().eventName(PaymentEvent.STATUS_CHANGE).build());
            } else {
                payment.get().setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith().eventName(PaymentEvent.STATUS_CHANGE).build()));
            }

            if (payment.get().getServiceCallbackUrl() != null) {
                callbackService.callback(payment.get().getPaymentLink(), payment.get());
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @ApiOperation(value = "Get payment details by payment reference", notes = "Get payment details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 403, message = "Payment info forbidden"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @GetMapping(value = "/payments/{reference}")
    public PaymentDto retrievePayment(@PathVariable("reference") String paymentReference) {
        PaymentFeeLink paymentFeeLink = paymentService.retrieve(paymentReference);
        return Optional.ofNullable(paymentFeeLink)
            .map(paymentDtoMapper::toReconciliationResponseDto)
            .orElseThrow(PaymentNotFoundException::new);
    }

    private Optional<Payment> getPaymentByReference(String reference) {
        PaymentFeeLink paymentFeeLink = paymentService.retrieve(reference);
        return paymentFeeLink.getPayments().stream()
            .filter(p -> p.getReference().equals(reference)).findAny();
    }

    private Optional<PaymentNoUpdate> getPaymentNoUpdateByReference(String reference) {
        PaymentFeeLink paymentFeeLink = paymentService.retrieve(reference);
        Optional<Payment> payment = paymentFeeLink.getPayments().stream()
            .filter(p -> p.getReference().equals(reference)).findAny();
        if (payment.isPresent()) {
            return Optional.ofNullable(PaymentNoUpdate.fromPayment(payment.get()));
        } else {
            return Optional.empty();
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }
}
