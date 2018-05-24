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
    public List<PaymentFeeLink> search(LocalDate startDate, LocalDate endDate, PaymentMethodUtil type) {
        Date fromDateTime = Date.from(startDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        Date toDateTime = Date.from(endDate.atStartOfDay().plusDays(1).minusSeconds(1).atZone(ZoneId.systemDefault()).toInstant());
        return cardPaymentService.search(fromDateTime, toDateTime, type.name());
    }

    private Payment findSavedPayment(@NotNull String paymentReference) {
        return paymentRepository.findByReference(paymentReference).orElseThrow(PaymentNotFoundException::new);
    }
}
