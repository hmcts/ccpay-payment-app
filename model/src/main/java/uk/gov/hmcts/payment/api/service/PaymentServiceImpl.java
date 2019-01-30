package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.constraints.NotNull;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService<PaymentFeeLink, String> {

    private final static PaymentStatus SUCCESS = new PaymentStatus("success", "success");
    private final static PaymentStatus FAILED = new PaymentStatus("failed", "failed");
    private final static PaymentStatus CANCELLED = new PaymentStatus("cancelled", "cancelled");
    private final static PaymentStatus ERROR = new PaymentStatus("error", "error");

    private final Payment2Repository paymentRepository;
    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;
    private final CallbackService callbackService;
    private final PaymentStatusRepository paymentStatusRepository;

    @Autowired
    public PaymentServiceImpl(@Qualifier("loggingPaymentService") DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService,
                              Payment2Repository paymentRepository, CallbackService callbackService, PaymentStatusRepository paymentStatusRepository) {
        this.paymentRepository = paymentRepository;
        this.delegatingPaymentService = delegatingPaymentService;
        this.callbackService = callbackService;
        this.paymentStatusRepository = paymentStatusRepository;
    }

    @Override
    public PaymentFeeLink retrieve(String reference) {
        Payment payment = findSavedPayment(reference);

        return payment.getPaymentLink();
    }

    @Override
    @Transactional
    public void updatePaymentStatus(String reference, String status) {
        Payment payment = findSavedPayment(reference);
        payment.setPaymentStatus(paymentStatusRepository.findByNameOrThrow(status));
        if (payment.getServiceCallbackUrl() != null) {
            callbackService.callback(payment.getPaymentLink(), payment);
        }
    }

    @Override
    public List<Reference> listInitiatedStatusPaymentsReferences() {
        return paymentRepository.findReferencesByPaymentProviderAndPaymentStatusNotIn(
            PaymentProvider.GOV_PAY,
            Lists.newArrayList(SUCCESS, FAILED, ERROR, CANCELLED)
        );
    }

    @Override
    public List<PaymentFeeLink> search(PaymentSearchCriteria searchCriteria) {
        return delegatingPaymentService.search(searchCriteria);
    }

    private Payment findSavedPayment(@NotNull String paymentReference) {
        return paymentRepository.findByReference(paymentReference).orElseThrow(PaymentNotFoundException::new);
    }
}
