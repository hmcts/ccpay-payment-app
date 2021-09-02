package uk.gov.hmcts.payment.api.validators;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class DuplicateSpecification {

    private final UserIdSupplier userIdSupplier;

    @Autowired
    public DuplicateSpecification(UserIdSupplier userIdSupplier) {
        this.userIdSupplier = userIdSupplier;
    }

    public Specification getBy(Payment payment, int timeInterval) {
        return ((root, query, cb) -> getPredicate(root, cb, payment, userIdSupplier.get(), timeInterval));
    }

    private Predicate getPredicate(Root<Payment> root, CriteriaBuilder cb, Payment payment, String userId, int timeInterval) {
        List<Predicate> predicates = new ArrayList<>();
        Join<PaymentFeeLink, Payment> paymentJoin = root.join("payments", JoinType.LEFT);

        predicates.add(cb.equal(paymentJoin.get("userId"), userId));
        predicates.add(cb.equal(paymentJoin.get("amount"), payment.getAmount()));
        predicates.add(cb.equal(paymentJoin.get("serviceType"), payment.getServiceType()));

        if (payment.getCcdCaseNumber() != null) {
            predicates.add(cb.equal(paymentJoin.get("ccdCaseNumber"), payment.getCcdCaseNumber()));
        } else {
            predicates.add(cb.equal(paymentJoin.get("caseReference"), payment.getCaseReference()));
        }

        if (payment.getPaymentChannel() != null) {
            predicates.add(cb.equal(paymentJoin.get("paymentChannel"), payment.getPaymentChannel()));
        }
        Date currentTime = new Date();
        Date targetTime = DateUtils.addMinutes(currentTime, -1 * timeInterval);
        predicates.add(cb.between(paymentJoin.get("dateCreated"), targetTime, currentTime));
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
