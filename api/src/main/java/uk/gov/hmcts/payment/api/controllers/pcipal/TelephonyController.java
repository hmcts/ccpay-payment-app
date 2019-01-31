package uk.gov.hmcts.payment.api.controllers.pcipal;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.controllers.PaymentExternalAPI;
import uk.gov.hmcts.payment.api.dto.TelephonyCallbackDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;


@RestController
@Api(tags = {"Telephony"})
@SwaggerDefinition(tags = {@Tag(name = "TelephonyController", description = "Telephony Payment REST API")})
public class TelephonyController {

    private static final Logger LOG = LoggerFactory.getLogger(TelephonyController.class);

    private final PaymentService<PaymentFeeLink, String> paymentService;

    @Autowired
    public TelephonyController(PaymentService<PaymentFeeLink, String> paymentService) {
        this.paymentService = paymentService;
    }

    @ApiOperation(value = "Update payment status with pci-pal call back response", notes = "pci-pal sends response in application/x-www-form-urlencoded format")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No content"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @PaymentExternalAPI
    @PostMapping(path = "/telephony/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity updateTelephonyPaymentStatus(@ModelAttribute TelephonyCallbackDto callbackDto) {
        LOG.info("Received callback request from pci-apl : {}", callbackDto);
        paymentService.updatePaymentStatus(callbackDto.getOrderReference(), callbackDto.getTransactionResult().toLowerCase());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return ex.getMessage();
    }

}
