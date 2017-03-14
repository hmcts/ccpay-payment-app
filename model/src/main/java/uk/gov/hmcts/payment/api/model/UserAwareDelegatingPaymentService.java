package uk.gov.hmcts.payment.api.model;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.model.exceptions.PaymentNotFoundException;

@Service
public class UserAwareDelegatingPaymentService implements PaymentService<Payment, Integer> {
    private final UserIdSupplier userIdSupplier;
    private final PaymentRepository paymentRepository;
    private final PaymentService<GovPayPayment, String> delegate;

    @Autowired
    public UserAwareDelegatingPaymentService(UserIdSupplier userIdSupplier, PaymentRepository paymentRepository, PaymentService<GovPayPayment, String> delegate) {
        this.userIdSupplier = userIdSupplier;
        this.paymentRepository = paymentRepository;
        this.delegate = delegate;
    }

    @Override
    public Payment create(int amount,
                          @NonNull String reference,
                          @NonNull String description,
                          @NonNull String returnUrl) {
        GovPayPayment govPayPayment = delegate.create(amount, reference, description, returnUrl);
        Payment payment = paymentRepository.save(Payment.paymentWith().govPayId(govPayPayment.getPaymentId()).userId(userIdSupplier.get()).build());
        fillTransientDetails(payment, govPayPayment);
        return payment;
    }

    @Override
    public Payment retrieve(@NonNull Integer paymentId) {
        Payment payment = findSavedPayment(paymentId);
        GovPayPayment govPayPayment = delegate.retrieve(payment.getGovPayId());
        fillTransientDetails(payment, govPayPayment);
        return payment;
    }

    @Override
    public void cancel(@NonNull Integer id) {
        Payment payment = findSavedPayment(id);
        delegate.cancel(payment.getGovPayId());
    }

    @Override
    public void refund(@NonNull Integer id, int amount, int refundAmountAvailable) {
        Payment payment = findSavedPayment(id);
        delegate.refund(payment.getGovPayId(), amount, refundAmountAvailable);
    }

    private Payment findSavedPayment(@NonNull Integer paymentId) {
        return paymentRepository.findByUserIdAndId(userIdSupplier.get(), paymentId).orElseThrow(PaymentNotFoundException::new);
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
