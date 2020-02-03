package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Date;
import java.util.List;


@Service
public class PaymentServiceImpl implements PaymentService<PaymentFeeLink, String> {

    private static final PaymentStatus SUCCESS = new PaymentStatus("success", "success");
    private static final PaymentStatus FAILED = new PaymentStatus("failed", "failed");
    private static final PaymentStatus CANCELLED = new PaymentStatus("cancelled", "cancelled");
    private static final PaymentStatus ERROR = new PaymentStatus("error", "error");
    private static final String PCI_PAL = "pci pal";

    private final Payment2Repository paymentRepository;
    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;
    private final CallbackService callbackService;
    private final PaymentStatusRepository paymentStatusRepository;
    private final TelephonyRepository telephonyRepository;

    @Value("${callback.payments.cutoff.time.in.minutes:2}")
    private int paymentsCutOffTime;

    @Autowired
    public PaymentServiceImpl(@Qualifier("loggingPaymentService") DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService,
                              Payment2Repository paymentRepository, CallbackService callbackService, PaymentStatusRepository paymentStatusRepository,
                              TelephonyRepository telephonyRepository) {
        this.paymentRepository = paymentRepository;
        this.delegatingPaymentService = delegatingPaymentService;
        this.callbackService = callbackService;
        this.paymentStatusRepository = paymentStatusRepository;
        this.telephonyRepository = telephonyRepository;
    }

    @Override
    public PaymentFeeLink retrieve(String reference) {
        Payment payment = findSavedPayment(reference);

        return payment.getPaymentLink();
    }

    @Override
    @Transactional
    public void updateTelephonyPaymentStatus(String paymentReference, String status, String payload) {
        Payment payment = paymentRepository.findByReferenceAndPaymentProvider(paymentReference,
            PaymentProvider.paymentProviderWith().name(PCI_PAL).build()).orElseThrow(PaymentNotFoundException::new);
        if(payment.getPaymentStatus() != null && !payment.getPaymentStatus().equals(PaymentStatus.SUCCESS)){
            payment.setPaymentStatus(paymentStatusRepository.findByNameOrThrow(status));

            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(status)
                .build()));
            if (payment.getServiceCallbackUrl() != null) {
                callbackService.callback(payment.getPaymentLink(), payment);
            }
            telephonyRepository.save(TelephonyCallback.telephonyCallbackWith().paymentReference(paymentReference).payload(payload).build());
        }
    }

    @Override
    public List<Payment> getPayments(Date atStartOfDay, Date atEndOfDay) {
        return paymentRepository.findAllByDateCreatedBetween(atStartOfDay, atEndOfDay).orElse(Collections.EMPTY_LIST);
    }

    @Override
    public List<Reference> listInitiatedStatusPaymentsReferences() {
        Date targetTime = DateUtils.addMinutes(new Date(), -1 * paymentsCutOffTime);
        return paymentRepository.findReferencesByPaymentProviderAndPaymentStatusNotInAndDateCreatedLessThan(
            PaymentProvider.GOV_PAY,
            Lists.newArrayList(SUCCESS, FAILED, ERROR, CANCELLED), targetTime
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
