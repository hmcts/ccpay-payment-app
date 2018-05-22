package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.UpdatePaymentRequest;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.util.PaymentMethodUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;

@RestController
@Api(tags = {"PaymentController"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentController", description = "Payment API")})
public class PaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService<PaymentFeeLink, String> paymentService;
    private final PaymentsReportService paymentsReportService;

    @Autowired
    public PaymentController(PaymentService<PaymentFeeLink, String> paymentService,
                             PaymentsReportService paymentsReportService) {
        this.paymentService = paymentService;
        this.paymentsReportService = paymentsReportService;
    }


    @ApiOperation(value = "Update case reference by payment reference", notes = "Update case reference by payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "No content"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/payments/{reference}", method = PATCH)
    @Transactional
    public ResponseEntity<?> updateCaseReference(@PathVariable("reference") String paymentReference,
                                                 @RequestBody @Validated UpdatePaymentRequest request) {

        PaymentFeeLink paymentFeeLink = paymentService.retrieve(paymentReference);
        Optional<Payment> payment = paymentFeeLink.getPayments().stream()
            .filter(p -> p.getReference().equals(paymentReference)).findAny();

        if (payment.isPresent()) {
            if (request.getCaseReference() != null) {
                payment.get().setCaseReference(request.getCaseReference());
            }
            if (request.getCcdCaseNumber() != null) {
                payment.get().setCcdCaseNumber(request.getCcdCaseNumber());
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @ApiOperation(value = "Get payments for between dates", notes = "Get payments for between dates, enter the date in format dd-MM-yyyy")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @RequestMapping(value = "/payments", method = GET)

    public PaymentsResponse retrievePayments(@RequestParam(name = "start_date", required = false) String startDate,
                                             @RequestParam(name = "end_date", required = false) String endDate,
                                             @RequestParam(name = "payment_method", required = false) String paymentMethodType,
                                             @RequestParam(name = "ccd_case_number", required = false) String ccdCaseNumber) {

        if (ccdCaseNumber != null) {
            return new PaymentsResponse(paymentsReportService.findPaymentsByCcdCaseNumber(ccdCaseNumber).orElse(Collections.emptyList()));
        }

        PaymentMethodUtil paymentMethod = Optional.ofNullable(paymentMethodType)
            .map(p -> PaymentMethodUtil.valueOf(p.toUpperCase()))
            .orElse(PaymentMethodUtil.ALL);

        Optional<List<PaymentDto>> paymentDtos = paymentsReportService.findCardPaymentsBetweenDates(startDate, endDate, paymentMethod.name());

        return new PaymentsResponse(paymentDtos.orElse(Collections.emptyList()));

    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }
}
