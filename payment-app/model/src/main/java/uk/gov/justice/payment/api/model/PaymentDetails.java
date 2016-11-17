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

    @SequenceGenerator(name = "idgen", sequenceName = "payment_details_id_seq", allocationSize = 10)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idgen")
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
