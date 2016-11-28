package uk.gov.justice.payment.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.payment.api.configuration.GovPayConfig;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.GovPayment;
import uk.gov.justice.payment.api.external.client.dto.Link;
import uk.gov.justice.payment.api.external.client.dto.RefundPaymentRequest;
import uk.gov.justice.payment.api.model.Payment;
import uk.gov.justice.payment.api.repository.PaymentRepository;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.payment.api.model.Payment.paymentWith;
import static uk.gov.justice.payment.api.model.QPayment.payment;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final ObjectMapper objectMapper;
    private final GovPayConfig govPayConfig;
    private final GovPayClient govPayClient;
    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentServiceImpl(ObjectMapper objectMapper, GovPayConfig govPayConfig, GovPayClient govPayClient, PaymentRepository paymentRepository) {
        this.objectMapper = objectMapper;
        this.govPayConfig = govPayConfig;
        this.govPayClient = govPayClient;
        this.paymentRepository = paymentRepository;
    }

    @Override
//    @SneakyThrows(JsonProcessingException.class)
    public Payment create(@NonNull String serviceId,
                          @NonNull Integer amount,
                          @NonNull String email,
                          @NonNull String applicationReference,
                          @NonNull String paymentReference,
                          @NonNull String description,
                          @NonNull String returnUrl) {
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(amount, applicationReference, description, returnUrl);
        GovPayment govGovPayment = govPayClient.createPayment(keyFor(serviceId), paymentRequest);

        Link nextUrl = govGovPayment.getLinks().getNextUrl();
        Link cancelUrl = govGovPayment.getLinks().getCancel();

        return paymentRepository.save(paymentWith()
                .serviceId(serviceId)
                .govPayId(govGovPayment.getPaymentId())
                .amount(amount)
                .email(email)
                .status(govGovPayment.getState().getStatus())
                .finished(govGovPayment.getState().getFinished())
                .applicationReference(applicationReference)
                .paymentReference(paymentReference)
                .description(description)
                .returnUrl(returnUrl)
                .nextUrl(hrefFor(nextUrl))
                .cancelUrl(hrefFor(cancelUrl))
                .build());
    }

    @Override
    public Payment findByPaymentId(@NonNull String serviceId, @NonNull String paymentId) {
        GovPayment govPayment = govPayClient.getPayment(keyFor(serviceId), paymentId);
        Link nextLink = govPayment.getLinks().getNextUrl();
        Link cancelLink = govPayment.getLinks().getCancel();

        Payment payment = paymentRepository.findByGovPayId(paymentId);
        payment.setStatus(govPayment.getState().getStatus());
        payment.setFinished(govPayment.getState().getFinished());
        payment.setNextUrl(hrefFor(nextLink));
        payment.setCancelUrl(hrefFor(cancelLink));

        return paymentRepository.save(payment);
    }

    private String hrefFor(Link cancelUrl) {
        return cancelUrl == null ? null : cancelUrl.getHref();
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
