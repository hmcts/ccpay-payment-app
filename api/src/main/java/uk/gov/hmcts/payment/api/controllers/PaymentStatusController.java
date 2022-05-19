package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.dto.servicerequest.PaymentStatusBouncedCheque;
import uk.gov.hmcts.payment.api.dto.servicerequest.PaymentStatusChargeBack;
import uk.gov.hmcts.payment.api.dto.servicerequest.PaymentStatusUnprocessedPayment;
import uk.gov.hmcts.payment.api.dto.servicerequest.PaymentStatusUpdateSecondDto;

@RestController
@Api(tags = {"Payment Status"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentStatusController", description = "Payment Status REST API")})
public class PaymentStatusController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentStatusController.class);

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "successful operation"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @PaymentExternalAPI
    @PostMapping(path = "/payment-failures/bounced-cheque")
    public ResponseEntity PaymentStatusBouncedCheque(@RequestBody PaymentStatusBouncedCheque paymentStatusBouncedCheque) {
        LOG.info("Received payment status ping 1 request : {}", paymentStatusBouncedCheque);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PaymentExternalAPI
    @PatchMapping(path = "/payment-failures/{failure_reference}")
    public ResponseEntity PaymentStatusPingSecond(@RequestBody PaymentStatusUpdateSecondDto paymentStatusUpdateSecondDto,
                                                  @PathVariable(value = "failure_reference") String failure_reference) {
        LOG.info("Received payment status ping 2 request : {}", paymentStatusUpdateSecondDto);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PaymentExternalAPI
    @PostMapping(path = "/payment-failures/unprocessed-payment")
    public ResponseEntity PaymentStatusUnprocessedPayment(@RequestBody PaymentStatusUnprocessedPayment paymentStatusUnprocessedPayment) {
        LOG.info("Received payment status ping 1 request : {}", paymentStatusUnprocessedPayment);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PaymentExternalAPI
    @PostMapping(path = "/payment-failures/chargeback")
    public ResponseEntity PaymentStatusChargeBack(@RequestBody PaymentStatusChargeBack paymentStatusChargeBack) {
        LOG.info("Received payment status ping 1 request : {}", paymentStatusChargeBack);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
