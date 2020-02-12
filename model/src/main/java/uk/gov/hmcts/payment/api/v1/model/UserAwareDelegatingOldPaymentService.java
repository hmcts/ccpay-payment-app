package uk.gov.hmcts.payment.api.v1.model;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

@Service
public class UserAwareDelegatingOldPaymentService implements PaymentService<PaymentOld, Integer> {
    private final UserIdSupplier userIdSupplier;
    private final PaymentRepository paymentRepository;
    private final PaymentService<GovPayPayment, String> delegate;

    @Autowired
    public UserAwareDelegatingOldPaymentService(UserIdSupplier userIdSupplier, PaymentRepository paymentRepository, PaymentService<GovPayPayment, String> delegate) {
        this.userIdSupplier = userIdSupplier;
        this.paymentRepository = paymentRepository;
        this.delegate = delegate;
    }

    @Override
    public PaymentOld create(int amount, @NonNull String reference, @NonNull String description, @NonNull String returnUrl, String language) {
        GovPayPayment govPayPayment = delegate.create(amount, reference, description, returnUrl, language);
        PaymentOld paymentOld = paymentRepository.save(PaymentOld.paymentWith().govPayId(govPayPayment.getPaymentId()).userId(userIdSupplier.get()).build());
        fillTransientDetails(paymentOld, govPayPayment);
        return paymentOld;
    }

    @Override
    public PaymentOld retrieve(@NonNull Integer paymentId) {
        PaymentOld paymentOld = findSavedPayment(paymentId);
        GovPayPayment govPayPayment = delegate.retrieve(paymentOld.getGovPayId());
        fillTransientDetails(paymentOld, govPayPayment);
        return paymentOld;
    }

    @Override
    public void cancel(@NonNull Integer id) {
        PaymentOld paymentOld = findSavedPayment(id);
        delegate.cancel(paymentOld.getGovPayId());
    }

    @Override
    public void refund(@NonNull Integer id, int amount, int refundAmountAvailable) {
        PaymentOld paymentOld = findSavedPayment(id);
        delegate.refund(paymentOld.getGovPayId(), amount, refundAmountAvailable);
    }

    private PaymentOld findSavedPayment(@NonNull Integer paymentId) {
        return paymentRepository
            .findByUserIdAndId(userIdSupplier.get(), paymentId)
            .orElseThrow(PaymentNotFoundException::new);
    }

    private void fillTransientDetails(PaymentOld paymentOld, GovPayPayment govPayPayment) {
        paymentOld.setAmount(govPayPayment.getAmount());
        paymentOld.setStatus(govPayPayment.getState().getStatus());
        paymentOld.setFinished(govPayPayment.getState().getFinished());
        paymentOld.setReference(govPayPayment.getReference());
        paymentOld.setDescription(govPayPayment.getDescription());
        paymentOld.setReturnUrl(govPayPayment.getReturnUrl());
        paymentOld.setNextUrl(hrefFor(govPayPayment.getLinks().getNextUrl()));
        paymentOld.setCancelUrl(hrefFor(govPayPayment.getLinks().getCancel()));
        paymentOld.setRefundsUrl(hrefFor(govPayPayment.getLinks().getRefunds()));
    }

    private String hrefFor(Link url) {
        return url == null ? null : url.getHref();
    }
}
