package uk.gov.hmcts.payment.api.contract;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CasePaymentOrdersDto {
    private List<CasePaymentOrderDto> content;
    private Integer number;
    private Integer size;
    private Long totalElements;
}
