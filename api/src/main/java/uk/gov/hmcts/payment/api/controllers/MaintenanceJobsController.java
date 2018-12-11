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
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.springframework.web.bind.annotation.RequestMethod.PATCH;

@RestController
@Api(tags = {"Maintenance Jobs"}, description = "Maintainance jobs REST API")
@SwaggerDefinition(tags = {@Tag(name = "MaintenanceJobsController", description = "Maintenance Jobs API")})
public class MaintenanceJobsController {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceJobsController.class);

    private final PaymentService<PaymentFeeLink, String> paymentService;

    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @Autowired
    private TopicClientProxy topicClientProxy;

    @Autowired
    public MaintenanceJobsController(PaymentService<PaymentFeeLink, String> paymentService,
                                     DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService) {
        this.paymentService = paymentService;
        this.delegatingPaymentService = delegatingPaymentService;
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

        /* We ask the topic client proxy to keep the reuse the connection to the service bus for the whole batch */
        if(topicClientProxy != null && referenceList.size() > 0) {
            topicClientProxy.setKeepClientAlive(true);
        }

        long count = referenceList
            .stream()
            .map(Reference::getReference)
            .map(delegatingPaymentService::retrieve)
            .filter(p -> p != null && p.getPayments() != null && p.getPayments().get(0) != null && p.getPayments().get(0).getStatus() != null)
            .count();

        LOG.warn(count + " payment references were successfully updated");

        if(topicClientProxy != null) {
            topicClientProxy.setKeepClientAlive(false);
            topicClientProxy.close();
        }

    }

}
