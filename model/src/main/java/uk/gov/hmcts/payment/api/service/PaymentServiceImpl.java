package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService<PaymentFeeLink, String> {

    private final static PaymentStatus CREATED = new PaymentStatus("created", "created");
    private final static PaymentStatus STARTED = new PaymentStatus("started", "Payment started");
    private final static PaymentStatus SUBMITTED = new PaymentStatus("submitted", "Payment submitted");

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
        return paymentRepository.findReferencesByPaymentProviderAndPaymentStatusIn(
            PaymentProvider.GOV_PAY,
            Lists.newArrayList(CREATED, STARTED, SUBMITTED)
        );
    }

    @Override
    public List<PaymentFeeLink> search(LocalDate startDate, LocalDate endDate, String paymentMethod, String serviceType, String ccdCaseNumber) {
        Date fromDateTime = Optional.ofNullable(startDate)
            .map(s -> Date.from(s.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()))
            .orElse(null);
        Date toDateTime = Optional.ofNullable(endDate)
            .map(s -> Date.from(s.atStartOfDay().plusDays(1).minusSeconds(1).atZone(ZoneId.systemDefault()).toInstant()))
            .orElse(null);
        return cardPaymentService.search(fromDateTime, toDateTime, paymentMethod, serviceType, ccdCaseNumber);
    }

    private Payment findSavedPayment(@NotNull String paymentReference) {
        return paymentRepository.findByReference(paymentReference).orElseThrow(PaymentNotFoundException::new);
    }
}
