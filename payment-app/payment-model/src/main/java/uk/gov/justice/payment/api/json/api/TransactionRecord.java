
package uk.gov.justice.payment.api.json.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "transactionRecordWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransactionRecord {
    private Integer amount;
    private String applicationReference;
    private String createdDate;
    private String description;
    private String paymentId;
    private String paymentReference;
    private String serviceId;
    private String email;
}
