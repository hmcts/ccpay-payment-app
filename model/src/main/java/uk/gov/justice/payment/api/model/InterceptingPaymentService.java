package uk.gov.justice.payment.api.model;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.payment.api.external.client.dto.GovPayPayment;
import uk.gov.justice.payment.api.external.client.dto.Link;
import uk.gov.justice.payment.api.model.exceptions.PaymentNotFoundException;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.payment.api.model.QPayment.payment;

@Service
public class InterceptingPaymentService implements PaymentService<Payment>, PaymentSearchService {
    private final PaymentService<GovPayPayment> govPayPaymentService;
    private final PaymentRepository paymentRepository;

    @Autowired
    public InterceptingPaymentService(PaymentService<GovPayPayment> govPayPaymentService, PaymentRepository paymentRepository) {
        this.govPayPaymentService = govPayPaymentService;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment create(@NonNull String serviceId,
                          @NonNull String applicationReference,
                          int amount,
                          @NonNull String email,
                          @NonNull String paymentReference,
                          @NonNull String description,
                          @NonNull String returnUrl) {
        GovPayPayment govPayPayment = govPayPaymentService.create(serviceId, applicationReference, amount, email, paymentReference, description, returnUrl);
        return updatePayment(Payment.paymentWith().serviceId(serviceId).applicationReference(applicationReference).email(email).build(), govPayPayment);
    }

    @Override
    public Payment retrieve(@NonNull String serviceId, @NonNull String govPayId) {
        return retrieveAndUpdatePayment(serviceId, govPayId, "retrieve");
    }

    @Override
    public void cancel(@NonNull String serviceId, @NonNull String govPayId) {
        govPayPaymentService.cancel(serviceId, govPayId);
        retrieveAndUpdatePayment(serviceId, govPayId, "cancel");
    }

    @Override
    public void refund(@NonNull String serviceId, @NonNull String govPayId, int amount, int refundAmountAvailable) {
        govPayPaymentService.refund(serviceId, govPayId, amount, refundAmountAvailable);
        retrieveAndUpdatePayment(serviceId, govPayId, "refund");
    }

    private Payment retrieveAndUpdatePayment(String serviceId, String govPayId, String action) {
        GovPayPayment govPayPayment = govPayPaymentService.retrieve(serviceId, govPayId);
        return updatePayment(paymentRepository.findByGovPayId(govPayId).orElseThrow(PaymentNotFoundException::new), govPayPayment);
    }

    private Payment updatePayment(Payment payment, GovPayPayment govPayPayment) {
        payment.setGovPayId(govPayPayment.getPaymentId());
        payment.setAmount(govPayPayment.getAmount());
        payment.setStatus(govPayPayment.getState().getStatus());
        payment.setFinished(govPayPayment.getState().getFinished());
        payment.setPaymentReference(govPayPayment.getReference());
        payment.setDescription(govPayPayment.getDescription());
        payment.setReturnUrl(govPayPayment.getReturnUrl());
        payment.setSelfUrl(hrefFor(govPayPayment.getLinks().getSelf()));
        payment.setNextUrl(hrefFor(govPayPayment.getLinks().getNextUrl()));
        payment.setCancelUrl(hrefFor(govPayPayment.getLinks().getCancel()));
        payment.setRefundsUrl(hrefFor(govPayPayment.getLinks().getRefunds()));
        return paymentRepository.save(payment);
    }

    private String hrefFor(Link url) {
        return url == null ? null : url.getHref();
    }

    @Override
    public List<Payment> find(@NonNull String serviceId, @NonNull PaymentSearchCriteria searchCriteria) {
        BooleanExpression criteria = new CriteriaBuilder()
                .eqIfNotNull(payment.serviceId, serviceId)
                .eqIfNotNull(payment.amount, searchCriteria.getAmount())
                .eqIfNotNull(payment.paymentReference, searchCriteria.getPaymentReference())
                .eqIfNotNull(payment.applicationReference, searchCriteria.getApplicationReference())
                .eqIfNotNull(payment.description, searchCriteria.getDescription())
                .eqIfNotNull(payment.status, searchCriteria.getStatus())
                .eqIfNotNull(payment.email, searchCriteria.getEmail())
                .build();

        return newArrayList(paymentRepository.findAll(criteria));
    }

    private static class CriteriaBuilder {
        private BooleanExpression criteria = null;

        private <T> CriteriaBuilder eqIfNotNull(SimpleExpression<T> stringPath, T value) {
            if (value != null) {
                criteria = stringPath.eq(value).and(criteria);
            }
            return this;
        }

        BooleanExpression build() {
            return criteria;
        }
    }
}
