package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.ff4j.FF4j;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@Api(tags = {"Payment"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentOperationsController", description = "Payment Search REST API")})
public class PaymentOperationsController {

    private final PaymentService<Payment, String> paymentService;
    private final FF4j ff4j;
    private final PaymentValidator paymentValidator;
    private final DateTimeFormatter formatter;
    private final PaymentDtoMapper paymentDtoMapper;

    @Autowired
    public PaymentOperationsController(@Qualifier("paymentOperationsService") final PaymentService paymentService,
                                       final FF4j ff4j,
                                       final PaymentValidator paymentValidator,
                                       final DateUtil dateUtil,
                                       final PaymentDtoMapper paymentDtoMapper) {
        this.paymentService = paymentService;
        this.ff4j = ff4j;
        this.paymentValidator = paymentValidator;
        this.formatter = dateUtil.getIsoDateTimeFormatter();
        this.paymentDtoMapper = paymentDtoMapper;
    }

    @ApiOperation(value = "Get payments for between dates", notes = "Get list of payments. You can optionally provide start date and end dates which can include times as well. Following are the supported date/time formats. These are yyyy-MM-dd, dd-MM-yyyy," +
        "yyyy-MM-dd HH:mm:ss, dd-MM-yyyy HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss, dd-MM-yyyy'T'HH:mm:ss")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping(value = "/payments1")
    @PaymentExternalAPI
    public PaymentsResponse retrieveAllPayments(@RequestParam(name = "start_date", required = false) Optional<String> startDateTimeString,
                                                @RequestParam(name = "end_date", required = false) Optional<String> endDateTimeString,
                                                @RequestParam(name = "payment_method", required = false) Optional<String> paymentMethodType,
                                                @RequestParam(name = "service_name", required = false) Optional<String> serviceType,
                                                @RequestParam(name = "ccd_case_number", required = false) String ccdCaseNumber,
                                                @RequestParam(name = "pba_number", required = false) String pbaNumber
    ) {

        if (!ff4j.check("payment-search")) {
            throw new PaymentException("Payment search feature is not available for usage.");
        }

        paymentValidator.validate(paymentMethodType, serviceType, startDateTimeString, endDateTimeString);

        final Date fromDateTime = Optional.ofNullable(startDateTimeString.map(formatter::parseLocalDateTime).orElse(null))
            .map(LocalDateTime::toDate)
            .orElse(null);

        final Date toDateTime = Optional.ofNullable(endDateTimeString.map(formatter::parseLocalDateTime).orElse(null))
            .map(s -> fromDateTime != null && s.getHourOfDay() == 0 ? s.plusDays(1).minusSeconds(1).toDate() : s.toDate())
            .orElse(null);

        final List<Payment> payments = paymentService
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
        final List<PaymentDto> paymentDtos = populatePaymentDtos(payments);
        return new PaymentsResponse(paymentDtos);
    }

    private List<PaymentDto> populatePaymentDtos(final List<Payment> payments) {
        final List<PaymentDto> paymentDtos = new ArrayList<>();
        for (final Payment payment: payments) {
            final PaymentFeeLink paymentFeeLink = payment.getPaymentLink();
            final String paymentReference = paymentFeeLink.getPaymentReference();
            final List<PaymentFee> fees = paymentFeeLink.getFees();
            final PaymentDto paymentDto = paymentDtoMapper.toReconciliationResponseDtoForLibereta(payment, paymentReference, fees,ff4j);
            paymentDtos.add(paymentDto);
        }
        return paymentDtos;
    }
}
