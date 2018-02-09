package uk.gov.hmcts.payment.api.model;

import lombok.NonNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;

@Service
public class UserAwareDelegatingCardPaymentService implements CardPaymentService<PaymentFeeLink, String> {

    private final static String PAYMENT_CHANNEL_ONLINE = "online";
    private final static String PAYMENT_PROVIDER_GOVPAY = "gov pay";
    private final static String PAYMENT_STATUS_CREATED = "created";
    private final static String PAYMENT_METHOD_CARD =  "card";
    private final static String PAYMENT_REF_REGEX = "(?<=\\G.{4})";

    private final UserIdSupplier userIdSupplier;
    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final CardPaymentService<GovPayPayment, String> delegate;
    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final PaymentProviderRepository paymentProviderRespository;
    private final PaymentMethodRepository paymentMethodRepository;

    private static final Predicate[] REF = new Predicate[0];

    @Autowired
    public UserAwareDelegatingCardPaymentService(UserIdSupplier userIdSupplier, PaymentFeeLinkRepository paymentFeeLinkRepository,
                                                 CardPaymentService<GovPayPayment, String> delegate, PaymentChannelRepository paymentChannelRepository,
                                                 PaymentMethodRepository paymentMethodRepository, PaymentProviderRepository paymentProviderRepository,
                                                 PaymentStatusRepository paymentStatusRepository) {
        this.userIdSupplier = userIdSupplier;
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.delegate = delegate;
        this.paymentChannelRepository = paymentChannelRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentProviderRespository = paymentProviderRepository;
        this.paymentStatusRepository = paymentStatusRepository;
    }

    @Override
    public PaymentFeeLink create(int amount, @NonNull String paymentGroupReference, @NonNull String description, @NonNull String returnUrl,
                                 String ccdCaseNumber, String caseReference, String currency, String siteId, String serviceType, List<Fee> fees) {
        String paymentReference = generatePaymentReference();

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

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference(paymentGroupReference)
            .payments(Arrays.asList(payment))
            .fees(fees)
            .build());

        return paymentFeeLink;
    }

    @Override
    @Transactional
    public PaymentFeeLink retrieve(String paymentReference) {
        PaymentFeeLink paymentFeeLink = findSavedPayment(paymentReference);
        Payment payment = paymentFeeLink.getPayments().get(0);

        GovPayPayment govPayPayment = delegate.retrieve(payment.getExternalReference());

        fillTransientDetails(payment, govPayPayment);
        return paymentFeeLink;
    }

    @Override
    public void cancel(String paymentReference) {

    }

    @Override
    public void refund(String paymentReference, int amount, int refundAmountAvailabie) {

    }

    @Override
    @Transactional
    public List<PaymentFeeLink> search(Date startDate, Date endDate) {
        List<PaymentFeeLink> paymentFeeLinks = paymentFeeLinkRepository.findAll(findByDatesBetween(startDate, endDate));

        // For each payment get the gov pay status.
        // commented b'coz the not efficient to make govPay calls for each payment in reconciliation.

//        paymentFeeLinks.stream().forEach(p -> {
//            Payment payment = p.getPayments().get(0);
//            GovPayPayment govPayPayment = delegate.retrieve(payment.getGovPayId());
//
//            fillTransientDetails(payment, govPayPayment);
//        });

        return paymentFeeLinks;
    }

    private static Specification findByDatesBetween(Date fromDate, Date toDate) {
        return Specifications
            .where(isBetween(fromDate, toDate));
    }

    private static Specification isBetween(Date startDate, Date endDate) {

        return ((root, query, cb) -> cb.between(root.get("dateCreated"), startDate, endDate));
    }


    private PaymentFeeLink findSavedPayment(@NotNull String paymentReference) {
        return paymentFeeLinkRepository.findByPaymentReference(paymentReference)
            .orElseThrow(PaymentNotFoundException::new);
    }

    private void fillTransientDetails(Payment payment, GovPayPayment govPayPayment) {
        BigDecimal amountInPounds = new BigDecimal(govPayPayment.getAmount());
        amountInPounds = amountInPounds.divide(new BigDecimal(100));
        payment.setAmount(amountInPounds);
        payment.setStatus(govPayPayment.getState().getStatus());
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

    private String generatePaymentReference() {
        DateTime dateTime = new DateTime(DateTimeZone.UTC);
        long timeInMillis = dateTime.getMillis()/100;

        StringBuffer sb = new StringBuffer();
        sb.append(timeInMillis);

        // append the random 4 characters
        Random random = new Random();
        sb.append(String.format("%04d", random.nextInt(10000)));
        sb.append("C");

        String[] parts = sb.toString().split(PAYMENT_REF_REGEX);

        return "RC-" + String.join("-", parts);
    }
}
