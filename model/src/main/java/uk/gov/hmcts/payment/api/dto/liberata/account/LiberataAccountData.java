package uk.gov.hmcts.payment.api.dto.liberata.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LiberataAccountData {

    @JsonProperty("pba_reference")
    private String pbaReference;

//    @JsonProperty("account_name")
//    Spec specifies account_name, but Test API returns trading_name....
    @JsonProperty("trading_name")
    private String tradingName;

    @JsonProperty("credit_limit")
    private BigDecimal creditLimit;

    @JsonProperty("available_balance")
    private BigDecimal availableBalance;

    @JsonProperty("status")
    private AccountStatus status;

    @JsonProperty("effective_date")
    private Date effectiveDate;

}
