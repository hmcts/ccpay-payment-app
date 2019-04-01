package uk.gov.hmcts.payment.api.contract;


import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private String id;
    private SimpleDateFormat sdf;
    private String giroSlipNo;
    private String reportedDateOffline;
    private String jurisdiction1;
    private String jurisdiction2;

    public PaymentDtoTest() {
        feeWithVolumeCode = "X0001";
        feeVersion = "3";
        calculatedAmountForFeeWithVolume = new BigDecimal(1);
        memoLine = "memoLine";
        naturalAccountCode = "naturalAccountCode";
        volume = 1;
        feeNoVolumeCode = "X0002";
        calculatedAmountForFeeNoVolume = new BigDecimal(1);
        jurisdiction1 = "family";
        jurisdiction2 = "probate service";


        feeWithVolumeDto = new FeeDto(feeWithVolumeCode, feeVersion, volume, calculatedAmountForFeeWithVolume,
            memoLine, naturalAccountCode, null, null, null, jurisdiction1, jurisdiction2);

        feeNoVolumeDto = new FeeDto(feeNoVolumeCode, feeVersion, volume, calculatedAmountForFeeNoVolume,
            memoLine, naturalAccountCode, null, null, null, jurisdiction1, jurisdiction2);
    }

    @Before
    public void beforeEach() {
        sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        DateTime currentDateTime = new DateTime();
        dateCreated = currentDateTime.toDate();
        dateUpdated = currentDateTime.plusDays(1).toDate();
        List<StatusHistoryDto> statusHistories = new ArrayList<>();
        PaymentDto.LinksDto links = new PaymentDto.LinksDto();

        amount = new BigDecimal(1);
        id = "1";
        description = "description";
        reference = "reference";
        gbp = CurrencyCode.GBP;
        ccdNumber = "ccdNumber";
        caseReference = "caseReference";
        paymentReference = "paymentReference";
        channel = "channel";
        method = "method";
        externalProvider = "externalProvider";
        status = "status";
        externalReference = "externalReference";
        siteId = "siteId";
        serviceName = "serviceName";
        customerReference = "customerReference";
        accountNumber = "accountNumber";
        organisationName = "organisationName";
        paymentGroupReference = "paymentGroupReference";
        giroSlipNo = "giroSlipNo";
        reportedDateOffline = "2018-09-05";


        testDto = new PaymentDto(id, amount, description, reference, dateCreated, dateUpdated,
            gbp, ccdNumber, caseReference, paymentReference, channel, method, externalProvider,
            status, externalReference, siteId, serviceName, customerReference, accountNumber,
            organisationName, paymentGroupReference, reportedDateOffline,
            null, statusHistories, giroSlipNo, links);
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
}
