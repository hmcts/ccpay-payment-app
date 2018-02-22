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
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

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
public class PaymentDto {

    private String id;

    @NotEmpty
    private BigDecimal amount;

    @NotEmpty
    private String description;

    private String reference;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateCreated;

    private CurrencyCode currency;

    private String ccdCaseNumber;

    private String caseReference;

    private String paymentReference;

    private String channel;

    private String method;

    private String externalProvider;

    private String status;

    private String externalReference;

    @NotEmpty
    private String siteId;

    private String serviceName;

    private String customerReference;

    private String accountNumber;

    private String organisationName;

    @JsonProperty(access= JsonProperty.Access.READ_ONLY)
    private String paymentGroupReference;

    //@JsonUnwrapped
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
        private LinkDto self;
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

    public String toCardPaymentCsv() {

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz");

        StringJoiner sb = new StringJoiner(",")
            .add(getServiceName())
            .add(getPaymentGroupReference())
            .add(getPaymentReference())
            .add(getCcdCaseNumber())
            .add(getCaseReference())
            .add(sdf.format(getDateCreated()))
            .add(getChannel())
            .add(getMethod())
            .add(getAmount().toString())
            .add(getSiteId());

        StringJoiner feeSb = new StringJoiner(",");

        for (FeeDto fee : getFees()) {

            feeSb.add(fee.getCode()).add(fee.getVersion());
        }


        return sb.merge(feeSb).toString();
    }


    public String toCreditAccountPaymentCsv() {

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz");

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
            .add(getChannel())
            .add(getMethod())
            .add(getAmount().toString())
            .add(getSiteId());

        StringJoiner feeSb = new StringJoiner(",");

        for (FeeDto fee : getFees()) {

            feeSb.add(fee.getCode()).add(fee.getVersion());
        }


        return sb.merge(feeSb).toString();
    }




}
