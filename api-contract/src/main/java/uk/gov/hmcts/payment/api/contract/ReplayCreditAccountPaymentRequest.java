package uk.gov.hmcts.payment.api.contract;

import com.opencsv.bean.CsvBindByName;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReplayCreditAccountPaymentRequest {

    @CsvBindByName(column = "impacted.payment.reference")
    private String existingPaymentReference;

    @CsvBindByName(column = "payment.amount")
    private BigDecimal amount;

    @CsvBindByName(column = "payment.ccd_case_number")
    private String ccdCaseNumber;

    @CsvBindByName(column = "payment.pba_number")
    private String pbaNumber;

    @CsvBindByName(column = "payment.description")
    private String description;

    @CsvBindByName(column = "payment.case_reference")
    private String caseReference;

    @CsvBindByName(column = "payment.service")
    private String service;

    @CsvBindByName(column = "payment.currency")
    private String currency;

    @CsvBindByName(column = "payment.customer_reference")
    private String customerReference;

    @CsvBindByName(column = "payment.organisation_name")
    private String organisationName;

    @CsvBindByName(column = "payment.site_id")
    private String siteId;

    @CsvBindByName(column = "fee.code")
    private String code;

    @CsvBindByName(column = "fee.calculated_amount")
    private BigDecimal calculatedAmount;

    @CsvBindByName(column = "fee.version")
    private String version;


}

