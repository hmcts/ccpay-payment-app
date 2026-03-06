package uk.gov.hmcts.payment.api.contract;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentDtoTest {

    private final String feeWithVolumeCode;
    private final String feeVersion;
    private final BigDecimal calculatedAmountForFeeWithVolume;
    private final String memoLine;
    private final String naturalAccountCode;
    private final Integer volume;
    private final String feeNoVolumeCode;
    private final BigDecimal calculatedAmountForFeeNoVolume;
    private PaymentDto testDto;
    private FeeDto feeWithVolumeDto;
    private FeeDto feeNoVolumeDto;
    private String description;
    private String reference;
    private CurrencyCode gbp;
    private String ccdNumber;
    private String caseReference;
    private String paymentReference;
    private String channel;
    private String method;
    private String externalProvider;
    private String status;
    private String externalReference;
    private String siteId;
    private String serviceName;
    private String customerReference;
    private String accountNumber;
    private String organisationName;
    private String paymentGroupReference;
    private Date dateCreated;
    private Date dateUpdated;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private String id;
    private SimpleDateFormat sdf;
    private String giroSlipNo;
    private Date reportedDateOffline;
    private String jurisdiction1;
    private String jurisdiction2;
    private String feeDescription;
    private String documentControlNumber;
    private Date bankedDate;
    private String payerName;
    private BigDecimal apportionAmount;
    private BigDecimal allocatedAmount;
    private BigDecimal amountDue;
    private BigDecimal apportionedPayment;
    private Date dateReceiptProcessed;
    private Date dateApportioned;
    private Boolean refundEnable;
    private Boolean remissionEnable;
    private String internalReference;
    private List<DisputeDto> disputeDto;
    private String defaultVolum;

    public PaymentDtoTest() {
        amount = new BigDecimal(1);
        defaultVolum = "1";
        feeWithVolumeCode = "X0001";
        feeVersion = "3";
        calculatedAmountForFeeWithVolume = new BigDecimal(1);
        feeAmount = new BigDecimal(0.03);
        memoLine = "memoLine";
        naturalAccountCode = "naturalAccountCode";
        volume = 1;
        feeNoVolumeCode = "X0002";
        calculatedAmountForFeeNoVolume = new BigDecimal(1);
        jurisdiction1 = "family";
        jurisdiction2 = "probate service";
        feeDescription = "Fee Description";
        caseReference = "12345";
        apportionAmount = new BigDecimal(1);
        allocatedAmount = new BigDecimal(1);
        DateTime currentDateTime = new DateTime();
        dateApportioned = currentDateTime.toDate();
        dateCreated = currentDateTime.toDate();
        dateUpdated = currentDateTime.plusDays(1).toDate();
        amountDue = new BigDecimal(1);
        paymentGroupReference = "paymentGroupReference";
        apportionedPayment = new BigDecimal(1);
        dateReceiptProcessed = currentDateTime.toDate();
        sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        List<StatusHistoryDto> statusHistories = new ArrayList<>();
        List<PaymentAllocationDto> paymentAllocations = new ArrayList<>();
    }

    @Before
    public void beforeEach() {
        testDto = defaultTestDto(testDto);
        feeNoVolumeDto = FeeDto.feeDtoWith().feeAmount(feeAmount).code(feeNoVolumeCode).version(feeVersion)
            .calculatedAmount(calculatedAmountForFeeNoVolume).memoLine(memoLine).naturalAccountCode(naturalAccountCode)
            .description(feeDescription).caseReference(caseReference).apportionAmount(apportionAmount)
            .allocatedAmount(allocatedAmount).dateApportioned(dateApportioned).dateCreated(dateCreated)
            .dateUpdated(dateUpdated).amountDue(amountDue).paymentGroupReference(paymentGroupReference)
            .apportionedPayment(apportionedPayment).dateReceiptProcessed(dateReceiptProcessed).build();

        feeWithVolumeDto = FeeDto.feeDtoWith().feeAmount(feeAmount).code(feeWithVolumeCode).version(feeVersion)
            .calculatedAmount(calculatedAmountForFeeNoVolume).memoLine(memoLine).naturalAccountCode(naturalAccountCode)
            .description(feeDescription).caseReference(caseReference).apportionAmount(apportionAmount)
            .allocatedAmount(allocatedAmount).dateApportioned(dateApportioned).dateCreated(dateCreated)
            .dateUpdated(dateUpdated).amountDue(amountDue).paymentGroupReference(paymentGroupReference)
            .apportionedPayment(apportionedPayment).dateReceiptProcessed(dateReceiptProcessed).volume(volume).build();
    }

    private PaymentDto defaultTestDto(PaymentDto paymentDto) {
        paymentDto = new PaymentDto();
        paymentDto.setAmount(amount);
        paymentDto.setCaseReference(caseReference);
        paymentDto.setCcdCaseNumber(ccdNumber);
        paymentDto.setChannel(channel);
        paymentDto.setCurrency(gbp);
        paymentDto.setDateCreated(dateCreated);
        paymentDto.setDateUpdated(dateUpdated);
        paymentDto.setDescription(description);
        paymentDto.setExternalProvider(externalProvider);
        paymentDto.setExternalReference(externalReference);
        paymentDto.setId(id);
        paymentDto.setMethod(method);
        paymentDto.setOrganisationName(organisationName);
        paymentDto.setPaymentGroupReference(paymentGroupReference);
        paymentDto.setPaymentReference(paymentReference);
        paymentDto.setReference(reference);
        paymentDto.setSiteId(siteId);
        paymentDto.setStatus(status);
        paymentDto.setServiceName(serviceName);
        paymentDto.setCustomerReference(customerReference);
        paymentDto.setAccountNumber(accountNumber);
        paymentDto.setGiroSlipNo(giroSlipNo);
        paymentDto.setReportedDateOffline(reportedDateOffline);
        paymentDto.setDocumentControlNumber(documentControlNumber);
        paymentDto.setBankedDate(bankedDate);
        paymentDto.setPayerName(payerName);
        paymentDto.setRefundEnable(refundEnable);
        return paymentDto;
    }


    @Test
    public void cardPaymentCsvFillsVolumeAmountWhenExists() {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(feeWithVolumeDto);
        testDto.setFees(fees);

        StringJoiner joiner = new StringJoiner(",");
        joiner.add(serviceName)
            .add(paymentGroupReference)
            .add(paymentReference)
            .add(ccdNumber)
            .add(caseReference)
            .add(sdf.format(dateCreated))
            .add(sdf.format(dateUpdated))
            .add(status)
            .add(channel)
            .add(method)
            .add(amount.toString())
            .add(siteId)
            .add(feeWithVolumeCode)
            .add(feeVersion)
            .add(calculatedAmountForFeeWithVolume.toString())
            .add("\"" + memoLine + "\"")
            .add(naturalAccountCode)
            .add(volume.toString());

        assertThat(testDto.toCardPaymentCsv()).isEqualTo(joiner.toString());
    }

    @Test
    public void cardPaymentCsvPutsEmptyStringVolumeAmountWhenNotExists() {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(feeNoVolumeDto);
        testDto.setFees(fees);

        StringJoiner joiner = new StringJoiner(",");
        joiner.add(serviceName)
            .add(paymentGroupReference)
            .add(paymentReference)
            .add(ccdNumber)
            .add(caseReference)
            .add(sdf.format(dateCreated))
            .add(sdf.format(dateUpdated))
            .add(status)
            .add(channel)
            .add(method)
            .add(amount.toString())
            .add(siteId)
            .add(feeNoVolumeCode)
            .add(feeVersion)
            .add(calculatedAmountForFeeNoVolume.toString())
            .add("\"" + memoLine + "\"")
            .add(naturalAccountCode)
            .add(volume.toString());

        assertThat(testDto.toCardPaymentCsv()).isEqualTo(joiner.toString());
    }

    @Test
    public void creditAccountCsvFillsVolumeAmountWhenExists() {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(feeWithVolumeDto);
        testDto.setFees(fees);

        StringJoiner joiner = new StringJoiner(",");
        joiner.add(serviceName)
            .add(paymentGroupReference)
            .add(paymentReference)
            .add(ccdNumber)
            .add(caseReference)
            .add(organisationName)
            .add(customerReference)
            .add(accountNumber)
            .add(sdf.format(dateCreated))
            .add(sdf.format(dateUpdated))
            .add(status)
            .add(channel)
            .add(method)
            .add(amount.toString())
            .add(siteId)
            .add(feeWithVolumeCode)
            .add(feeVersion)
            .add(calculatedAmountForFeeWithVolume.toString())
            .add("\"" + memoLine + "\"")
            .add(naturalAccountCode)
            .add(volume.toString());

        assertThat(testDto.toCreditAccountPaymentCsv()).isEqualTo(joiner.toString());
    }

    @Test
    public void creditAccountCsvPutsEmptyStringVolumeAmountWhenNotExists() {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(feeNoVolumeDto);
        testDto.setFees(fees);

        StringJoiner joiner = new StringJoiner(",");
        joiner.add(serviceName)
            .add(paymentGroupReference)
            .add(paymentReference)
            .add(ccdNumber)
            .add(caseReference)
            .add(organisationName)
            .add(customerReference)
            .add(accountNumber)
            .add(sdf.format(dateCreated))
            .add(sdf.format(dateUpdated))
            .add(status)
            .add(channel)
            .add(method)
            .add(amount.toString())
            .add(siteId)
            .add(feeNoVolumeCode)
            .add(feeVersion)
            .add(calculatedAmountForFeeNoVolume.toString())
            .add("\"" + memoLine + "\"")
            .add(naturalAccountCode)
            .add(volume.toString());

        assertThat(testDto.toCreditAccountPaymentCsv()).isEqualTo(joiner.toString());
    }

    @Test
    public void cardPaymentCsvUnwrapsMultipleFeesOfOnePaymentIntoMultipleRows() {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(feeWithVolumeDto);
        fees.add(feeNoVolumeDto);
        testDto.setFees(fees);

        StringJoiner rowJointer = new StringJoiner("\n");

        StringJoiner joiner = new StringJoiner(",");
        joiner.add(serviceName)
            .add(paymentGroupReference)
            .add(paymentReference)
            .add(ccdNumber)
            .add(caseReference)
            .add(sdf.format(dateCreated))
            .add(sdf.format(dateUpdated))
            .add(status)
            .add(channel)
            .add(method)
            .add(amount.toString())
            .add(siteId)
            .add(feeWithVolumeCode)
            .add(feeVersion)
            .add(calculatedAmountForFeeWithVolume.toString())
            .add("\"" + memoLine + "\"")
            .add(naturalAccountCode)
            .add(volume.toString());

        StringJoiner joiner2 = new StringJoiner(",");
        joiner2.add(serviceName)
            .add(paymentGroupReference)
            .add(paymentReference)
            .add(ccdNumber)
            .add(caseReference)
            .add(sdf.format(dateCreated))
            .add(sdf.format(dateUpdated))
            .add(status)
            .add(channel)
            .add(method)
            .add(amount.toString())
            .add(siteId)
            .add(feeNoVolumeCode)
            .add(feeVersion)
            .add(calculatedAmountForFeeNoVolume.toString())
            .add("\"" + memoLine + "\"")
            .add(naturalAccountCode)
            .add(volume.toString());


        rowJointer.add(joiner.toString());
        rowJointer.add(joiner2.toString());
        assertThat(testDto.toCardPaymentCsv()).isEqualTo(rowJointer.toString());
    }

    @Test
    public void creditAccountCsvUnwrapsMultipleFeesOfOnePaymentIntoMultipleRows() {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(feeWithVolumeDto);
        fees.add(feeNoVolumeDto);
        testDto.setFees(fees);

        StringJoiner rowJointer = new StringJoiner("\n");

        StringJoiner joiner = new StringJoiner(",");
        joiner.add(serviceName)
            .add(paymentGroupReference)
            .add(paymentReference)
            .add(ccdNumber)
            .add(caseReference)
            .add(organisationName)
            .add(customerReference)
            .add(accountNumber)
            .add(sdf.format(dateCreated))
            .add(sdf.format(dateUpdated))
            .add(status)
            .add(channel)
            .add(method)
            .add(amount.toString())
            .add(siteId)
            .add(feeWithVolumeCode)
            .add(feeVersion)
            .add(calculatedAmountForFeeWithVolume.toString())
            .add("\"" + memoLine + "\"")
            .add(naturalAccountCode)
            .add(volume.toString());

        StringJoiner joiner2 = new StringJoiner(",");
        joiner2.add(serviceName)
            .add(paymentGroupReference)
            .add(paymentReference)
            .add(ccdNumber)
            .add(caseReference)
            .add(organisationName)
            .add(customerReference)
            .add(accountNumber)
            .add(sdf.format(dateCreated))
            .add(sdf.format(dateUpdated))
            .add(status)
            .add(channel)
            .add(method)
            .add(amount.toString())
            .add(siteId)
            .add(feeNoVolumeCode)
            .add(feeVersion)
            .add(calculatedAmountForFeeNoVolume.toString())
            .add("\"" + memoLine + "\"")
            .add(naturalAccountCode)
            .add(volume.toString());


        rowJointer.add(joiner.toString());
        rowJointer.add(joiner2.toString());
        assertThat(testDto.toCreditAccountPaymentCsv()).isEqualTo(rowJointer.toString());
    }

    @Test
    public void paymentCsvPutsEmptyStringVolumeAmountWhenExists() {
            List<FeeDto> fees = new ArrayList<>();
            fees.add(feeWithVolumeDto);
            testDto.setFees(fees);

            StringJoiner joiner = new StringJoiner(",");
            joiner.add(serviceName)
                .add(paymentGroupReference)
                .add(paymentReference)
                .add(ccdNumber)
                .add(caseReference)
                .add(organisationName)
                .add(customerReference)
                .add(accountNumber)
                .add(sdf.format(dateCreated))
                .add(sdf.format(dateUpdated))
                .add(status)
                .add(channel)
                .add(method)
                .add(amount.toString())
                .add(siteId)
                .add(feeWithVolumeCode)
                .add(feeVersion)
                .add(calculatedAmountForFeeWithVolume.toString())
                .add("\"" + memoLine + "\"")
                .add(naturalAccountCode)
                .add(volume.toString());

            assertThat(testDto.toPaymentCsv()).isEqualTo(joiner.toString());
        }

    @Test
    public void paymentCsvPutsEmptyStringVolumeAmountWhenNotExists() {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(feeNoVolumeDto);
        testDto.setFees(fees);

        StringJoiner joiner = new StringJoiner(",");
        joiner.add(serviceName)
            .add(paymentGroupReference)
            .add(paymentReference)
            .add(ccdNumber)
            .add(caseReference)
            .add(organisationName)
            .add(customerReference)
            .add(accountNumber)
            .add(sdf.format(dateCreated))
            .add(sdf.format(dateUpdated))
            .add(status)
            .add(channel)
            .add(method)
            .add(amount.toString())
            .add(siteId)
            .add(feeNoVolumeCode)
            .add(feeVersion)
            .add(calculatedAmountForFeeNoVolume.toString())
            .add("\"" + memoLine + "\"")
            .add(naturalAccountCode)
            .add(defaultVolum);

        assertThat(testDto.toPaymentCsv()).isEqualTo(joiner.toString());
    }

}
