package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.audit.AuditRepository;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.PciPalPayment;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannelRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentMethodRepository;
import uk.gov.hmcts.payment.api.model.PaymentProviderRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@Primary
public class UserAwareDelegatingPaymentService implements DelegatingPaymentService<PaymentFeeLink, String> {
    private static final Logger LOG = LoggerFactory.getLogger(UserAwareDelegatingPaymentService.class);

    private final static String PAYMENT_CHANNEL_TELEPHONY = "telephony";
    private final static String PAYMENT_PROVIDER_PCI_PAL = "pci pal";
    private final static String PAYMENT_BY_CARD = "card";
    private final static String PAYMENT_STATUS_CREATED = "created";
    private final static String PAYMENT_METHOD_CARD = "card";

    private final UserIdSupplier userIdSupplier;
    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final DelegatingPaymentService<GovPayPayment, String> delegateGovPay;
    private final DelegatingPaymentService<PciPalPayment, String> delegatePciPal;
    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final PaymentProviderRepository paymentProviderRespository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final Payment2Repository paymentRespository;
    private final ReferenceUtil referenceUtil;
    private final GovPayAuthUtil govPayAuthUtil;
    private final ServiceIdSupplier serviceIdSupplier;
    private final AuditRepository auditRepository;
    private final CallbackService callbackService;

    @Autowired
    private Environment environment;

    private String[] testProfiles = {"embedded", "local", "componenttest"};

    private static final Predicate[] REF = new Predicate[0];

    @Autowired
    public UserAwareDelegatingPaymentService(UserIdSupplier userIdSupplier,
                                             PaymentFeeLinkRepository paymentFeeLinkRepository,
                                             DelegatingPaymentService<GovPayPayment, String> delegateGovPay,
                                             DelegatingPaymentService<PciPalPayment, String> delegatePciPal,
                                             PaymentChannelRepository paymentChannelRepository,
                                             PaymentMethodRepository paymentMethodRepository,
                                             PaymentProviderRepository paymentProviderRepository,
                                             PaymentStatusRepository paymentStatusRepository,
                                             Payment2Repository paymentRespository,
                                             ReferenceUtil referenceUtil,
                                             GovPayAuthUtil govPayAuthUtil,
                                             ServiceIdSupplier serviceIdSupplier,
                                             AuditRepository auditRepository,
                                             CallbackService callbackService) {
        this.userIdSupplier = userIdSupplier;
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.delegateGovPay = delegateGovPay;
        this.delegatePciPal = delegatePciPal;
        this.paymentChannelRepository = paymentChannelRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentProviderRespository = paymentProviderRepository;
        this.paymentStatusRepository = paymentStatusRepository;
        this.paymentRespository = paymentRespository;
        this.referenceUtil = referenceUtil;
        this.govPayAuthUtil = govPayAuthUtil;
        this.serviceIdSupplier = serviceIdSupplier;
        this.auditRepository = auditRepository;
        this.callbackService = callbackService;
    }

