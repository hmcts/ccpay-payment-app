package uk.gov.justice.payment.api.services;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.payment.api.domain.PaymentDetails;
import uk.gov.justice.payment.api.domain.QPaymentDetails;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.api.TransactionRecord;
import uk.gov.justice.payment.api.json.external.GDSCreatePaymentResponse;
import uk.gov.justice.payment.api.repository.PaymentRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zeeshan on 27/09/2016.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentRepository paymentRepository;

    public void storePayment(CreatePaymentRequest request, GDSCreatePaymentResponse response) {
        PaymentDetails paymentRequest = new PaymentDetails(request,response);
        paymentRepository.save(paymentRequest);
    }

    public void updatePayment(String paymentId, String status) {
        PaymentDetails paymentRequest = paymentRepository.findByPaymentId(paymentId);
        paymentRequest.setStatus(status);
        paymentRepository.save(paymentRequest);
    }

    @Override
    public List<TransactionRecord> searchPayment(SearchCriteria searchCriteria) {
        QPaymentDetails qPaymentDetails = QPaymentDetails.paymentDetails;
        BooleanExpression criteria = null;
        if (searchCriteria.getAmount() != null){
            criteria = qPaymentDetails.amount.eq(searchCriteria.getAmount());
        }
        if (searchCriteria.getPaymentReference() != null ){
            criteria = qPaymentDetails.paymentReference.eq(searchCriteria.getPaymentReference()).and(criteria);
        }
        if (searchCriteria.getApplicationReference() != null ){
            criteria = qPaymentDetails.applicationReference.eq(searchCriteria.getApplicationReference()).and(criteria);
        }

        if (searchCriteria.getApplicationReference() != null ){
            criteria = qPaymentDetails.applicationReference.eq(searchCriteria.getApplicationReference()).and(criteria);
        }
        if (searchCriteria.getServiceId() != null ){
            criteria = qPaymentDetails.serviceId.eq(searchCriteria.getServiceId()).and(criteria);
        }

        if (searchCriteria.getDescription() != null ){
            criteria = qPaymentDetails.description.eq(searchCriteria.getDescription()).and(criteria);
        }
        if (searchCriteria.getStatus() != null ){
            criteria = qPaymentDetails.status.eq(searchCriteria.getStatus()).and(criteria);
        }
        if (searchCriteria.getEmail() != null ){
            criteria = qPaymentDetails.email.eq(searchCriteria.getEmail()).and(criteria);
        }
        if (searchCriteria.getCreatedDate() != null ){
            criteria = qPaymentDetails.createdDate.eq(searchCriteria.getCreatedDate()).and(criteria);
        }
        List<TransactionRecord> list = new ArrayList<>();
        paymentRepository.findAll(criteria).forEach(
                paymentDetails-> {
                        TransactionRecord transactionRecord = new TransactionRecord();
                        transactionRecord.setAmount(paymentDetails.getAmount());
                        transactionRecord.setApplicationReference(paymentDetails.getApplicationReference());
                        transactionRecord.setCreatedDate(paymentDetails.getCreatedDate());
                        transactionRecord.setDescription(paymentDetails.getDescription());
                        transactionRecord.setPaymentId(paymentDetails.getPaymentId());
                        transactionRecord.setPaymentReference(paymentDetails.getPaymentReference());
                        transactionRecord.setServiceId(paymentDetails.getServiceId());
                        transactionRecord.setEmail(paymentDetails.getEmail());
                        list.add(transactionRecord);
                });
        return list;
    }
}
