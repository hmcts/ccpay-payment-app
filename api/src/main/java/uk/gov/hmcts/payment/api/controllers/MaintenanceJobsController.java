package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CardPaymentService;
import uk.gov.hmcts.payment.api.service.PaymentService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static org.springframework.web.bind.annotation.RequestMethod.PATCH;

@RestController
@Api(tags = {"MaintenanceJobsController"})
@SwaggerDefinition(tags = {@Tag(name = "MaintenanceJobsController", description = "Maintenance Jobs API")})
public class MaintenanceJobsController {

    private final PaymentService<PaymentFeeLink, String> paymentService;

    private final CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    @Autowired
    public MaintenanceJobsController(PaymentService<PaymentFeeLink, String> paymentService,
                                     CardPaymentService<PaymentFeeLink, String> cardPaymentService) {
        this.paymentService = paymentService;
        this.cardPaymentService = cardPaymentService;
    }

    @ApiOperation(value = "Update payment status", notes = "Updates the payment status on all gov pay pending card payments")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Reports sent")
    })
    @RequestMapping(value = "/jobs/card-payments-status-update", method = PATCH)
    public void updatePaymentsStatus() throws ExecutionException, InterruptedException {

        ForkJoinPool updatePool = new ForkJoinPool(5);

        updatePool.submit(
            () -> paymentService.listCreatedStatusPaymentsReferences()
                .parallelStream()
                .map(Reference::getReference)
                .forEach(cardPaymentService::retrieve)
        ).get();

    }

}
