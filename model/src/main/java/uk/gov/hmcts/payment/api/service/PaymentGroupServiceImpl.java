package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.exceptions.ServiceRequestReferenceNotFoundException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.util.ServiceRequestCaseUtil;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PaymentGroupServiceImpl implements PaymentGroupService<PaymentFeeLink, String> {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentGroupServiceImpl.class);

    private static final Predicate[] REF = new Predicate[0];

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;

    private final PaymentStatusRepository paymentStatusRepository;

    private static final String PAYMENTS = "payments";
    private static final String FEES = "fees";
    private static final String REMISSIONS = "remissions";
    private static final String CCD_CASE_NUMBER = "ccdCaseNumber";

    @Autowired
    private ServiceRequestCaseUtil serviceRequestCaseUtil;

    public PaymentGroupServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository, PaymentStatusRepository paymentStatusRepository) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.paymentStatusRepository = paymentStatusRepository;
    }

    @Override
    public PaymentFeeLink findByPaymentGroupReference(String paymentGroupReference) {
        return paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference).orElseThrow(() -> new ServiceRequestReferenceNotFoundException("ServiceRequest reference doesn't exist"));
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
    public PaymentFeeLink addNewPaymenttoExistingPaymentGroup(Payment payment, String PaymentGroupReference) {

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference(PaymentGroupReference)
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + PaymentGroupReference + " does not exists."));

        serviceRequestCaseUtil.updateServiceRequestCaseDetails(paymentFeeLink, payment);

        payment.setPaymentStatus(paymentStatusRepository.findByNameOrThrow(payment.getPaymentStatus().getName()));
        payment.setStatus(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).getMappedStatus());
        payment.setStatusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
            .status(paymentStatusRepository.findByNameOrThrow(payment.getPaymentStatus().getName()).getName())
            .build()));
        payment.setPaymentLink(paymentFeeLink);

        if(paymentFeeLink.getPayments() != null){
            paymentFeeLink.getPayments().addAll(Lists.newArrayList(payment));
        } else {
            paymentFeeLink.setPayments(Lists.newArrayList(payment));
        }

        return paymentFeeLink;
    }

    @Override
    @Transactional
    public PaymentFeeLink addNewBulkScanPayment(Payment payment, String PaymentGroupReference) {

        payment.setPaymentStatus(paymentStatusRepository.findByNameOrThrow(payment.getPaymentStatus().getName()));
        payment.setStatus(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).getMappedStatus());
        payment.setStatusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
            .status(paymentStatusRepository.findByNameOrThrow(payment.getPaymentStatus().getName()).getName())
            .build()));

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(PaymentGroupReference)
            .payments(Arrays.asList(payment))
            .build();

        return  paymentFeeLinkRepository.save(serviceRequestCaseUtil.enhanceWithServiceRequestCaseDetails(paymentFeeLink, payment));
    }


    @Override
    public List<PaymentFeeLink> search(String ccdCaseNumber) {

        List<PaymentFeeLink> paymentFeeLinkWithMatchingPayments
            = paymentFeeLinkRepository.findAll(findPaymentGroupsWithMatchingCriteria(ccdCaseNumber, PAYMENTS));

        List<PaymentFeeLink> paymentFeeLinkWithMatchingFees
            = paymentFeeLinkRepository.findAll(findPaymentGroupsWithMatchingCriteria(ccdCaseNumber, FEES));

        List<PaymentFeeLink> paymentFeeLinkWithMatchingRemissions
            = paymentFeeLinkRepository.findAll(findPaymentGroupsWithMatchingCriteria(ccdCaseNumber, REMISSIONS));

        List<PaymentFeeLink> paymentFeeLinks = Stream.of(paymentFeeLinkWithMatchingPayments, paymentFeeLinkWithMatchingFees,
            paymentFeeLinkWithMatchingRemissions)
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());

        paymentFeeLinks.sort(Comparator.comparing(PaymentFeeLink::getDateUpdated));

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

        if (ccdCaseNumber != null && tablejoin.equals(PAYMENTS)) {
            Join<PaymentFeeLink, Payment> paymentJoin = root.join(tablejoin, JoinType.LEFT);
            predicates.add(cb.or(cb.equal(paymentJoin.get(CCD_CASE_NUMBER), ccdCaseNumber),(cb.equal(paymentJoin.get("caseReference"), ccdCaseNumber))));
        } else if (ccdCaseNumber != null && tablejoin.equals(FEES)){
            Join<PaymentFeeLink, PaymentFee> paymentFeeJoin = root.join(tablejoin, JoinType.LEFT);
            predicates.add(cb.equal(paymentFeeJoin.get(CCD_CASE_NUMBER), ccdCaseNumber));
        } else if (ccdCaseNumber != null && tablejoin.equals(REMISSIONS)){
            Join<PaymentFeeLink, Remission> remissionJoin = root.join(tablejoin, JoinType.LEFT);
            predicates.add(cb.equal(remissionJoin.get(CCD_CASE_NUMBER), ccdCaseNumber));
        }

        return cb.and(predicates.toArray(REF));
    }

}
