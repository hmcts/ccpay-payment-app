package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.scheduler.CardPaymentsReportScheduler;
import uk.gov.hmcts.payment.api.scheduler.CreditAccountPaymentsReportScheduler;
import uk.gov.hmcts.payment.api.scheduler.Clock;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@Api(tags = {"PaymentReportController"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentReportController", description = "Payment Report API")})
public class PaymentReportController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentReportController.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE;

    private final CardPaymentsReportScheduler cardPaymentsReportScheduler;
    private final CreditAccountPaymentsReportScheduler creditAccountPaymentsReportScheduler;
    private final Clock clock;

    @Value("${pba.payments.report.scheduler.enabled:true}")
    private boolean pbaReportsEnabled;
    @Value("${card.payments.report.scheduler.enabled:true}")
    private boolean cardReportsEnabled;

    @Autowired
    public PaymentReportController(CardPaymentsReportScheduler cardPaymentsReportScheduler,
                                   CreditAccountPaymentsReportScheduler creditAccountPaymentsReportScheduler, Clock clock) {
        this.cardPaymentsReportScheduler = cardPaymentsReportScheduler;
        this.creditAccountPaymentsReportScheduler = creditAccountPaymentsReportScheduler;
        this.clock = clock;
    }

    @ApiOperation(value = "Email payment csv reports", notes  = "fetch payments for between dates, enter the date in format YYYY-MM-DD")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Reports sent")
    })
    @RequestMapping(value = "/payments/email-pay-reports", method = POST)
    public void retrievePayments(@RequestParam(name = "start_date", required = false) Optional<String> startDateString,
                                 @RequestParam(name = "end_date", required = false) Optional<String> endDateString) {

        Date fromDate = startDateString.map(s -> clock.atStartOfDay(s, FORMATTER)).orElseGet(clock::getYesterdayDate);
        Date toDate = endDateString.map(s -> clock.atEndOfDay(s, FORMATTER)).orElseGet(clock::getTodayDate);

        if (cardReportsEnabled) {
            cardPaymentsReportScheduler.generateCardPaymentsReportTask(fromDate, toDate);
        } else {
            LOG.info("Card payments report is disabled");
        }

        if (pbaReportsEnabled) {
            creditAccountPaymentsReportScheduler.generateCreditAccountPaymentsReportTask(fromDate, toDate);
        } else {
            LOG.info("Pba credit account payments report is disabled");
        }
    }


}
