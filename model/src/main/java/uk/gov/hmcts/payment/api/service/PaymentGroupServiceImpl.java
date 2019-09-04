package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;

import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PaymentGroupServiceImpl implements PaymentGroupService<PaymentFeeLink, String> {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentGroupServiceImpl.class);

    private static final Predicate[] REF = new Predicate[0];

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;

    private final PaymentStatusRepository paymentStatusRepository;

    public PaymentGroupServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository, PaymentStatusRepository paymentStatusRepository) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.paymentStatusRepository = paymentStatusRepository;
    }

    @Override
    public PaymentFeeLink findByPaymentGroupReference(String paymentGroupReference) {
        return paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference).orElseThrow(InvalidPaymentGroupReferenceException::new);
    }

    @Override
    public PaymentFeeLink addNewFeeWithPaymentGroup(PaymentFeeLink feeLink) {
        return paymentFeeLinkRepository.save(feeLink);
    }

    @Override
    @Transactional
    public PaymentFeeLink addNewFeetoExistingPaymentGroup(List<PaymentFee> fees, String paymentGroupReference) {

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference)
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + paymentGroupReference + " does not exists."));

        paymentFeeLink.getFees().addAll(fees);

        fees.stream().forEach(fee -> fee.setPaymentLink(paymentFeeLink));

        return paymentFeeLink;
    }

    @Override
    @Transactional
    public PaymentFeeLink addNewPaymenttoExistingPaymentGroup(Payment payment, String PaymentGroupReference) {

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference(PaymentGroupReference)
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + PaymentGroupReference + " does not exists."));

        payment.setPaymentStatus(paymentStatusRepository.findByNameOrThrow(payment.getPaymentStatus().getName()));
        payment.setStatus(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).getMappedStatus());
        payment.setPaymentLink(paymentFeeLink);

        if(paymentFeeLink.getPayments() != null){
            paymentFeeLink.getPayments().addAll(Lists.newArrayList(payment));
        } else {
            paymentFeeLink.setPayments(Lists.newArrayList(payment));
        }

        return paymentFeeLink;
    }


    @Override
    public List<PaymentFeeLink> search(String ccdCaseNumber) {

        List<PaymentFeeLink> paymentFeeLinkWithMatchingPayments
            = paymentFeeLinkRepository.findAll(findPaymentGroupsWithMatchingCriteria(ccdCaseNumber, "payments"));

        List<PaymentFeeLink> paymentFeeLinkWithMatchingFees
            = paymentFeeLinkRepository.findAll(findPaymentGroupsWithMatchingCriteria(ccdCaseNumber, "fees"));

        List<PaymentFeeLink> paymentFeeLinkWithMatchingRemissions
            = paymentFeeLinkRepository.findAll(findPaymentGroupsWithMatchingCriteria(ccdCaseNumber, "remissions"));

        List<PaymentFeeLink> paymentFeeLinks = Stream.of(paymentFeeLinkWithMatchingPayments, paymentFeeLinkWithMatchingFees,
            paymentFeeLinkWithMatchingRemissions)
            .flatMap(Collection::stream)
            .distinct().collect(Collectors.toList());

        return paymentFeeLinks;

    }

    private static Specification findPaymentGroupsWithMatchingCriteria(String ccdCaseNumber, String table) {
        return ((root, query, cb) -> getPredicate(root, cb, ccdCaseNumber, table));
    }

    private static Predicate getPredicate(
        Root<PaymentFeeLink> root,
        CriteriaBuilder cb,
        String ccdCaseNumber, String tablejoin) {
        List<Predicate> predicates = new ArrayList<>();

        if (ccdCaseNumber != null & tablejoin.equals("payments")) {
            Join<PaymentFeeLink, Payment> paymentJoin = root.join(tablejoin, JoinType.LEFT);
            predicates.add(cb.or(cb.equal(paymentJoin.get("ccdCaseNumber"), ccdCaseNumber),(cb.equal(paymentJoin.get("caseReference"), ccdCaseNumber))));
        } else if (ccdCaseNumber != null & tablejoin.equals("fees")){
            Join<PaymentFeeLink, PaymentFee> paymentFeeJoin = root.join(tablejoin, JoinType.LEFT);
            predicates.add(cb.equal(paymentFeeJoin.get("ccdCaseNumber"), ccdCaseNumber));
        } else if (ccdCaseNumber != null & tablejoin.equals("remissions")){
            Join<PaymentFeeLink, Remission> remissionJoin = root.join(tablejoin, JoinType.LEFT);
            predicates.add(cb.equal(remissionJoin.get("ccdCaseNumber"), ccdCaseNumber));
        }

        return cb.and(predicates.toArray(REF));
    }

}
