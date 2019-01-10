package uk.gov.hmcts.payment.api.componenttests.util;

import lombok.SneakyThrows;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.payment.api.componenttests.PaymentDbBackdoor;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.model.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;
import static uk.gov.hmcts.payment.api.model.PaymentFee.feeWith;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

public class PaymentsDataUtil {

    @Autowired
    protected PaymentDbBackdoor db;

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    private static final String USER_ID = "user-id";

    public List<Payment> getCreditAccountPaymentsData() {
        List<Payment> payments = new ArrayList<>();
        payments.add(paymentWith().amount(BigDecimal.valueOf(10000).movePointRight(2)).reference("reference1").description("desc1")
            .returnUrl("returnUrl1")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .ccdCaseNumber("ccdCaseNo1").caseReference("caseRef1").serviceType("cmc").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(20000).movePointRight(2)).reference("reference2").description("desc2")
            .returnUrl("returnUrl2")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .ccdCaseNumber("ccdCaseNo2").caseReference("caseRef2").serviceType("divorce").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(30000).movePointRight(2)).reference("reference3").description("desc3")
            .returnUrl("returnUrl3")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .ccdCaseNumber("ccdCaseNo3").caseReference("caseRef3").serviceType("probate").currency("GBP").build());

        return payments;
    }

    public List<Payment> getCardPaymentsData() {
        List<Payment> payments = new ArrayList<>();
        payments.add(paymentWith().amount(BigDecimal.valueOf(10000).movePointRight(2)).reference("reference1").description("desc1")
            .returnUrl("returnUrl1")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .ccdCaseNumber("ccdCaseNo1").caseReference("caseRef1").serviceType("cmc").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(20000).movePointRight(2)).reference("reference2").description("desc2")
            .returnUrl("returnUrl2")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .ccdCaseNumber("ccdCaseNo2").caseReference("caseRef2").serviceType("divorce").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(30000).movePointRight(2)).reference("reference3").description("desc3")
            .returnUrl("returnUrl3")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .ccdCaseNumber("ccdCaseNo3").caseReference("caseRef3").serviceType("probate").currency("GBP").build());

        return payments;
    }

    public static List<PaymentFee> getFeesData() {
        List<PaymentFee> fees = new ArrayList<>();
        fees.add(feeWith().code("X0011").version("1").build());
        fees.add(feeWith().code("X0022").version("2").build());
        fees.add(feeWith().code("X0033").version("3").build());
        fees.add(feeWith().code("X0044").version("4").build());

        return fees;
    }

    public void populateCardPaymentToDb(String number) throws Exception {
        //Create a payment in db
        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference" + number)
            .ccdCaseNumber("ccdCaseNumber" + number)
            .description("Test payments statuses for " + number)
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0" + number)
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v" + number)
            .reference("RC-1519-9028-2432-000" + number)
            .statusHistories(Arrays.asList(statusHistory))
            .build();

        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE000" + number).volume(1).build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-0000000000" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);
    }


    public void populateCreditAccountPaymentToDb(String number) throws Exception {
        //Create a payment in db
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("11.99"))
            .caseReference("Reference" + number)
            .ccdCaseNumber("ccdCaseNumber" + number)
            .description("Description" + number)
            .serviceType("Probate")
            .currency("GBP")
            .siteId("AA0" + number)
            .pbaNumber("123456")
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .reference("RC-1519-9028-1909-000" + number)
            .build();

        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("FEE000" + number).volume(1).build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-0000000000" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

    }

    public void populateBarCashPaymentToDb(String number) throws Exception {
        //create a payment in db
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("123.19"))
            .caseReference("Reference" + number)
            .ccdCaseNumber("ccdCaseNumber" + number)
            .description("Description" + number)
            .serviceType("Digital Bar")
            .currency("GBP")
            .siteId("AA0" + number)
            .giroSlipNo("Grio" + number)
            .reportedDateOffline(DateTime.now().toDate())
            .userId(USER_ID)
            .paymentProvider(PaymentProvider.paymentProviderWith().name("middle office provider").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("digital bar").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("cash").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("pending").build())
            .reference("RC-1519-9028-1909-111" + number)
            .build();

        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("123.19")).version("1").code("FEE000" + number).volume(1).build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-0000000011" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);
    }


    public void populateBarChequePaymentToDb(String number) throws Exception {
        //create a payment in db
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("333.19"))
            .caseReference("Reference" + number)
            .ccdCaseNumber("ccdCaseNumber" + number)
            .description("Description" + number)
            .serviceType("Digital Bar")
            .currency("GBP")
            .siteId("AA0" + number)
            .giroSlipNo("Grio" + number)
            .reportedDateOffline(DateTime.now().toDate())
            .userId(USER_ID)
            .paymentProvider(PaymentProvider.paymentProviderWith().name("middle office provider").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("digital bar").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("cheque").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .reference("RC-1519-9028-1909-112" + number)
            .build();

        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("333.19")).version("1").code("FEE011" + number).volume(1).build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-0000000012" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);
    }

    public void populateBarCardPaymentToDb(String number) throws Exception {
        //create a payment in db
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("432.19"))
            .caseReference("Reference" + number)
            .ccdCaseNumber("ccdCaseNumber" + number)
            .description("Description" + number)
            .serviceType("Digital Bar")
            .currency("GBP")
            .siteId("AA0" + number)
            .giroSlipNo("Grio" + number)
            .reportedDateOffline(DateTime.now().toDate())
            .userId(USER_ID)
            .paymentProvider(PaymentProvider.paymentProviderWith().name("barclaycard").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("digital bar").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .reference("RC-1519-9028-1909-113" + number)
            .build();

        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("432.19")).version("1").code("FEE011" + number).volume(1).build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-0000000012" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

    }

    public void populateTelephonyPaymentToDb(String number) throws Exception {
        //Create a payment in db
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("101.99"))
            .caseReference("caseReference" + number)
            .description("description" + number)
            .serviceType("Divorce")
            .currency("GBP")
            .siteId("AA00" + number)
            .userId(USER_ID)
            .paymentProvider(PaymentProvider.paymentProviderWith().name("pci pal").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("telephony").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .reference("RC-1519-9028-1909-143" + number)
            .build();

        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("432.19")).version("1").code("FEE011" + number).volume(1).build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-0000000012" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);
    }

    public void populateCardPaymentToDbWith(Payment payment, String number) {
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE000" + number).volume(1).build();
        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-0000000000" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);
    }

    protected void assertPbaPayments(List<PaymentDto> payments) {
        assertThat(payments.size()).isEqualTo(1);
        payments.stream().forEach(p -> {
            assertThat(p.getPaymentReference()).isEqualTo("RC-1519-9028-1909-0002");
            assertThat(p.getCcdCaseNumber()).isEqualTo("ccdCaseNumber2");
            assertThat(p.getCaseReference()).isEqualTo("Reference2");
            assertThat(p.getAmount()).isEqualTo(new BigDecimal("11.99"));
            assertThat(p.getChannel()).isEqualTo("online");
            assertThat(p.getMethod()).isEqualTo("payment by account");
            assertThat(p.getStatus()).isEqualTo("Initiated");
            assertThat(p.getSiteId()).isEqualTo("AA02");
            assertThat(p.getAccountNumber()).isEqualTo("123456");
            assertThat(p.getDateCreated()).isNotNull();
            assertThat(p.getDateUpdated()).isNotNull();
            p.getFees().stream().forEach(f -> {
                assertThat(f.getCode()).isEqualTo("FEE0002");
                assertThat(f.getVersion()).isEqualTo("1");
                assertThat(f.getCalculatedAmount()).isEqualTo(new BigDecimal("11.99"));
            });
        });
    }

    @SneakyThrows
    protected String contentsOf(String fileName) {
        String content = new String(Files.readAllBytes(Paths.get(ResourceUtils.getURL("classpath:" + fileName).toURI())));
        return resolvePlaceholders(content);
    }

    protected String resolvePlaceholders(String content) {
        return configurableListableBeanFactory.resolveEmbeddedValue(content);
    }

    protected String requestJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD101\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"return_url\": \"https://www.gooooogle.com\",\n" +
            "  \"site_id\": \"AA101\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }


}
