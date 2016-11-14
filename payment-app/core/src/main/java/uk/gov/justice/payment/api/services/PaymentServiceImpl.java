package uk.gov.justice.payment.api.services;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.payment.api.domain.PaymentDetails;
import uk.gov.justice.payment.api.json.api.TransactionRecord;
import uk.gov.justice.payment.api.repository.PaymentRepository;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static uk.gov.justice.payment.api.domain.QPaymentDetails.paymentDetails;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public void storePayment(PaymentDetails paymentDetails) {
        paymentRepository.save(paymentDetails);
    }

    public void updatePayment(String paymentId, String status) {
        PaymentDetails paymentRequest = paymentRepository.findByPaymentId(paymentId);
        paymentRequest.setStatus(status);
        paymentRepository.save(paymentRequest);
    }

    @Override
    public List<TransactionRecord> searchPayment(SearchCriteria searchCriteria) {
        BooleanExpression criteria = new CriteriaBuilder()
                .eqIfNotNull(paymentDetails.amount, searchCriteria.getAmount())
                .eqIfNotNull(paymentDetails.paymentReference, searchCriteria.getPaymentReference())
                .eqIfNotNull(paymentDetails.applicationReference, searchCriteria.getApplicationReference())
                .eqIfNotNull(paymentDetails.serviceId, searchCriteria.getServiceId())
                .eqIfNotNull(paymentDetails.description, searchCriteria.getDescription())
                .eqIfNotNull(paymentDetails.status, searchCriteria.getStatus())
                .eqIfNotNull(paymentDetails.email, searchCriteria.getEmail())
                .eqIfNotNull(paymentDetails.createdDate, searchCriteria.getCreatedDate())
                .build();


        return stream(paymentRepository.findAll(criteria).spliterator(), false)
                .map(paymentDetails -> {
                    TransactionRecord transactionRecord = new TransactionRecord();
                    transactionRecord.setAmount(paymentDetails.getAmount());
                    transactionRecord.setApplicationReference(paymentDetails.getApplicationReference());
                    transactionRecord.setCreatedDate(paymentDetails.getCreatedDate());
                    transactionRecord.setDescription(paymentDetails.getDescription());
                    transactionRecord.setPaymentId(paymentDetails.getPaymentId());
                    transactionRecord.setPaymentReference(paymentDetails.getPaymentReference());
                    transactionRecord.setServiceId(paymentDetails.getServiceId());
                    transactionRecord.setEmail(paymentDetails.getEmail());
                    return transactionRecord;
                })
                .collect(toList());
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
