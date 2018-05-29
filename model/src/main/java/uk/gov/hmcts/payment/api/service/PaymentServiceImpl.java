package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.util.PaymentMethodUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService<PaymentFeeLink, String> {

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
    public List<PaymentFeeLink> search(LocalDate startDate, LocalDate endDate, PaymentMethodUtil type, String ccdCaseNumber) {
        Date fromDateTime = Optional.ofNullable(startDate)
            .map(s -> Date.from(s.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()))
            .orElse(null);
        Date toDateTime = Optional.ofNullable(endDate)
            .map(s -> Date.from(s.atStartOfDay().plusDays(1).minusSeconds(1).atZone(ZoneId.systemDefault()).toInstant()))
            .orElse(null);
        return cardPaymentService.search(fromDateTime, toDateTime, type.name(), ccdCaseNumber);
    }

    private Payment findSavedPayment(@NotNull String paymentReference) {
        return paymentRepository.findByReference(paymentReference).orElseThrow(PaymentNotFoundException::new);
    }
}
