package uk.gov.justice.payment.api.model.govpay;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.GovPayPayment;
import uk.gov.justice.payment.api.external.client.dto.Link;
import uk.gov.justice.payment.api.external.client.dto.RefundPaymentRequest;
import uk.gov.justice.payment.api.model.Payment;
import uk.gov.justice.payment.api.model.PaymentRepository;
import uk.gov.justice.payment.api.model.PaymentService;
import uk.gov.justice.payment.api.model.exceptions.PaymentNotFoundException;

@Service
public class GovPayPaymentService implements PaymentService<Payment> {
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
    public Payment create(@NonNull String serviceId,
                          int amount,
                          @NonNull String reference,
                          @NonNull String description,
                          @NonNull String returnUrl) {
        GovPayPayment govPayPayment = govPayClient.createPayment(keyFor(serviceId), new CreatePaymentRequest(amount, reference, description, returnUrl));
        Payment payment = paymentRepository.save(Payment.paymentWith().govPayId(govPayPayment.getPaymentId()).build());
        fillTransientDetails(payment, govPayPayment);
        return payment;
    }

    @Override
    public Payment retrieve(@NonNull String serviceId, @NonNull Integer id) {
        Payment payment = paymentRepository.findById(id).orElseThrow(PaymentNotFoundException::new);
        GovPayPayment govPayPayment = govPayClient.retrievePayment(keyFor(serviceId), payment.getGovPayId());
        fillTransientDetails(payment, govPayPayment);
        return payment;
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

    @Override
    public void cancel(@NonNull String serviceId, @NonNull Integer id) {
        Payment payment = retrieve(serviceId, id);
        govPayClient.cancelPayment(keyFor(serviceId), payment.getCancelUrl());
    }

    @Override
    public void refund(@NonNull String serviceId, @NonNull Integer id, int amount, int refundAmountAvailable) {
        Payment payment = retrieve(serviceId, id);
        govPayClient.refundPayment(keyFor(serviceId), payment.getRefundsUrl(), new RefundPaymentRequest(amount, refundAmountAvailable));
    }

    private String hrefFor(Link url) {
        return url == null ? null : url.getHref();
    }


    private String keyFor(String serviceId) {
        return govPayConfig.getKeyForService(serviceId);
    }
}
