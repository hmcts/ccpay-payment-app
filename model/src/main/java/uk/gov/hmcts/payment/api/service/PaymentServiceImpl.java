package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.constraints.NotNull;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService<PaymentFeeLink, String> {

    private final static PaymentStatus SUCCESS = new PaymentStatus("success", "success");
    private final static PaymentStatus FAILED = new PaymentStatus("failed", "failed");
    private final static PaymentStatus CANCELLED = new PaymentStatus("cancelled", "cancelled");
    private final static PaymentStatus ERROR = new PaymentStatus("error", "error");

    private final Payment2Repository paymentRepository;
    private final CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    @Autowired
    public PaymentServiceImpl(@Qualifier("loggingCardPaymentService") CardPaymentService<PaymentFeeLink, String> cardPaymentService,
                              Payment2Repository paymentRepository) {
        this.paymentRepository = paymentRepository;
        this.cardPaymentService = cardPaymentService;
    }

    @Override
    public PaymentFeeLink retrieve(String reference) {
        Payment payment = findSavedPayment(reference);

        return payment.getPaymentLink();
    }

    @Override
    public List<Reference> listInitiatedStatusPaymentsReferences() {
        return paymentRepository.findReferencesByPaymentProviderAndPaymentStatusNotIn(
            PaymentProvider.GOV_PAY,
            Lists.newArrayList(SUCCESS, FAILED, ERROR, CANCELLED)
        );
    }

    @Override
    public List<PaymentFeeLink> search(LocalDateTime startDate, LocalDateTime endDate, String paymentMethod, String serviceType, String ccdCaseNumber) {
        Date fromDateTime = Optional.ofNullable(startDate)
            .map(s -> s.toDate())
            .orElse(null);
        Date toDateTime = Optional.ofNullable(endDate)
            .map(s -> fromDateTime != null && fromDateTime.compareTo(s.toDate()) == 0 ? s.plusDays(1).minusSeconds(1).toDate() : s.toDate())
            .orElse(null);
        return cardPaymentService.search(fromDateTime, toDateTime, paymentMethod, serviceType, ccdCaseNumber);
    }

    private Payment findSavedPayment(@NotNull String paymentReference) {
        return paymentRepository.findByReference(paymentReference).orElseThrow(PaymentNotFoundException::new);
    }
}
