package uk.gov.justice.payment.api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.payment.api.external.client.dto.GovPayPayment;
import uk.gov.justice.payment.api.external.client.dto.Link;
import uk.gov.justice.payment.api.model.Payment;
import uk.gov.justice.payment.api.repository.PaymentHistoryRepository;
import uk.gov.justice.payment.api.repository.PaymentRepository;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.payment.api.model.Payment.paymentWith;
import static uk.gov.justice.payment.api.model.PaymentHistoryEntry.paymentHistoryEntryWith;
import static uk.gov.justice.payment.api.model.QPayment.payment;

@Service
public class DefaultPaymentService implements PaymentService<Payment>, PaymentSearchService {
    private final ObjectMapper objectMapper;
    private final PaymentService<GovPayPayment> govPayPaymentService;
    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    public DefaultPaymentService(ObjectMapper objectMapper, PaymentService<GovPayPayment> govPayPaymentService, PaymentRepository paymentRepository, PaymentHistoryRepository paymentHistoryRepository) {
        this.objectMapper = objectMapper;
        this.govPayPaymentService = govPayPaymentService;
        this.paymentRepository = paymentRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    @Override
    public Payment create(@NonNull String serviceId,
                          @NonNull String applicationReference,
                          @NonNull Integer amount,
                          @NonNull String email,
                          @NonNull String paymentReference,
                          @NonNull String description,
                          @NonNull String returnUrl) {
        GovPayPayment govPayPayment = govPayPaymentService.create(serviceId, applicationReference, amount, email, paymentReference, description, returnUrl);
        Payment payment = updatePayment(paymentWith().serviceId(serviceId).applicationReference(applicationReference).email(email).build(), govPayPayment);
        updatePaymentHistory(payment, "create", govPayPayment);
        return payment;
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
    public void refund(@NonNull String serviceId, @NonNull String govPayId, @NonNull Integer amount, @NonNull Integer refundAmountAvailable) {
        govPayPaymentService.refund(serviceId, govPayId, amount, refundAmountAvailable);
        retrieveAndUpdatePayment(serviceId, govPayId, "refund");
    }

    private Payment retrieveAndUpdatePayment(String serviceId, String govPayId, String action) {
        GovPayPayment govPayPayment = govPayPaymentService.retrieve(serviceId, govPayId);
        Payment payment = updatePayment(paymentRepository.findByGovPayId(govPayId), govPayPayment);
        updatePaymentHistory(payment, action, govPayPayment);
        return payment;
    }

    private Payment updatePayment(Payment payment, GovPayPayment govPayPayment) {
        payment.setGovPayId(govPayPayment.getPaymentId());
        payment.setAmount(govPayPayment.getAmount());
        payment.setStatus(govPayPayment.getState().getStatus());
        payment.setFinished(govPayPayment.getState().getFinished());
        payment.setPaymentReference(govPayPayment.getReference());
        payment.setDescription(govPayPayment.getDescription());
        payment.setReturnUrl(govPayPayment.getReturnUrl());
        payment.setNextUrl(hrefFor(govPayPayment.getLinks().getNextUrl()));
        payment.setCancelUrl(hrefFor(govPayPayment.getLinks().getCancel()));
        return paymentRepository.save(payment);
    }

    private String hrefFor(Link cancelUrl) {
        return cancelUrl == null ? null : cancelUrl.getHref();
    }

    @SneakyThrows(JsonProcessingException.class)
    private void updatePaymentHistory(Payment payment, String action, GovPayPayment govPayPayment) {
        paymentHistoryRepository.save(paymentHistoryEntryWith()
                .payment(payment)
                .action(action)
                .status(govPayPayment.getState().getStatus())
                .finished(govPayPayment.getState().getFinished())
                .govPayJson(objectMapper.writeValueAsString(govPayPayment))
                .build());
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
