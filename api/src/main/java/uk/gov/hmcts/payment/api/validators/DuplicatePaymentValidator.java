package uk.gov.hmcts.payment.api.validators;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.exceptions.DuplicatePaymentException;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class DuplicatePaymentValidator {

    private final Payment2Repository paymentRepository;
    private final UserIdSupplier userIdSupplier;
    private final int timeInterval;

    @Autowired
    public DuplicatePaymentValidator(@Value("${duplicate.payment.check.interval.in.minutes:2}") int timeInterval,
                                     Payment2Repository paymentRepository, UserIdSupplier userIdSupplier) {
        this.paymentRepository = paymentRepository;
        this.timeInterval = timeInterval;
        this.userIdSupplier = userIdSupplier;
    }

    public boolean checkDuplication(Payment payment) {
        if (paymentRepository.count(findPayment(payment)) > 0) {
            throw new DuplicatePaymentException("duplicate payment");
        }
        return true;
    }

    private Specification findPayment(Payment payment) {
        return ((root, query, cb) -> getPredicate(root, cb, payment, userIdSupplier.get()));
    }

    private Predicate getPredicate(Root<Payment> root, CriteriaBuilder cb, Payment payment, String userId) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("userId"), userId));
        predicates.add(cb.equal(root.get("amount"), payment.getAmount()));
        predicates.add(cb.equal(root.get("serviceType"), payment.getServiceType()));

        if (payment.getCcdCaseNumber() != null) {
            predicates.add(cb.equal(root.get("ccdCaseNumber"), payment.getCcdCaseNumber()));
        } else {
            predicates.add(cb.equal(root.get("caseReference"), payment.getCaseReference()));
        }
        Date targetTime = DateUtils.addMinutes(new Date(), -1 * timeInterval);
        predicates.add(cb.between(root.get("dateCreated"), targetTime, new Date()));
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
