package uk.gov.hmcts.payment.api.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.v1.model.exceptions.DuplicatePaymentException;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

@Component
@Transactional
public class DuplicatePaymentValidator {

    private static final Logger LOG = LoggerFactory.getLogger(DuplicatePaymentValidator.class);

    private final DuplicateSpecification duplicateSpecification;
    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final int timeInterval;

    private Comparator<PaymentFee> FEE_COMPARATOR  = Comparator.comparing(PaymentFee::getCode)
        .thenComparing(PaymentFee::getVersion)
        .thenComparing(PaymentFee::getVolume, nullsFirst(naturalOrder()))
        .thenComparing(PaymentFee::getCalculatedAmount);

    @Autowired
    public DuplicatePaymentValidator(DuplicateSpecification duplicateSpecification, @Value("${duplicate.payment.check.interval.in.minutes:2}") int timeInterval,
                                     PaymentFeeLinkRepository paymentFeeLinkRepository) {
        this.duplicateSpecification = duplicateSpecification;
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.timeInterval = timeInterval;
    }

    public void checkDuplication(Payment payment, List<PaymentFee> requestFees) {
        List<PaymentFeeLink> dbPayments = paymentFeeLinkRepository.findAll(duplicateSpecification.getBy(payment, timeInterval));
        if (!dbPayments.isEmpty()) {
            boolean sameFees = dbPayments.stream()
                .anyMatch(feePredicate(requestFees));
            if (sameFees) {
                LOG.info("CreditAccountPayment received for ccdCaseNumber : {} PaymentStatus : {} - Duplicate Payment!!!", payment.getCcdCaseNumber(), payment.getPaymentStatus().getName());
                throw new DuplicatePaymentException("duplicate payment");
            }
        }
    }

    private Predicate<PaymentFeeLink> feePredicate(List<PaymentFee> requestFees) {
        return p -> p.getFees().size() == requestFees.size() && IntStream.range(0, requestFees.size())
                    .allMatch(i-> contains(p.getFees(), requestFees.get(i), FEE_COMPARATOR));
    }

    private static <T> boolean contains(List<T> list, T item, Comparator<? super T> comparator) {
        return list.stream().anyMatch(listItem -> comparator.compare(listItem, item) == 0);
    }

}
