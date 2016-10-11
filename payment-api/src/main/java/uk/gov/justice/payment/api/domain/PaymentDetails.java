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

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public void setApplicationReference(String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    String paymentId;

    public Integer getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getDescription() {
        return description;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getResponse() {
        return response;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedDate() {
        return createdDate;
    }


    public String getEmail() {
        return email;
    }

    Integer amount;

    String paymentReference;

    String applicationReference;

    String serviceId;

    String description;


    String returnUrl;

    String response;

    String status;


    String createdDate;

    String email;




    public PaymentDetails(){};
    public PaymentDetails(CreatePaymentRequest request, GDSCreatePaymentResponse response) {
        this.paymentId = response.getPaymentId();
        this.amount = request.getAmount();
        this.paymentReference=request.getPaymentReference();
        this.applicationReference=request.getApplicationReference();
        this.serviceId=request.getServiceId();
        this.description=request.getDescription();
        this.returnUrl=request.getReturnUrl();
        this.email=request.getEmail();
        this.response = response.toString();
        this.status = response.getState().getStatus();
        this.createdDate = response.getCreatedDate();

    }
}
