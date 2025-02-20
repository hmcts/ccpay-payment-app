package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "unprocessedPayment")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UnprocessedPayment {

    @NotNull
    @NotEmpty
    private String failureReference;

    private String reason;

    private String poBoxNumber;

    private BigDecimal amount;

    @NotNull
    @NotEmpty
    private String dcn;

    private String eventDateTime;
}
