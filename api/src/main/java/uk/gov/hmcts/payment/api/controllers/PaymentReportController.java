package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.scheduler.CardPaymentsReportScheduler;
import uk.gov.hmcts.payment.api.scheduler.CreditAccountPaymentsReportScheduler;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@Api(tags = {"PaymentReportController"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentReportController", description = "Payment Report API")})
public class PaymentReportController {

    private final CardPaymentsReportScheduler cardPaymentsReportScheduler;
    private final CreditAccountPaymentsReportScheduler creditAccountPaymentsReportScheduler;

    @Value("${pba.payments.report.scheduler.enabled:true}")
    private boolean pbaReportsEnabled;
    @Value("${card.payments.report.scheduler.enabled:true}")
    private boolean cardReportsEnabled;

    @Autowired
    public PaymentReportController(CardPaymentsReportScheduler cardPaymentsReportScheduler,
                                   CreditAccountPaymentsReportScheduler creditAccountPaymentsReportScheduler) {
        this.cardPaymentsReportScheduler = cardPaymentsReportScheduler;
        this.creditAccountPaymentsReportScheduler = creditAccountPaymentsReportScheduler;
    }

    @ApiOperation(value = "Send payment reports", notes = "Generate csv reports and email to recipients")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Reports sent")
    })
    @RequestMapping(value = "/payments/email-pay-reports", method = POST)
    public void retrievePayments() {
        if (cardReportsEnabled) {
            cardPaymentsReportScheduler.generateCardPaymentsReportTask();
        }

        if (pbaReportsEnabled) {
            creditAccountPaymentsReportScheduler.generateCreditAccountPaymentsReportTask();
        }
    }
}
