package uk.gov.justice.payment.api.domain;

import org.hibernate.Hibernate;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.api.CreatePaymentResponse;
import uk.gov.justice.payment.api.json.external.GDSCreatePaymentResponse;

import javax.persistence.*;
import java.sql.Clob;

@Entity
public class PaymentDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Integer id;

    String paymentId;

    Integer amount;

    String paymentReference;

    String applicationReference;

    String serviceId;

    String description;


    String returnUrl;


    String response;

    public PaymentDetails(){};
    public PaymentDetails(CreatePaymentRequest request, GDSCreatePaymentResponse response) {
        this.paymentId = response.getPaymentId();
        this.amount = request.getAmount();
        this.paymentReference=request.getPaymentReference();
        this.applicationReference=request.getApplicationReference();
        this.serviceId=request.getServiceId();
        this.description=request.getDescription();
        this.returnUrl=request.getReturnUrl();
        this.response = response.toString();
        System.out.println("lob="+this.response);
    }
}
