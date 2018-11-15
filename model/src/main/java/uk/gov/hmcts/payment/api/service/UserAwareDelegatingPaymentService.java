package uk.gov.hmcts.payment.api.service;

import lombok.NonNull;
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
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;
import uk.gov.hmcts.payment.api.util.PaymentReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;

import javax.persistence.criteria.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Primary
public class UserAwareDelegatingPaymentService implements DelegatingPaymentService<PaymentFeeLink, String> {
    private static final Logger LOG = LoggerFactory.getLogger(UserAwareDelegatingPaymentService.class);

    private final static String PAYMENT_CHANNEL_ONLINE = "online";
    private final static String PAYMENT_PROVIDER_GOVPAY = "gov pay";
    private final static String PAYMENT_BY_CARD = "card";
    private final static String PAYMENT_STATUS_CREATED = "created";
    private final static String PAYMENT_METHOD_CARD = "card";

    private final UserIdSupplier userIdSupplier;
    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final DelegatingPaymentService<GovPayPayment, String> delegate;
    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final PaymentProviderRepository paymentProviderRespository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final Payment2Repository paymentRespository;
    private final PaymentReferenceUtil paymentReferenceUtil;
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
                                             DelegatingPaymentService<GovPayPayment, String> delegate,
                                             PaymentChannelRepository paymentChannelRepository,
                                             PaymentMethodRepository paymentMethodRepository,
                                             PaymentProviderRepository paymentProviderRepository,
                                             PaymentStatusRepository paymentStatusRepository,
                                             Payment2Repository paymentRespository,
                                             PaymentReferenceUtil paymentReferenceUtil,
                                             GovPayAuthUtil govPayAuthUtil,
                                             ServiceIdSupplier serviceIdSupplier,
                                             AuditRepository auditRepository,
                                             CallbackService callbackService) {
        this.userIdSupplier = userIdSupplier;
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.delegate = delegate;
        this.paymentChannelRepository = paymentChannelRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentProviderRespository = paymentProviderRepository;
        this.paymentStatusRepository = paymentStatusRepository;
        this.paymentRespository = paymentRespository;
        this.paymentReferenceUtil = paymentReferenceUtil;
        this.govPayAuthUtil = govPayAuthUtil;
        this.serviceIdSupplier = serviceIdSupplier;
        this.auditRepository = auditRepository;
        this.callbackService = callbackService;
    }

    @Override
    @Transactional
    public PaymentFeeLink create(@NonNull String paymentGroupReference, @NonNull String description, @NonNull String returnUrl, String ccdCaseNumber, String caseReference, String currency, String siteId, String serviceType, List<PaymentFee> fees, int amount, String serviceCallbackUrl) throws CheckDigitException {
        String paymentReference = paymentReferenceUtil.getNext();

        GovPayPayment govPayPayment = delegate.create(paymentReference, description, returnUrl, ccdCaseNumber, caseReference, currency, siteId, serviceType, fees, amount, serviceCallbackUrl);

        //Build PaymentLink obj
        Payment payment = Payment.paymentWith().userId(userIdSupplier.get())
            .amount(BigDecimal.valueOf(amount).movePointRight(2))
            .description(description).returnUrl(returnUrl).ccdCaseNumber(ccdCaseNumber)
            .caseReference(caseReference).currency(currency).siteId(siteId)
            .serviceType(serviceType)
            .s2sServiceName(serviceIdSupplier.get())
            .paymentChannel(paymentChannelRepository.findByNameOrThrow(PAYMENT_CHANNEL_ONLINE))
            .paymentMethod(paymentMethodRepository.findByNameOrThrow(PAYMENT_METHOD_CARD))
            .paymentProvider(paymentProviderRespository.findByNameOrThrow(PAYMENT_PROVIDER_GOVPAY))
            .paymentStatus(paymentStatusRepository.findByNameOrThrow(PAYMENT_STATUS_CREATED))
            .reference(paymentReference)
            .serviceCallbackUrl(serviceCallbackUrl)
            .build();
        fillTransientDetails(payment, govPayPayment);

        payment.setStatusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
            .externalStatus(govPayPayment.getState().getStatus())
            .status(PayStatusToPayHubStatus.valueOf(govPayPayment.getState().getStatus().toLowerCase()).mapedStatus)
            .errorCode(govPayPayment.getState().getCode())
            .message(govPayPayment.getState().getMessage())
            .build()));

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference(paymentGroupReference)
            .payments(Arrays.asList(payment))
            .fees(fees)
            .build();

        payment.setPaymentLink(paymentFeeLink);

        paymentFeeLink = paymentFeeLinkRepository.save(paymentFeeLink);

        auditRepository.trackPaymentEvent("CREATE_CARD_PAYMENT", payment, fees);
        return paymentFeeLink;
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
            GovPayPayment govPayPayment = delegate.retrieve(payment.getExternalReference(), paymentService);

            fillTransientDetails(payment, govPayPayment);

            // Checking if the gov pay status already exists.
            boolean statusExists = payment.getStatusHistories().stream()
                .map(StatusHistory::getExternalStatus)
                .anyMatch(govPayPayment.getState().getStatus().toLowerCase()::equals);
            LOG.debug("Payment status exists in status history: {}", statusExists);

            if (!statusExists) {

                payment.setStatusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                    .externalStatus(govPayPayment.getState().getStatus())
                    .status(PayStatusToPayHubStatus.valueOf(govPayPayment.getState().getStatus().toLowerCase()).mapedStatus)
                    .errorCode(govPayPayment.getState().getCode())
                    .message(govPayPayment.getState().getMessage())
                    .build()));

                if(payment.getServiceCallbackUrl() != null) {
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
    public List<PaymentFeeLink> search(Date startDate, Date endDate, String paymentMethod, String serviceName, String ccdCaseNumber, String pbaNumber) {
        return paymentFeeLinkRepository.findAll(findPayments(startDate, endDate, paymentMethod, serviceName, ccdCaseNumber, pbaNumber));
    }

    private static Specification findPayments(Date fromDate, Date toDate, String paymentMethod, String serviceName, String ccdCaseNumber, String pbaNumber) {
        return ((root, query, cb) -> getPredicate(root, cb, fromDate, toDate, paymentMethod, serviceName, ccdCaseNumber, pbaNumber));
    }

    private static Predicate getPredicate(
        Root<Payment> root,
        CriteriaBuilder cb,
        Date fromDate,
        Date toDate,
        String paymentMethod,
        String serviceName,
        String ccdCaseNumber,
        String pbaNumber) {
        List<Predicate> predicates = new ArrayList<>();

        Join<PaymentFeeLink, Payment> paymentJoin = root.join("payments", JoinType.LEFT);

        if (paymentMethod != null) {
            predicates.add(cb.equal(paymentJoin.get("paymentMethod"), PaymentMethod.paymentMethodWith().name(paymentMethod).build()));
        }

        Expression<Date> dateUpdatedExpr = cb.function("date_trunc", Date.class, cb.literal("seconds"), paymentJoin.get("dateUpdated"));
        if (fromDate != null && toDate != null) {
            predicates.add(cb.between(dateUpdatedExpr, fromDate, toDate));
        } else if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(dateUpdatedExpr, fromDate));
        } else if (toDate != null) {
            predicates.add(cb.lessThanOrEqualTo(dateUpdatedExpr, toDate));
        }

        if (ccdCaseNumber != null) {
            predicates.add(cb.equal(paymentJoin.get("ccdCaseNumber"), ccdCaseNumber));
        }

        if (serviceName != null) {
            predicates.add(cb.equal(paymentJoin.get("serviceType"), serviceName));
        }

        if (pbaNumber != null) {
            predicates.add(cb.equal(paymentJoin.get("pbaNumber"), pbaNumber));
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

    private String hrefFor(Link url) {
        return url == null ? null : url.getHref();
    }

}
