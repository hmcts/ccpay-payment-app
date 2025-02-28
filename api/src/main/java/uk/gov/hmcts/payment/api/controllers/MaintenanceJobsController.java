package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;

import java.util.List;

@RestController
@Tag(name = "MaintenanceJobsController", description = "Maintainance jobs REST API")
public class MaintenanceJobsController {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceJobsController.class);

    private final PaymentService<PaymentFeeLink, String> paymentService;

    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    private final FeePayApportionService feePayApportionService;

    @Autowired
    private ReferenceUtil referenceUtil;

    @Autowired
    private TopicClientProxy topicClientProxy;

    @Autowired
    public MaintenanceJobsController(PaymentService<PaymentFeeLink, String> paymentService,
                                     DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService,
                                     FeePayApportionService feePayApportionService) {
        this.paymentService = paymentService;
        this.delegatingPaymentService = delegatingPaymentService;
        this.feePayApportionService = feePayApportionService;
    }

    @Operation(summary = "Update payment status", description = "Updates the payment status on all gov pay pending card payments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reports sent")
    })
    @PatchMapping(value = "/jobs/card-payments-status-update")
    public void updatePaymentsStatus() {

        List<Reference> referenceList = paymentService.listInitiatedStatusPaymentsReferences();

        LOG.warn("Found {} references that require an status update and the payment reference list is : {}",
                referenceList.size(), referenceList);

        /* We ask the topic client proxy to keep the reuse the connection to the service bus for the whole batch */
        if(topicClientProxy != null && !referenceList.isEmpty()) {
            topicClientProxy.setKeepClientAlive(true);
        }

        long count = referenceList.stream()
            .filter(reference -> {
                try {
                    PaymentFeeLink paymentFeeLink = delegatingPaymentService.retrieveWithCallBack(reference.getReference());
                    return paymentFeeLink != null && paymentFeeLink.getPayments() != null && paymentFeeLink.getPayments().get(0) != null && paymentFeeLink.getPayments().get(0).getStatus() != null;
                } catch (Exception e) {
                    LOG.error("Error while updating payment status for reference {}", reference.getReference(), e);
                    return false;
                }
            })
            .count();

        LOG.warn("{} payment references were successfully updated", count);

        if(topicClientProxy != null) {
            topicClientProxy.setKeepClientAlive(false);
            topicClientProxy.close();
        }
    }
}
