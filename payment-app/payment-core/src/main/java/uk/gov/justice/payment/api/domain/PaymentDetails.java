package uk.gov.justice.payment.api.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.external.GDSCreatePaymentResponse;

import javax.persistence.*;

@Entity
@Data
@Builder(builderMethodName = "paymentDetailsWith")
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDetails {

    @SequenceGenerator(name = "idgen", sequenceName = "payment_details_id_seq", allocationSize = 10)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idgen")
    private Integer id;

    private String paymentId;

    private Integer amount;

    private String paymentReference;

    private String applicationReference;

    private String serviceId;

    private String description;

    private String returnUrl;

    private String response;

    private String status;

    private String createdDate;

    private String email;


    public PaymentDetails(CreatePaymentRequest request, GDSCreatePaymentResponse response) {
        this.paymentId = response.getPaymentId();
        this.amount = request.getAmount();
        this.paymentReference = request.getPaymentReference();
        this.applicationReference = request.getApplicationReference();
        this.serviceId = request.getServiceId();
        this.description = request.getDescription();
        this.returnUrl = request.getReturnUrl();
        this.email = request.getEmail();
        this.response = response.toString();
        this.status = response.getState().getStatus();
        this.createdDate = response.getCreatedDate();
    }
}
