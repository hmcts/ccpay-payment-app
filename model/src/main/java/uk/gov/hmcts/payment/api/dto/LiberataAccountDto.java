package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "liberataAccountDtoWith")
@Data
public class LiberataAccountDto {

    private String accountNumber;

    private String accountName;

    private BigDecimal creditLimit;

    private BigDecimal availableBalance;

    private String status;

    private String reason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "GMT")
    private Date effectiveDate;

}
