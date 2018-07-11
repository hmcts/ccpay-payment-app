package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.scheduler.Clock;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static uk.gov.hmcts.payment.api.util.PaymentMethodType.CARD;
import static uk.gov.hmcts.payment.api.util.PaymentMethodType.PBA;

@RestController
@Api(tags = {"PaymentReportController"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentReportController", description = "Payment Report API")})
public class PaymentReportController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentReportController.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE;

    private final PaymentsReportService paymentsReportService;
    private final PaymentValidator validator;
    private final Clock clock;

    @Value("${pba.payments.report.scheduler.enabled:true}")
    private boolean pbaReportsEnabled;
    @Value("${card.payments.report.scheduler.enabled:true}")
    private boolean cardReportsEnabled;

    @Autowired
    public PaymentReportController(PaymentsReportService paymentsReportService, PaymentValidator validator, Clock clock) {
        this.paymentsReportService = paymentsReportService;
        this.validator = validator;
        this.clock = clock;
    }

    @ApiOperation(value = "Email payment csv reports", notes  = "fetch payments for between dates, enter the date in format YYYY-MM-DD")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Reports sent")
    })
    @RequestMapping(value = "/payments/email-pay-reports", method = POST)
    public void generateAndEmailReport(@RequestParam(name = "payment_method") String paymentMethodType,
                                       @RequestParam(name = "service_name", required = false) Optional<String> serviceType,
                                       @RequestParam(name = "start_date", required = false) Optional<String> startDateString,
                                       @RequestParam(name = "end_date", required = false) Optional<String> endDateString) {

        validator.validate(Optional.of(paymentMethodType), serviceType, startDateString, endDateString);

        Date fromDate = startDateString.map(s -> clock.atStartOfDay(s, FORMATTER)).orElseGet(clock::getYesterdayDate);
        Date toDate = endDateString.map(s -> clock.atEndOfDay(s, FORMATTER)).orElseGet(clock::getTodayDate);
        String serviceName = serviceType.map(value -> Service.valueOf(value.toUpperCase()).getName()).orElse(null);

        if (CARD.name().equalsIgnoreCase(paymentMethodType) && cardReportsEnabled) {
            paymentsReportService.generateCardPaymentsCsvAndSendEmail(fromDate, toDate, serviceName);
        } else if (PBA.name().equalsIgnoreCase(paymentMethodType) && pbaReportsEnabled) {
            paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(fromDate, toDate, serviceName);
        } else {
            LOG.info("payments report flag is disabled for type :{}. So, system will not send CSV email", paymentMethodType);
        }
    }
}
