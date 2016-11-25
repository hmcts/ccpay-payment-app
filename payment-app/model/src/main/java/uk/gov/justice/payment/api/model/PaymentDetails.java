package uk.gov.justice.payment.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@Builder(builderMethodName = "paymentDetailsWith")
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String serviceId;
    private String paymentId;
    private String email;
    private Integer amount;
    private String paymentReference;
    private String applicationReference;
    private String description;
    private String returnUrl;
    private String nextUrl;
    private String cancelUrl;
    private String response;
    private String status;
    private Boolean finished;
    private String createdDate;
}
