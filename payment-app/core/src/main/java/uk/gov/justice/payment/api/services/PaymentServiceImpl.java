package uk.gov.justice.payment.api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.payment.api.configuration.GovPayConfig;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.Link;
import uk.gov.justice.payment.api.external.client.dto.Payment;
import uk.gov.justice.payment.api.model.PaymentDetails;
import uk.gov.justice.payment.api.repository.PaymentRepository;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.payment.api.model.PaymentDetails.paymentDetailsWith;
import static uk.gov.justice.payment.api.model.QPaymentDetails.paymentDetails;

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
    @SneakyThrows(JsonProcessingException.class)
    public PaymentDetails create(@NonNull String serviceId,
                                 @NonNull Integer amount,
                                 @NonNull String email,
                                 @NonNull String applicationReference,
                                 @NonNull String paymentReference,
                                 @NonNull String description,
                                 @NonNull String returnUrl) {
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(amount, applicationReference, description, returnUrl);
        Payment govPayment = govPayClient.createPayment(keyFor(serviceId), paymentRequest);

        Link nextUrl = govPayment.getLinks().getNextUrl();
        Link cancelUrl = govPayment.getLinks().getCancel();

        return paymentRepository.save(paymentDetailsWith()
                .serviceId(serviceId)
                .paymentId(govPayment.getPaymentId())
                .amount(amount)
                .email(email)
                .status(govPayment.getState().getStatus())
                .finished(govPayment.getState().getFinished())
                .applicationReference(applicationReference)
                .paymentReference(paymentReference)
                .description(description)
                .returnUrl(returnUrl)
                .nextUrl(hrefFor(nextUrl))
                .cancelUrl(hrefFor(cancelUrl))
                .response(objectMapper.writeValueAsString(govPayment))
                .createdDate(govPayment.getCreatedDate())
                .build());
    }

    @Override
    public PaymentDetails findByPaymentId(@NonNull String serviceId, @NonNull String paymentId) {
        Payment payment = govPayClient.getPayment(keyFor(serviceId), paymentId);
        Link nextLink = payment.getLinks().getNextUrl();
        Link cancelLink = payment.getLinks().getCancel();

        PaymentDetails paymentDetails = paymentRepository.findByPaymentId(paymentId);
        paymentDetails.setStatus(payment.getState().getStatus());
        paymentDetails.setFinished(payment.getState().getFinished());
        paymentDetails.setNextUrl(hrefFor(nextLink));
        paymentDetails.setCancelUrl(hrefFor(cancelLink));

        return paymentRepository.save(paymentDetails);
    }

    private String hrefFor(Link cancelUrl) {
        return cancelUrl == null ? null : cancelUrl.getHref();
    }

    @Override
    public List<PaymentDetails> search(@NonNull String serviceId, @NonNull PaymentSearchCriteria searchCriteria) {
        BooleanExpression criteria = new CriteriaBuilder()
                .eqIfNotNull(paymentDetails.serviceId, serviceId)
                .eqIfNotNull(paymentDetails.amount, searchCriteria.getAmount())
                .eqIfNotNull(paymentDetails.paymentReference, searchCriteria.getPaymentReference())
                .eqIfNotNull(paymentDetails.applicationReference, searchCriteria.getApplicationReference())
                .eqIfNotNull(paymentDetails.description, searchCriteria.getDescription())
                .eqIfNotNull(paymentDetails.status, searchCriteria.getStatus())
                .eqIfNotNull(paymentDetails.email, searchCriteria.getEmail())
                .eqIfNotNull(paymentDetails.createdDate, searchCriteria.getCreatedDate())
                .build();


        return newArrayList(paymentRepository.findAll(criteria));
    }

    @Override
    public void cancel(String serviceId, String paymentId) {
        govPayClient.cancelPayment(govPayConfig.getKeyForService(serviceId), paymentId);
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
