package uk.gov.hmcts.payment.api.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.StatusHistory;

import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "orderPaymentBoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderPaymentBo {

    private String reference;

    private BigDecimal amount;

    private String description;

    private String ccdCaseNumber;

    private String caseReference;

    private Service service;

    private CurrencyCode currency;

    private String customerReference;

    private String organisationName;

    private String accountNumber;

    private PaymentStatus status;

    private List<StatusHistory> statusHistories;
}
