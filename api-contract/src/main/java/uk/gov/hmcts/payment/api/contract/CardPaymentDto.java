package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "payment2DtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CardPaymentDto {

    private String id;

    @NotEmpty
    private BigDecimal amount;

    @NotEmpty
    private String description;

    private String reference;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss.SSS'Z'", timezone = "GMT")
    private Date dateCreated;

    private String currency;

    private String ccdCaseNumber;

    private String caseReference;

    private String paymentReference;

    private String channel;

    private String method;

    private String provider;

    private String status;

    @NotEmpty
    private String siteId;

    private String serviceName;

    @JsonUnwrapped
    @NotNull
    private List<FeeDto> fees;

    @JsonProperty("_links")
    private LinksDto links;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(SnakeCaseStrategy.class)
    @JsonInclude(NON_NULL)
    public static class LinksDto {
        private LinkDto nextUrl;
        private LinkDto cancel;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(NON_NULL)
    public static class LinkDto {
        private String href;
        private String method;
    }

    public String toCsv() {

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");

        StringJoiner sb = new StringJoiner(",")
            .add(getServiceName())
            .add(getPaymentReference())
            .add(getCcdCaseNumber())
            .add(getCaseReference())
            .add(sdf.format(getDateCreated()))
            .add(getChannel())
            .add(getAmount().toString())
            .add(getSiteId());

        StringJoiner feeSb = new StringJoiner(",");

        for (FeeDto fee : getFees()) {

            feeSb.add(fee.getCode()).add(fee.getVersion());
        }


        return sb.merge(feeSb).toString();
    }

}
