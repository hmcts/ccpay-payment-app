package uk.gov.hmcts.payment.casepaymentorders.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpoGetResponse implements Serializable {
    private static final long serialVersionUID = -3552426127042849422L;

    List<CasePaymentOrder> content;
    Integer number;
    Integer size;
    Long totalElements;
}
