package uk.gov.hmcts.payment.api.controllers.pcipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.controllers.PaymentExternalAPI;
import uk.gov.hmcts.payment.api.dto.TelephonyCallbackDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import jakarta.validation.Valid;


@RestController
@Tag(name = "Telephony", description = "Telephony Payment REST API")
public class TelephonyController {

    private static final Logger LOG = LoggerFactory.getLogger(TelephonyController.class);

    private final PaymentService<PaymentFeeLink, String> paymentService;

    @Autowired
    public TelephonyController(PaymentService<PaymentFeeLink, String> paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Update payment status with pci-pal call back response",
        description = "pci-pal sends response in application/x-www-form-urlencoded format \n\n" +
            "Example : orderCurrency=&orderAmount=488.50&orderReference=MOJTest1&ppAccountID=1210&transactionResult=SUCCESS \n" +
            "&transactionAuthCode=test123&transactionID=3045021106&transactionResponseMsg=&cardExpiry=1220&cardLast4=9999& \n" +
            "cardType=MASTERCARD&ppCallID=820782890&customData1=MOJTest120190124123432&customData2=MASTERCARD&customData3=CreditCard")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No content"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PaymentExternalAPI
    @PostMapping(path = "/telephony/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity updateTelephonyPaymentStatus(@Valid @ModelAttribute TelephonyCallbackDto callbackDto) {
        LOG.info("Received callback request from pci-apl : {}", callbackDto);
        paymentService.updateTelephonyPaymentStatus(callbackDto.getOrderReference(),
            callbackDto.getTransactionResult().toLowerCase(), callbackDto.toString());
        return ResponseEntity.noContent().build();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return ex.getMessage();
    }

}
