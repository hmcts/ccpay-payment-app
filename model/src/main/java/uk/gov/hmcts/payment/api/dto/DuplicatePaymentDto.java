package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringJoiner;
import java.util.TimeZone;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DuplicatePaymentDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateUpdated;

    private String ccdCaseNumber;

    private String serviceName;

    @NotEmpty
    private BigDecimal amount;

    @NotEmpty
    private String channel;

    private String method;

    private Integer paymentLinkId;

    @NotEmpty
    private BigInteger count;

    public String toDuplicatePaymentCsv() {
        StringJoiner result = new StringJoiner("\n");
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
        sdfDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdfTime.setTimeZone(TimeZone.getTimeZone("UTC"));

        StringJoiner sb = new StringJoiner(",")
            .add(sdfDate.format(getDateUpdated()))
            .add(sdfTime.format(getDateUpdated()))
            .add(getCcdCaseNumber())
            .add(getServiceName())
            .add(getAmount().setScale(2, RoundingMode.UNNECESSARY).toString())
            .add(getChannel())
            .add(getMethod())
            .add(getPaymentLinkId().toString())
            .add(getCount().toString());

        result.add(sb.toString());
        return result.toString();
    }

}
