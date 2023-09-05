package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.reports.PaymentsReportFacade;
import uk.gov.hmcts.payment.api.scheduler.Clock;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@RestController
@Tag(name = "PaymentReportController", description = "Payment report REST API")
public class PaymentReportController {

    private static final Logger LOG = getLogger(PaymentReportController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE;

    private final PaymentsReportFacade paymentsReportFacade;
    private final PaymentValidator validator;
    private final Clock clock;

    @Autowired
    public PaymentReportController(PaymentsReportFacade paymentsReportFacade, PaymentValidator validator, Clock clock) {
        this.paymentsReportFacade = paymentsReportFacade;
        this.validator = validator;
        this.clock = clock;
    }

    @Operation(summary = "Email payment csv reports", description = "fetch payments for between dates, enter the date in format YYYY-MM-DD")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reports sent")
    })
    @PostMapping(value = "/jobs/email-pay-reports")
    public void generateAndEmailReport(@RequestParam(name = "payment_method", required = false) Optional<String> paymentMethodType,
                                       @RequestParam(name = "service_name", required = false) Optional<String> serviceType,
                                       @RequestParam(name = "start_date", required = false) Optional<String> startDateString,
                                       @RequestParam(name = "end_date", required = false) Optional<String> endDateString) {


        LOG.info("Inside /jobs/email-pay-reports");
        validator.validate(paymentMethodType, startDateString, endDateString);

        Date fromDate = startDateString.map(s -> clock.atStartOfDay(s, FORMATTER)).orElseGet(clock::getYesterdayDate);
        Date toDate = endDateString.map(s -> clock.atEndOfDay(s, FORMATTER)).orElseGet(clock::getTodayDate);
        String service = serviceType.isPresent() ? serviceType.get() : null;
        PaymentMethodType paymentMethodTypeName = paymentMethodType.map(value -> PaymentMethodType.valueOf(value.toUpperCase())).orElse(null);

        paymentsReportFacade.generateCsvAndSendEmail(fromDate, toDate, paymentMethodTypeName, service);
    }

    @PostMapping(value = "/jobs/duplicate-payment-process")
    public void duplicatePaymentEmailReport(@RequestParam(name = "start_date", required = false) Optional<String> startDateString,
                                            @RequestParam(name = "end_date", required = false) Optional<String> endDateString) {

        LOG.info("Inside /jobs/duplicate-payment-process");
        validator.validate(null, startDateString, endDateString);

        Date fromDate = startDateString.map(s -> clock.atStartOfDay(s, FORMATTER)).orElseGet(clock::getYesterdayDate);
        Date toDate = endDateString.map(s -> clock.atEndOfDay(s, FORMATTER)).orElseGet(clock::getTodayDate);

        paymentsReportFacade.generateDuplicatePaymentCsvAndSendEmail(fromDate, toDate);
    }

}
