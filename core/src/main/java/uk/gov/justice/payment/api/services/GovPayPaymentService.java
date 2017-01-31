package uk.gov.justice.payment.api.services;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.payment.api.configuration.GovPayConfig;
import uk.gov.justice.payment.api.exceptions.PaymentNotFoundException;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.GovPayPayment;
import uk.gov.justice.payment.api.external.client.dto.RefundPaymentRequest;
import uk.gov.justice.payment.api.model.Payment;
import uk.gov.justice.payment.api.repository.PaymentRepository;

@Service
public class GovPayPaymentService implements PaymentService<GovPayPayment> {
    private final PaymentRepository paymentRepository;
    private final GovPayConfig govPayConfig;
    private final GovPayClient govPayClient;

    @Autowired
    public GovPayPaymentService(PaymentRepository paymentRepository, GovPayConfig govPayConfig, GovPayClient govPayClient) {
        this.paymentRepository = paymentRepository;
        this.govPayConfig = govPayConfig;
        this.govPayClient = govPayClient;
    }

    @Override
    public GovPayPayment create(@NonNull String serviceId,
                                @NonNull String applicationReference,
                                int amount,
                                @NonNull String email,
                                @NonNull String paymentReference,
                                @NonNull String description,
                                @NonNull String returnUrl) {
        return govPayClient.createPayment(keyFor(serviceId), new CreatePaymentRequest(amount, paymentReference, description, returnUrl));
    }

    @Override
    public GovPayPayment retrieve(@NonNull String serviceId, @NonNull String govPayId) {
        Payment payment = paymentFor(govPayId);
        return govPayClient.retrievePayment(keyFor(serviceId), payment.getSelfUrl());
    }

    @Override
    public void cancel(@NonNull String serviceId, @NonNull String govPayId) {
        Payment payment = paymentFor(govPayId);
        govPayClient.cancelPayment(keyFor(serviceId), payment.getCancelUrl());
    }

    @Override
    public void refund(@NonNull String serviceId, @NonNull String govPayId, int amount, int refundAmountAvailable) {
        Payment payment = paymentFor(govPayId);
        govPayClient.refundPayment(keyFor(serviceId), payment.getRefundsUrl(), new RefundPaymentRequest(amount, refundAmountAvailable));
    }

    private Payment paymentFor(@NonNull String govPayId) {
        return paymentRepository.findByGovPayId(govPayId).orElseThrow(PaymentNotFoundException::new);
    }

    private String keyFor(String serviceId) {
        return govPayConfig.getKeyForService(serviceId);
    }
}
