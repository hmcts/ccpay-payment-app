package uk.gov.hmcts.payment.api.service;

import lombok.NonNull;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
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
public class UserAwareDelegatingCardPaymentService implements CardPaymentService<PaymentFeeLink, String> {
    private static final Logger LOG = LoggerFactory.getLogger(UserAwareDelegatingCardPaymentService.class);

    private final static String PAYMENT_CHANNEL_ONLINE = "online";
    private final static String PAYMENT_PROVIDER_GOVPAY = "gov pay";
    private final static String PAYMENT_BY_ALL = "all";
    private final static String PAYMENT_BY_CARD = "card";
    private final static String PAYMENT_BY_ACCOUNT = "payment by account";
    private final static String PAYMENT_BY_ACCOUNT_SHORT_ALIAS = "pba";
    private final static String PAYMENT_STATUS_CREATED = "created";
    private final static String PAYMENT_METHOD_CARD = "card";

    private final UserIdSupplier userIdSupplier;
    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final CardPaymentService<GovPayPayment, String> delegate;
    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final PaymentProviderRepository paymentProviderRespository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final Payment2Repository paymentRespository;
    private final PaymentReferenceUtil paymentReferenceUtil;
    private final GovPayAuthUtil govPayAuthUtil;
    private final ServiceIdSupplier serviceIdSupplier;

    private static final Predicate[] REF = new Predicate[0];

    @Autowired
    public UserAwareDelegatingCardPaymentService(UserIdSupplier userIdSupplier, PaymentFeeLinkRepository paymentFeeLinkRepository,
                                                 CardPaymentService<GovPayPayment, String> delegate, PaymentChannelRepository paymentChannelRepository,
                                                 PaymentMethodRepository paymentMethodRepository, PaymentProviderRepository paymentProviderRepository,
                                                 PaymentStatusRepository paymentStatusRepository, Payment2Repository paymentRespository,
                                                 PaymentReferenceUtil paymentReferenceUtil, GovPayAuthUtil govPayAuthUtil, ServiceIdSupplier serviceIdSupplier) {
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
    }

    @Override
    @Transactional
    public PaymentFeeLink create(int amount, @NonNull String paymentGroupReference, @NonNull String description, @NonNull String returnUrl,
                                 String ccdCaseNumber, String caseReference, String currency, String siteId, String serviceType, List<PaymentFee> fees) throws CheckDigitException {
        String paymentReference = paymentReferenceUtil.getNext();

        GovPayPayment govPayPayment = delegate.create(amount, paymentReference, description, returnUrl,
            ccdCaseNumber, caseReference, currency, siteId, serviceType, fees);

        //Build PaymentLink obj
        Payment payment = Payment.paymentWith().userId(userIdSupplier.get())
            .amount(BigDecimal.valueOf(amount).movePointRight(2))
            .description(description).returnUrl(returnUrl).ccdCaseNumber(ccdCaseNumber)
            .caseReference(caseReference).currency(currency).siteId(siteId)
            .serviceType(serviceType)
            .paymentChannel(paymentChannelRepository.findByNameOrThrow(PAYMENT_CHANNEL_ONLINE))
            .paymentMethod(paymentMethodRepository.findByNameOrThrow(PAYMENT_METHOD_CARD))
            .paymentProvider(paymentProviderRespository.findByNameOrThrow(PAYMENT_PROVIDER_GOVPAY))
            .paymentStatus(paymentStatusRepository.findByNameOrThrow(PAYMENT_STATUS_CREATED))
            .reference(paymentReference)
            .build();
        fillTransientDetails(payment, govPayPayment);

        payment.setStatusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
            .externalStatus(govPayPayment.getState().getStatus())
            .status(PayStatusToPayHubStatus.valueOf(govPayPayment.getState().getStatus().toLowerCase()).mapedStatus)
            .errorCode(govPayPayment.getState().getCode())
            .message(govPayPayment.getState().getMessage())
            .build()));

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference(paymentGroupReference)
            .payments(Arrays.asList(payment))
            .fees(fees)
            .build());

        return paymentFeeLink;
    }

    @Override
    @Transactional
    public PaymentFeeLink retrieve(String paymentReference) {
        Payment payment = findSavedPayment(paymentReference);

        PaymentFeeLink paymentFeeLink = payment.getPaymentLink();

        String paymentService = payment.getServiceType();

        if (null == paymentService || paymentService.trim().equals("")) {
            LOG.error("Unable to determine the payment service which created this payment-Ref:" + paymentReference);
        }

        String callingService = null;

        try {
            callingService = serviceIdSupplier.get();
        } catch (NullPointerException exp) {
            //where some endpoints are not using S2S, in those scenarios it should continue as is.
        }

        paymentService = govPayAuthUtil.getServiceName(callingService, paymentService);

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
        }

        return paymentFeeLink;
    }

    @Override
    public PaymentFeeLink retrieve(String s, String paymentTargetService) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PaymentFeeLink> search(Date startDate, Date endDate, String paymentMethod, String serviceName, String ccdCaseNumber) {
        return paymentFeeLinkRepository.findAll(findCardPayments(startDate, endDate, paymentMethod, serviceName, ccdCaseNumber));
    }

    private static Specification findCardPayments(Date fromDate, Date toDate, String paymentMethod, String serviceName, String ccdCaseNumber) {
        return ((root, query, cb) -> getPredicate(root, cb, fromDate, toDate, paymentMethod, serviceName, ccdCaseNumber));
    }

    private static Predicate getPredicate(Root<Payment> root, CriteriaBuilder cb, Date fromDate, Date toDate, String paymentMethod, String serviceName, String ccdCaseNumber) {

        List<Predicate> predicates = new ArrayList<>();

        Join<PaymentFeeLink, Payment> paymentJoin = root.join("payments", JoinType.LEFT);

        if (paymentMethod != null) {

            paymentMethod = paymentMethod.toLowerCase();

            if (paymentMethod.equals(PAYMENT_BY_ACCOUNT_SHORT_ALIAS)) {
                predicates.add(cb.equal(paymentJoin.get("paymentMethod"), new PaymentMethod(PAYMENT_BY_ACCOUNT, null)));
            } else if (!paymentMethod.equals(PAYMENT_BY_ALL)) {
                predicates.add(cb.equal(paymentJoin.get("paymentMethod"), new PaymentMethod(paymentMethod, null)));
            }
        }

        if (fromDate != null && toDate != null) {
            predicates.add(cb.between(paymentJoin.get("dateUpdated"), fromDate, toDate));
        } else if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(paymentJoin.get("dateUpdated"), fromDate));
        } else if (toDate != null) {
            predicates.add(cb.lessThanOrEqualTo(paymentJoin.get("dateUpdated"), toDate));
        }

        if (ccdCaseNumber != null) {
            predicates.add(cb.equal(paymentJoin.get("ccdCaseNumber"), ccdCaseNumber));
        }

        if (serviceName != null) {
            predicates.add(cb.equal(paymentJoin.get("serviceType"), serviceName));
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
