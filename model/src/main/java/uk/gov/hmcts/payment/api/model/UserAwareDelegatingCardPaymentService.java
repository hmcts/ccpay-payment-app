package uk.gov.hmcts.payment.api.model;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.util.Arrays;
import java.util.List;

@Service
public class UserAwareDelegatingCardPaymentService implements CardPaymentService<PaymentFeeLink, Integer> {

    private final static String PAYMENT_CHANNEL_ONLINE = "online";
    private final static String PAYMENT_PROVIDER_GOVPAY = "gov pay";
    private final static String PAYMENT_STATUS_CREATED = "created";
    private final static String PAYMENT_METHOD_CARD =  "card";

    private final UserIdSupplier userIdSupplier;
    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final CardPaymentService<GovPayPayment, String> delegate;
    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final PaymentProviderRepository paymentProviderRespository;
    private final PaymentMethodRepository paymentMethodRepository;

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
    public PaymentFeeLink create(int amount, @NonNull String paymentReference, @NonNull String description, @NonNull String returnUrl,
                                 String ccdCaseNumber, String caseReference, String currency, String siteId, List<Fee> fees) {

        GovPayPayment govPayPayment = delegate.create(amount, paymentReference, description, returnUrl,
            ccdCaseNumber, caseReference, currency, siteId, fees);

        //Build PaymentLink obj
        Payment payment = Payment.paymentWith().govPayId(govPayPayment.getPaymentId()).userId(userIdSupplier.get()).amount(amount)
                                .description(description).returnUrl(returnUrl).ccdCaseNumber(ccdCaseNumber)
                                .caseReference(caseReference).currency(currency).siteId(siteId)
                                .paymentChannel(paymentChannelRepository.findByNameOrThrow(PAYMENT_CHANNEL_ONLINE))
                                .paymentMethod(paymentMethodRepository.findByNameOrThrow(PAYMENT_METHOD_CARD))
                                .paymentProvider(paymentProviderRespository.findByNameOrThrow(PAYMENT_PROVIDER_GOVPAY))
                                .paymentStatus(paymentStatusRepository.findByNameOrThrow(PAYMENT_STATUS_CREATED))
                                .build();
        fillTransientDetails(payment, govPayPayment);

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference(paymentReference)
            .payments(Arrays.asList(payment))
            .fees(fees)
            .build());

        return paymentFeeLink;
    }

    @Override
    public PaymentFeeLink retrieve(Integer integer) {
        return null;
    }

    @Override
    public void cancel(Integer integer) {

    }

    @Override
    public void refund(Integer integer, int amount, int refundAmountAvailabie) {

    }

    private void fillTransientDetails(Payment payment, GovPayPayment govPayPayment) {
        payment.setAmount(govPayPayment.getAmount());
        payment.setStatus(govPayPayment.getState().getStatus());
        payment.setFinished(govPayPayment.getState().getFinished());
        payment.setReference(govPayPayment.getReference());
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
