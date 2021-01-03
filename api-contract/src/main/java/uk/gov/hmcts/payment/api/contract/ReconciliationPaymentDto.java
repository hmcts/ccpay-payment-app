package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.payment.api.EnrichablePaymentDto;
import uk.gov.hmcts.payment.api.EnrichablePaymentFeeDto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.TimeZone;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@SuperBuilder(builderMethodName = "casePaymentResponseWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReconciliationPaymentDto extends EnrichablePaymentDto {
    private List<StatusHistoryDto> statusHistories;

    public String toCardPaymentCsv() {
        StringJoiner result = new StringJoiner("\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (EnrichablePaymentFeeDto fee : getFees()) {
            StringJoiner sb = new StringJoiner(",")
                .add(getServiceName())
                .add(getPaymentGroupReference())
                .add(getPaymentReference())
                .add(getCcdCaseNumber())
                .add(getCaseReference())
                .add(sdf.format(getDateCreated()))
                .add(sdf.format(getDateUpdated()))
                .add(getStatus())
                .add(getChannel())
                .add(getMethod())
                .add(getAmount().toString())
                .add(getSiteId());

            String memoLineWithQuotes = fee.getMemoLine() != null ? new StringBuffer().append('"').append(fee.getMemoLine()).append('"').toString() : "";
            String naturalAccountCode = fee.getNaturalAccountCode() != null ? fee.getNaturalAccountCode() : "";
            sb.add(fee.getCode())
                .add(fee.getVersion())
                .add(fee.getCalculatedAmount().toString())
                .add(memoLineWithQuotes)
                .add(naturalAccountCode)
                .add(fee.getVolume().toString());

            result.add(sb.toString());
        }

        return result.toString();
    }

    public String toPaymentCsv() {
        return toCreditAccountPaymentCsv();
    }

    public String toCreditAccountPaymentCsv() {
        StringJoiner result = new StringJoiner("\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (EnrichablePaymentFeeDto fee : getFees()) {
            StringJoiner sb = new StringJoiner(",")
                .add(getServiceName())
                .add(getPaymentGroupReference())
                .add(getPaymentReference())
                .add(getCcdCaseNumber())
                .add(getCaseReference())
                .add(getOrganisationName())
                .add(getCustomerReference())
                .add(getAccountNumber())
                .add(sdf.format(getDateCreated()))
                .add(sdf.format(getDateUpdated()))
                .add(getStatus())
                .add(getChannel())
                .add(getMethod())
                .add(getAmount().toString())
                .add(getSiteId());

            String memolineWithQuotes = fee.getMemoLine() != null ? new StringBuffer().append('"').append(fee.getMemoLine()).append('"').toString() : "";
            String naturalAccountCode = fee.getNaturalAccountCode() != null ? fee.getNaturalAccountCode() : "";

            sb.add(fee.getCode())
                .add(fee.getVersion())
                .add(fee.getCalculatedAmount().toString())
                .add(memolineWithQuotes)
                .add(naturalAccountCode)
                .add(fee.getVolume().toString());
            result.add(sb.toString());
        }

        return result.toString();
    }


}
