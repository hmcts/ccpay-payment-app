package uk.gov.hmcts.payment.functional.fixture;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.RefundsFeeDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RetrospectiveRemissionRequest;
import uk.gov.hmcts.payment.api.model.ContactDetails;
import uk.gov.hmcts.payment.api.dto.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusChargebackDto;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.math.BigDecimal;
import java.util.List;

public class PaymentFixture {

    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;

    public static CardPaymentRequest aCardPaymentRequest(String amountString) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service("CMC")
            .currency(CurrencyCode.GBP)
            .siteId("AA101")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal(amountString))
                .code("FEE0001")
                .version("1")
                .build())
            )
            .build();
    }

    public static CardPaymentRequest cardPaymentRequestProbate(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("AA08")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0001")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CardPaymentRequest cardPaymentRequestIAC(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("BFA1")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0123")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CardPaymentRequest cardPaymentRequestAdoption(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("ABA4")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0123")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CardPaymentRequest cardPaymentRequestPRL(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("ABA5")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0123")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CardPaymentRequest cardPaymentRequestall(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service(service)
            .siteId("AA07")
            .currency(CurrencyCode.GBP)
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0002")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequest(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("AA101")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("PBAFUNC12345")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0001")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequestForCivil(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("AAA7")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("PBAFUNC12345")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0001")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequestForDivorce(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("AA08")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("PBAFUNC12345")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0002")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequestForIAC(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("BFA1")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("PBAFUNC12345")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0001")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequestForFPL(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("ABA3")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("PBAFUNC12345")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0001")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequestForSPEC(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("AAA6")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("PBAFUNC12345")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0209")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequestForProbate(
        final String amountString, final String service, final String pbaAccountNumber) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("ABA6")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber(pbaAccountNumber)
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0001")
                    .version("1")
                    .feeAmount(new BigDecimal(amountString))
                    .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequestForProbateWithFeeCode(
        final String amountString, final String feeCode, final String service, final String pbaAccountNumber, String ccdCaseNumber) {
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("ABA6")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber(pbaAccountNumber)
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code(feeCode)
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequestForProbateSinglePaymentFor2Fees(
        final String amountString,
        final String service,
        final String pbaAccountNumber,
        final String feeCode1,
        final String feeAmount1,
        final String feeCode2,
        final String feeAmount2 ) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("ABA6")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber(pbaAccountNumber)
            .fees(List.of(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(feeAmount1))
                    .code(feeCode1)
                    .version("1")
                    .build(),
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(feeAmount2))
                    .code(feeCode2)
                    .version("1")
                    .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequestForProbateForSuccessLiberataValidation(String amountString, String service) {
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("aCaseReference")
            .service(service)
            .currency(CurrencyCode.GBP)
            .siteId("ABA6")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("PBAFUNC12345")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0226")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static PaymentRecordRequest aBarPaymentRequest(String amountString) {
        return  PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .paymentMethod(PaymentMethodType.CASH)
            .reference("case_ref_123")
            .service("DIGITAL_BAR")
            .currency(CurrencyCode.GBP)
            .giroSlipNo("12345")
            .reportedDateOffline(DateTime.now().toString())
            .siteId("AA07")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal(amountString))
                    .code("FEE0001")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static PaymentRefundRequest aRefundRequest(final int paymentId,
                                                      final String refundReason,
                                                      final String paymentReference,
                                                      final String refundAmount,
                                                      final String feeAmount) {
        return PaymentRefundRequest
            .refundRequestWith().paymentReference(paymentReference)
            .refundReason(refundReason)
            .totalRefundAmount(new BigDecimal(refundAmount))
            .fees(Lists.newArrayList(
                RefundsFeeDto.refundFeeDtoWith()
                    .apportionAmount(BigDecimal.valueOf(0))
                    .calculatedAmount(new BigDecimal(feeAmount))
                    .code("FEE0001")
                    .id(paymentId)
                    .version("1")
                    .updatedVolume(1)
                    .refundAmount(new BigDecimal(refundAmount))
                    .build())
            )
            .contactDetails(ContactDetails.contactDetailsWith().
                addressLine("High Street 112")
                .country("UK")
                .county("Londonshire")
                .city("London")
                .postalCode("P1 1PO")
                .email("person@gmail.com")
                .notificationType("EMAIL")
                .build())
            .build();

    }

    public static PaymentStatusBouncedChequeDto bouncedChequeRequest(String paymentReference){
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String failureReference = "FR-111-CC13-" + RandomUtils.nextInt();
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        return PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .reason("RR001")
            .paymentReference(paymentReference)
            .eventDateTime(actualDateTime.plusMinutes(5).toString())
            .additionalReference("AR1234556")
            .amount(new BigDecimal(100.00))
            .failureReference(failureReference)
            .ccdCaseNumber(ccdCaseNumber)
            .build();
    }

    public static PaymentStatusBouncedChequeDto bouncedChequeRequestService(String paymentReference, String ccdCaseNumber){
        String failureReference = "FR-111-CC13-" + RandomUtils.nextInt();
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        return PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .reason("RR001")
            .paymentReference(paymentReference)
            .eventDateTime(actualDateTime.plusMinutes(5).toString())
            .additionalReference("AR1234556")
            .amount(new BigDecimal(100.00))
            .failureReference(failureReference)
            .ccdCaseNumber(ccdCaseNumber)
            .build();
    }

    public static PaymentStatusChargebackDto chargebackRequest(String paymentReference){
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String failureReference = "FR-111-CC13-" + RandomUtils.nextInt();
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        return PaymentStatusChargebackDto.paymentStatusChargebackRequestWith()
            .reason("RR001")
            .paymentReference(paymentReference)
            .eventDateTime(actualDateTime.plusMinutes(5).toString())
            .additionalReference("AR1234556")
            .amount(new BigDecimal(50.00))
            .failureReference(failureReference)
            .ccdCaseNumber(ccdCaseNumber)
            .hasAmountDebited("Yes")
            .build();
    }

    public static PaymentStatusChargebackDto chargebackRequestService(String paymentReference, String ccdCaseNumber){
        String failureReference = "FR-111-CC14-" + RandomUtils.nextInt();
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        return PaymentStatusChargebackDto.paymentStatusChargebackRequestWith()
            .reason("RR001")
            .paymentReference(paymentReference)
            .eventDateTime(actualDateTime.plusMinutes(5).toString())
            .additionalReference("AR1234556")
            .amount(new BigDecimal(50.00))
            .failureReference(failureReference)
            .ccdCaseNumber(ccdCaseNumber)
            .hasAmountDebited("Yes")
            .build();
    }

    public static PaymentStatusBouncedChequeDto bouncedChequeRequestForFailureRef(String paymentReference, String failureReference){
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        return PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .reason("RR001")
            .paymentReference(paymentReference)
            .eventDateTime(actualDateTime.plusMinutes(5).toString())
            .additionalReference("AR1234556")
            .amount(new BigDecimal(100))
            .failureReference(failureReference)
            .ccdCaseNumber(ccdCaseNumber)
            .build();
    }

    public static PaymentStatusChargebackDto chargebackRequestForFailureRef(String paymentReference, String failureReference){
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        return PaymentStatusChargebackDto.paymentStatusChargebackRequestWith()
            .reason("RR001")
            .paymentReference(paymentReference)
            .eventDateTime(actualDateTime.plusMinutes(5).toString())
            .additionalReference("AR1234556")
            .amount(new BigDecimal(35))
            .failureReference(failureReference)
            .ccdCaseNumber(ccdCaseNumber)
            .hasAmountDebited("yes")
            .build();
    }

    public static PaymentStatusBouncedChequeDto bouncedChequeRequestForLessEventTime(String paymentReference){
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String failureReference = "FR-111-CC13-" + RandomUtils.nextInt();
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        return PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .reason("RR001")
            .paymentReference(paymentReference)
            .eventDateTime(actualDateTime.minusHours(2).toString())
            .additionalReference("AR1234556")
            .amount(new BigDecimal(100.00))
            .failureReference(failureReference)
            .ccdCaseNumber(ccdCaseNumber)
            .build();
    }

    public static PaymentStatusChargebackDto chargebackRequestForLessEventTime(String paymentReference){
        String ccdCaseNumber = "11113333" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String failureReference = "FR-111-CC13-" + RandomUtils.nextInt();
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        return PaymentStatusChargebackDto.paymentStatusChargebackRequestWith()
            .reason("RR001")
            .paymentReference(paymentReference)
            .eventDateTime(actualDateTime.minusHours(5).toString())
            .additionalReference("AR1234556")
            .amount(new BigDecimal(50.00))
            .failureReference(failureReference)
            .ccdCaseNumber(ccdCaseNumber)
            .hasAmountDebited("Yes")
            .build();
    }


    public static RetrospectiveRemissionRequest aRetroRemissionRequest(final String remissionReference) {

        return RetrospectiveRemissionRequest
            .retrospectiveRemissionRequestWith().remissionReference(remissionReference)
            .contactDetails(ContactDetails.contactDetailsWith()
                .addressLine("High Street 112")
                .country("UK")
                .county("Londonshire")
                .city("London")
                .postalCode("P1 1PO")
                .email("person@gmail.com")
                .notificationType("EMAIL")
                .build())
            .build();
    }
}
