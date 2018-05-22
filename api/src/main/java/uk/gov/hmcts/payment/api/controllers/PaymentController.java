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
import uk.gov.hmcts.fees2.register.data.exceptions.BadRequestException;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.UpdatePaymentRequest;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.util.PaymentMethodUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
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

        try{
            Date fromDate = null, toDate = null;

            if(startDate != null) {

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                sdf.setLenient(false);

                fromDate = sdf.parse(startDate);
                toDate = endDate != null ? sdf.parse(endDate) : sdf.parse(paymentsReportService.getTodaysDate());

                if(!fromDate.before(toDate)) {
                    throw new PaymentException("Invalid input dates");
                }

            }

            return new PaymentsResponse(
                paymentsReportService
                    .findCardPayments(fromDate, toDate, paymentMethodType, ccdCaseNumber)
                    .orElse(Collections.emptyList())
            );


        } catch (ParseException paex) {

            LOG.error("PaymentsReportService - Error while creating card payments csv file." +
                " Error message is " + paex.getMessage() + ". Expected format is dd-mm-yyyy.");

            throw new PaymentException("Input dates parsing exception, valid date format is dd-MM-yyyy");
        }

    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }
}
