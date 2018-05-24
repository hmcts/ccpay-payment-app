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
import uk.gov.hmcts.payment.api.dto.mapper.CardPaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static uk.gov.hmcts.payment.api.util.PaymentMethodUtil.valueOf;

@RestController
@Api(tags = {"PaymentController"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentController", description = "Payment API")})
public class PaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

    private final PaymentService<PaymentFeeLink, String> paymentService;
    private final PaymentsReportService paymentsReportService;
    private final CardPaymentDtoMapper cardPaymentDtoMapper;
    private final PaymentValidator validator;

    @Autowired
    public PaymentController(PaymentService<PaymentFeeLink, String> paymentService, PaymentsReportService paymentsReportService, CardPaymentDtoMapper cardPaymentDtoMapper, PaymentValidator paymentValidator) {
        this.paymentService = paymentService;
        this.paymentsReportService = paymentsReportService;
        this.cardPaymentDtoMapper = cardPaymentDtoMapper;
        this.validator = paymentValidator;
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
    public PaymentsResponse retrievePayments(@RequestParam(name = "start_date", required = false) Optional<String> startDateString,
                                             @RequestParam(name = "end_date", required = false) Optional<String> endDateString ,
                                             @RequestParam(name = "payment_method", required = false, defaultValue = "ALL") String paymentMethodType) {

        validator.validate(paymentMethodType, startDateString, endDateString);

        LocalDate startDate = startDateString.map(date -> LocalDate.parse(date, formatter)).orElse(LocalDate.now().minusDays(1));
        LocalDate endDate = endDateString.map(date -> LocalDate.parse(date, formatter)).orElse(LocalDate.now());

        List<PaymentFeeLink>  paymentFeeLinks = paymentService.search(startDate, endDate, valueOf(paymentMethodType.toUpperCase()));
        List<PaymentDto> paymentDto = paymentFeeLinks.stream()
            .map(cardPaymentDtoMapper::toReconciliationResponseDto).collect(Collectors.toList());

        return new PaymentsResponse(paymentsReportService.enrichWithFeeData(paymentDto));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }
}