    @Override
    @Transactional
    public PaymentFeeLink create(PaymentServiceRequest paymentServiceRequest)
        throws CheckDigitException {
        String paymentReference = referenceUtil.getNext("RC");

        Payment payment;
        if (PAYMENT_CHANNEL_TELEPHONY.equals(paymentServiceRequest.getChannel()) &&
            PAYMENT_PROVIDER_PCI_PAL.equals(paymentServiceRequest.getProvider())) {
            PciPalPayment pciPalPayment = delegatePciPal.create(paymentServiceRequest);
            payment = buildPayment(paymentReference, paymentServiceRequest);
//            fillTransientDetails(payment, pciPalPayment);
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .externalStatus(pciPalPayment.getState().getStatus().toLowerCase())
                .status(PayStatusToPayHubStatus.valueOf(pciPalPayment.getState().getStatus().toLowerCase()).mapedStatus)
                .errorCode(pciPalPayment.getState().getCode())
                .message(pciPalPayment.getState().getMessage())
                .build()));
        } else {
            GovPayPayment govPayPayment = delegateGovPay.create(paymentServiceRequest);
            payment = buildPayment(paymentReference, paymentServiceRequest);
            fillTransientDetails(payment, govPayPayment);
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .externalStatus(govPayPayment.getState().getStatus().toLowerCase())
                .status(PayStatusToPayHubStatus.valueOf(govPayPayment.getState().getStatus().toLowerCase()).mapedStatus)
                .errorCode(govPayPayment.getState().getCode())
                .message(govPayPayment.getState().getMessage())
                .build()));
        }

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(paymentServiceRequest.getPaymentReference())
            .payments(Collections.singletonList(payment))
            .fees(paymentServiceRequest.getFees())
            .build();

        payment.setPaymentLink(paymentFeeLink);

        paymentFeeLink = paymentFeeLinkRepository.save(paymentFeeLink);

        auditRepository.trackPaymentEvent("CREATE_CARD_PAYMENT", payment, paymentServiceRequest.getFees());
        return paymentFeeLink;
    }

    private Payment buildPayment(String paymentReference, PaymentServiceRequest paymentServiceRequest) {
        return Payment.paymentWith().userId(userIdSupplier.get())
            .amount(BigDecimal.valueOf(paymentServiceRequest.getAmount()).movePointRight(2))
            .description(paymentServiceRequest.getDescription()).returnUrl(paymentServiceRequest.getReturnUrl())
            .ccdCaseNumber(paymentServiceRequest.getCcdCaseNumber())
            .caseReference(paymentServiceRequest.getCaseReference()).currency(paymentServiceRequest.getCurrency())
            .siteId(paymentServiceRequest.getSiteId())
            .serviceType(paymentServiceRequest.getServiceType())
            .s2sServiceName(serviceIdSupplier.get())
            .paymentChannel(paymentChannelRepository.findByNameOrThrow(paymentServiceRequest.getChannel()))
            .paymentMethod(paymentMethodRepository.findByNameOrThrow(PAYMENT_METHOD_CARD))
            .paymentProvider(paymentProviderRespository.findByNameOrThrow(paymentServiceRequest.getProvider()))
            .paymentStatus(paymentStatusRepository.findByNameOrThrow(PAYMENT_STATUS_CREATED))
            .reference(paymentReference)
            .serviceCallbackUrl(paymentServiceRequest.getServiceCallbackUrl())
            .build();
    }

    @Override
    @Transactional
    public PaymentFeeLink retrieve(String paymentReference) {

        final Payment payment = findSavedPayment(paymentReference);

        final PaymentFeeLink paymentFeeLink = payment.getPaymentLink();

        String paymentService = payment.getS2sServiceName();

        if (null == paymentService || paymentService.trim().equals("")) {
            LOG.error("Unable to determine the payment service which created this payment-Ref:" + paymentReference);
        }

        paymentService = govPayAuthUtil.getServiceName(serviceIdSupplier.get(), paymentService);

        try {
            GovPayPayment govPayPayment = delegateGovPay.retrieve(payment.getExternalReference(), paymentService);

            fillTransientDetails(payment, govPayPayment);

            // Checking if the gov pay status already exists.
            boolean statusExists = payment.getStatusHistories().stream()
                .map(StatusHistory::getExternalStatus)
                .anyMatch(govPayPayment.getState().getStatus().toLowerCase()::equals);
            LOG.debug("Payment status exists in status history: {}", statusExists);

            if (!statusExists) {

                payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                    .externalStatus(govPayPayment.getState().getStatus())
                    .status(PayStatusToPayHubStatus.valueOf(govPayPayment.getState().getStatus().toLowerCase()).mapedStatus)
                    .errorCode(govPayPayment.getState().getCode())
                    .message(govPayPayment.getState().getMessage())
                    .build()));

                if (payment.getServiceCallbackUrl() != null) {
                    callbackService.callback(paymentFeeLink, payment);
                }
            }
        } catch (GovPayPaymentNotFoundException | NullPointerException pnfe) {
            LOG.error("Gov Pay payment not found id is:{} and govpay id is:{}", payment.getExternalReference(), paymentReference);
        }

        return paymentFeeLink;
    }

    @Override
    public PaymentFeeLink retrieve(String s, String paymentTargetService) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PaymentFeeLink> search(PaymentSearchCriteria searchCriteria) {
        return paymentFeeLinkRepository.findAll(findPayments(searchCriteria));
    }

    private static Specification findPayments(PaymentSearchCriteria searchCriteria) {
        return ((root, query, cb) -> getPredicate(root, cb, searchCriteria));
    }

    private static Predicate getPredicate(
        Root<Payment> root,
        CriteriaBuilder cb,
        PaymentSearchCriteria searchCriteria) {
        List<Predicate> predicates = new ArrayList<>();

        Join<PaymentFeeLink, Payment> paymentJoin = root.join("payments", JoinType.LEFT);

        if (searchCriteria.getPaymentMethod() != null) {
            predicates.add(cb.equal(paymentJoin.get("paymentMethod"), PaymentMethod.paymentMethodWith().name(searchCriteria.getPaymentMethod()).build()));
        }

        Expression<Date> dateUpdatedExpr = cb.function("date_trunc", Date.class, cb.literal("seconds"), paymentJoin.get("dateUpdated"));

        if (searchCriteria.getStartDate() != null && searchCriteria.getEndDate() != null) {
            predicates.add(cb.between(dateUpdatedExpr, searchCriteria.getStartDate(), searchCriteria.getEndDate()));
        } else if (searchCriteria.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(dateUpdatedExpr, searchCriteria.getStartDate()));
        } else if (searchCriteria.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(dateUpdatedExpr, searchCriteria.getEndDate()));
        }

        if (searchCriteria.getCcdCaseNumber() != null) {
            predicates.add(cb.equal(paymentJoin.get("ccdCaseNumber"), searchCriteria.getCcdCaseNumber()));
        }

        if (searchCriteria.getServiceType() != null) {
            predicates.add(cb.equal(paymentJoin.get("serviceType"), searchCriteria.getServiceType()));
        }

        if (searchCriteria.getPbaNumber() != null) {
            predicates.add(cb.equal(paymentJoin.get("pbaNumber"), searchCriteria.getPbaNumber()));
        }

        return cb.and(predicates.toArray(REF));
    }

    private Payment findSavedPayment(@NotNull String paymentReference) {
        return paymentRespository.findByReferenceAndPaymentMethod(paymentReference,
            PaymentMethod.paymentMethodWith().name(PAYMENT_BY_CARD).build()).orElseThrow(PaymentNotFoundException::new);
    }

    private void fillTransientDetails(Payment payment, GovPayPayment govPayPayment) {
        BigDecimal amountInPounds = new BigDecimal(govPayPayment.getAmount());
        amountInPounds = amountInPounds.divide(new BigDecimal(100));
        payment.setAmount(amountInPounds);
        payment.setStatus(govPayPayment.getState().getStatus());
        payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(govPayPayment.getState().getStatus().toLowerCase()).build());
        payment.setFinished(govPayPayment.getState().getFinished());
        payment.setExternalReference(govPayPayment.getPaymentId());
        payment.setDescription(govPayPayment.getDescription());
        payment.setReturnUrl(govPayPayment.getReturnUrl());
        payment.setNextUrl(hrefFor(govPayPayment.getLinks().getNextUrl()));
        payment.setCancelUrl(hrefFor(govPayPayment.getLinks().getCancel()));
        payment.setRefundsUrl(hrefFor(govPayPayment.getLinks().getRefunds()));
    }

//    private void fillTransientDetails(Payment payment, PciPalPayment pciPalPayment) {
//        // TODO to be implemented in upcoming story
//    }

    private String hrefFor(Link url) {
        return url == null ? null : url.getHref();
    }

}
