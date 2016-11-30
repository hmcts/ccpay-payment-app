package uk.gov.justice.payment.api.services;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.payment.api.configuration.GovPayConfig;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.GovPayPayment;
import uk.gov.justice.payment.api.external.client.dto.RefundPaymentRequest;

@Service
public class GovPayPaymentService implements PaymentService<GovPayPayment> {
    private final GovPayConfig govPayConfig;
    private final GovPayClient govPayClient;

    @Autowired
    public GovPayPaymentService(GovPayConfig govPayConfig, GovPayClient govPayClient) {
        this.govPayConfig = govPayConfig;
        this.govPayClient = govPayClient;
    }

    @Override
    public GovPayPayment create(@NonNull String serviceId,
                                @NonNull String applicationReference,
                                @NonNull Integer amount,
                                @NonNull String email,
                                @NonNull String paymentReference,
                                @NonNull String description,
                                @NonNull String returnUrl) {
        return govPayClient.createPayment(keyFor(serviceId), new CreatePaymentRequest(amount, paymentReference, description, returnUrl));
    }

    @Override
    public GovPayPayment retrieve(@NonNull String serviceId, @NonNull String id) {
        return govPayClient.retrievePayment(keyFor(serviceId), id);
    }

    @Override
    public void cancel(String serviceId, String paymentId) {
        govPayClient.cancelPayment(keyFor(serviceId), paymentId);
    }

    @Override
    public void refund(String serviceId, String paymentId, Integer amount, Integer refundAmountAvailable) {
        govPayClient.refundPayment(keyFor(serviceId), paymentId, new RefundPaymentRequest(amount, refundAmountAvailable));
    }

    private String keyFor(String serviceId) {
        return govPayConfig.getKeyForService(serviceId);
    }
}
