package uk.gov.justice.payment.api.services;

import com.querydsl.core.types.Expression;

/**
 * Created by zeeshan on 14/10/2016.
 */
public class SearchCriteria {
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


    public Integer getAmount() {
        return amount;
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

    public void setEmail(String email) {
        this.email = email;
    }
}
