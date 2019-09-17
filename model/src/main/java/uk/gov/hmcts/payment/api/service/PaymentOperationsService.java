package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class PaymentOperationsService implements PaymentService<Payment, String>{

    private final Payment2Repository paymentRespository;

    @Autowired
    public PaymentOperationsService(final Payment2Repository paymentRespository) {
        this.paymentRespository = paymentRespository;
    }

    @Override
    public Payment retrieve(final String s) {
        return null;
    }

    @Override
    public List<Reference> listInitiatedStatusPaymentsReferences() {
        return null;
    }

    @Override
    public List<Payment> search(final PaymentSearchCriteria searchCriteria) {
        return paymentRespository.findAll(constructPaymentSpecification(searchCriteria));
    }

    @Override
    public void updateTelephonyPaymentStatus(final String reference, final String status, final String payload) {

    }

    private static final Specification constructPaymentSpecification(final PaymentSearchCriteria searchCriteria) {
        return ((root, query, cb) -> constructPredicate(root, cb, searchCriteria, query));
    }

    private static Predicate constructPredicate(final Root<Payment> root,
                                                final CriteriaBuilder cb,
                                                final PaymentSearchCriteria searchCriteria,
                                                final CriteriaQuery<?> query) {
        final List<Predicate> predicates = new ArrayList<>();
        final Expression<Date> dateUpdatedExpr = cb.function("date_trunc", Date.class, cb.literal("seconds"), root.get("dateUpdated"));

        if (searchCriteria.getCcdCaseNumber() != null) {
            predicates.add(cb.equal(root.get("ccdCaseNumber"), searchCriteria.getCcdCaseNumber()));
        }
        if (searchCriteria.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(dateUpdatedExpr, searchCriteria.getStartDate()));
        }
        if (searchCriteria.getPaymentMethod() != null) {
            predicates.add(cb.equal(root.get("paymentMethod"), PaymentMethod.paymentMethodWith().name(searchCriteria.getPaymentMethod()).build()));
        }
        if (searchCriteria.getServiceType() != null) {
            predicates.add(cb.equal(root.get("serviceType"), searchCriteria.getServiceType()));
        }
        if (searchCriteria.getPbaNumber() != null) {
            predicates.add(cb.equal(root.get("pbaNumber"), searchCriteria.getPbaNumber()));
        }
        if (searchCriteria.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(dateUpdatedExpr, searchCriteria.getEndDate()));
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }

}
