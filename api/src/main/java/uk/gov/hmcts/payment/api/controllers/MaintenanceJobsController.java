package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CardPaymentService;
import uk.gov.hmcts.payment.api.service.PaymentService;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.springframework.web.bind.annotation.RequestMethod.PATCH;

@RestController
@Api(tags = {"MaintenanceJobsController"})
@SwaggerDefinition(tags = {@Tag(name = "MaintenanceJobsController", description = "Maintenance Jobs API")})
public class MaintenanceJobsController {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceJobsController.class);

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
    @Transactional
    public void updatePaymentsStatus() throws ExecutionException, InterruptedException {

        List<Reference> referenceList = paymentService.listInitiatedStatusPaymentsReferences();

        LOG.warn("Found " + referenceList.size() + " references that require an status update");

        long count = referenceList
            .stream()
            .map(Reference::getReference)
            .map(cardPaymentService::retrieve)
            .filter(p -> p != null && p.getPayments() != null && p.getPayments().get(0) != null && p.getPayments().get(0).getStatus() != null)
            .count();

        LOG.warn(count + " payment references were successfully updated");

    }

}
