package uk.gov.hmcts.payment.api.controllers.pcipal;

import io.swagger.annotations.*;
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

import javax.validation.Valid;


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

    @ApiOperation(value = "Update payment status with pci-pal call back response",
        notes = "pci-pal sends response in application/x-www-form-urlencoded format \n\n" +
            "Example : orderCurrency=&orderAmount=488.50&orderReference=MOJTest1&ppAccountID=1210&transactionResult=SUCCESS \n" +
            "&transactionAuthCode=test123&transactionID=3045021106&transactionResponseMsg=&cardExpiry=1220&cardLast4=9999& \n" +
            "cardType=MASTERCARD&ppCallID=820782890&customData1=MOJTest120190124123432&customData2=MASTERCARD&customData3=CreditCard")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No content"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @PaymentExternalAPI
    @PostMapping(path = "/telephony/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity updateTelephonyPaymentStatus(@Valid @ModelAttribute TelephonyCallbackDto callbackDto) {
        LOG.info("Received callback request from pci-apl : {}", callbackDto);
        paymentService.updateTelephonyPaymentStatus(callbackDto.getOrderReference(),
            callbackDto.getTransactionResult().toLowerCase(), callbackDto.toString());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return ex.getMessage();
    }

}
