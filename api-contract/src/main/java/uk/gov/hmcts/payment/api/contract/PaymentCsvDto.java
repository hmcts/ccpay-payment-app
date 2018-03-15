package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
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


@Builder(builderMethodName = "paymentCsvDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentCsvDto {

    private String id;

    @NotEmpty
    private BigDecimal amount;

    @NotEmpty
    private String description;

    private String reference;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateUpdated;

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


    private String paymentGroupReference;


    @NotNull
    private List<FeeCsvDto> fees;

    private List<StatusHistoryDto> statusHistories;


    public String toCardPaymentCsv() {


        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz");

        StringJoiner sb = new StringJoiner(",")
            .add(getServiceName())
            .add(getPaymentGroupReference())
            .add(getPaymentReference())
            .add(sdf.format(getDateCreated()))
            .add(sdf.format(getDateUpdated()))
            .add(getStatus())
            .add(getChannel())
            .add(getMethod())
            .add(getAmount().toString())
            .add(getSiteId());

        StringJoiner feeSb = new StringJoiner(",");

        for (FeeCsvDto fee : getFees()) {
            String memolineWithQuotes="";
            if (null!=fee.getMemoLine()){
                memolineWithQuotes = new StringBuffer().append('"').append(fee.getMemoLine()).append('"').toString();
            }
            feeSb.add(fee.getCode())
                .add(fee.getVersion())
                .add(fee.getCalculatedAmount().toString())
                .add(memolineWithQuotes)
                .add(fee.getNaturalAccountCode());
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
            .add(sdf.format(getDateUpdated()))
            .add(getStatus())
            .add(getChannel())
            .add(getMethod())
            .add(getAmount().toString())
            .add(getSiteId());

        StringJoiner feeSb = new StringJoiner(",");

        for (FeeCsvDto fee : getFees()) {
            String memolineWithQuotes="";
            if (!fee.getMemoLine().isEmpty()){
             memolineWithQuotes = new StringBuffer().append('"').append(fee.getMemoLine()).append('"').toString();
            }
            feeSb.add(fee.getCode())
                .add(fee.getVersion())
                .add(fee.getCalculatedAmount().toString())
                .add(memolineWithQuotes)
                .add(fee.getNaturalAccountCode());
        }
        return sb.merge(feeSb).toString();
    }


}
