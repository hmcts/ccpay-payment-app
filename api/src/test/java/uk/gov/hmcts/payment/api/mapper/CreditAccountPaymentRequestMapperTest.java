package uk.gov.hmcts.payment.api.mapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.PaymentByAccountRequest;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreditAccountPaymentRequestMapperTest {

    @Mock
    CreditAccountDtoMapper creditAccountDtoMapper;

    @InjectMocks
    CreditAccountPaymentRequestMapper creditAccountPaymentRequestMapper = new CreditAccountPaymentRequestMapper();

    CreditAccountPaymentRequest creditAccountPaymentRequest;

    @Before
    public void initiate(){
        List<FeeDto> fees = new ArrayList<>();
        FeeDto fee = FeeDto.feeDtoWith().ccdCaseNumber("ccd-case-number").build();
        fees.add(fee);
        creditAccountPaymentRequest = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("100.00"))
            .description("description")
            .ccdCaseNumber("ccd-case-number-1")
            .caseReference("case-reference")
            .fees(fees)
            .currency(CurrencyCode.GBP).service("CMC").customerReference("reference").organisationName("org-name")
            .accountNumber("acc-number").siteId("site-id").build();
    }

    @Test
    public void testMapPBARequest(){
        Payment payment = creditAccountPaymentRequestMapper.mapPBARequest(creditAccountPaymentRequest);
        assertEquals("ccd-case-number-1",payment.getCcdCaseNumber());
        assertEquals("case-reference",payment.getCaseReference());
    }

    @Test
    public void testMapPBAFeesFromRequest(){
        PaymentFee fee = PaymentFee.feeWith().code("FEE123").feeAmount(new BigDecimal("100.00")).ccdCaseNumber("ccd-case-number").build();
        when(creditAccountDtoMapper.toFee(Mockito.any(FeeDto.class))).thenReturn(fee);
        List<PaymentFee> paymentFees = creditAccountPaymentRequestMapper.mapPBAFeesFromRequest(creditAccountPaymentRequest);
        assertEquals("ccd-case-number",paymentFees.get(0).getCcdCaseNumber());
        assertEquals("FEE123",paymentFees.get(0).getCode());
    }

    @Test
    public void testMapPaymentByAccountRequestMapsRequestAndPaymentDetails() {
        CreditAccountPaymentRequest request = createPaymentByAccountRequest();

        PaymentByAccountRequest paymentByAccountRequest = creditAccountPaymentRequestMapper.mapPaymentByAccountRequest(request);

        assertEquals("pba123456", paymentByAccountRequest.getPbaNumber());
        assertEquals("CMC", paymentByAccountRequest.getPayment().getServiceName());
        assertEquals("100.00", paymentByAccountRequest.getPayment().getAmount());
        assertEquals("GBP", paymentByAccountRequest.getPayment().getCurrency());
        assertEquals("site-id", paymentByAccountRequest.getPayment().getSiteId());
        assertEquals("case-reference", paymentByAccountRequest.getPayment().getCaseReference());
        assertEquals("ccd-case-number-1", paymentByAccountRequest.getPayment().getCcdCaseNumber());
        assertEquals("reference", paymentByAccountRequest.getPayment().getCustomerReference());

        assertEquals("fee123", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getCode());
        assertEquals(123, paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getId());
        assertEquals("1", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getVersion());
        assertEquals("memo-line", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getMemosline());
        assertEquals("nac", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getNac());
        assertEquals("jurisdiction-1", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getJurisdiction1());
        assertEquals("jurisdiction-2", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getJurisdiction2());
        assertEquals("1", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getVolume());
        assertEquals("100.00", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getCalculatedAmount());

        assertEquals("fee234", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getCode());
        assertEquals(234, paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getId());
        assertEquals("2", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getVersion());
        assertEquals("memo-line2", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getMemosline());
        assertEquals("nac2", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getNac());
        assertEquals("jurisdiction-3", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getJurisdiction1());
        assertEquals("jurisdiction-4", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getJurisdiction2());
        assertEquals("2", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getVolume());
        assertEquals("200.00", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getCalculatedAmount());
    }

    @Test
    public void testMapPaymentByAccountRequestMapsFirstFeeDetails() {
        CreditAccountPaymentRequest request = createPaymentByAccountRequest();

        PaymentByAccountRequest paymentByAccountRequest = creditAccountPaymentRequestMapper.mapPaymentByAccountRequest(request);

        assertEquals("fee123", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getCode());
        assertEquals(Integer.valueOf(123), paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getId());
        assertEquals("1", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getVersion());
        assertEquals("memo-line", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getMemosline());
        assertEquals("nac", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getNac());
        assertEquals("jurisdiction-1", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getJurisdiction1());
        assertEquals("jurisdiction-2", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getJurisdiction2());
        assertEquals("1", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getVolume());
        assertEquals("100.00", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getCalculatedAmount());
    }

    @Test
    public void testMapPaymentByAccountRequestAllowsMissingOptionalFeeVolume() {
        CreditAccountPaymentRequest request = createPaymentByAccountRequest();
        request.getFees().getFirst().setVolume(null);

        PaymentByAccountRequest paymentByAccountRequest = creditAccountPaymentRequestMapper.mapPaymentByAccountRequest(request);

        assertEquals(null, paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getVolume());
        assertEquals("100.00", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getCalculatedAmount());
    }

    @Test
    public void testMapPaymentByAccountRequestMapsPaymentDtoPaymentAndFeeDetails() {
        Date dateCreated = new Date(1704067200000L);
        PaymentDto paymentDto = createPaymentDto(dateCreated);

        PaymentByAccountRequest paymentByAccountRequest = creditAccountPaymentRequestMapper.mapPaymentByAccountRequest(paymentDto);

        assertEquals("pba654321", paymentByAccountRequest.getPbaNumber());
        assertEquals("Divorce", paymentByAccountRequest.getPayment().getServiceName());
        assertEquals("group-reference", paymentByAccountRequest.getPayment().getGroupReference());
        assertEquals("payment-reference", paymentByAccountRequest.getPayment().getPaymentReference());
        assertEquals(dateCreated.toString(), paymentByAccountRequest.getPayment().getDateCreated());
        assertEquals("300.00", paymentByAccountRequest.getPayment().getAmount());
        assertEquals("GBP", paymentByAccountRequest.getPayment().getCurrency());
        assertEquals("BFA1", paymentByAccountRequest.getPayment().getSiteId());
        assertEquals("case-reference", paymentByAccountRequest.getPayment().getCaseReference());
        assertEquals("ccd-case-number", paymentByAccountRequest.getPayment().getCcdCaseNumber());
        assertEquals("customer-reference", paymentByAccountRequest.getPayment().getCustomerReference());

        assertEquals("fee345", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getCode());
        assertEquals(Integer.valueOf(345), paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getId());
        assertEquals("3", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getVersion());
        assertEquals("memo-line-3", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getMemosline());
        assertEquals("nac3", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getNac());
        assertEquals("jurisdiction-5", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getJurisdiction1());
        assertEquals("jurisdiction-6", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getJurisdiction2());
        assertEquals("3", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getVolume());
        assertEquals("300.00", paymentByAccountRequest.getPayment().getPaymentByAccountFees().getFirst().getCalculatedAmount());

        assertEquals("fee456", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getCode());
        assertEquals(Integer.valueOf(456), paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getId());
        assertEquals("4", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getVersion());
        assertEquals("memo-line-4", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getMemosline());
        assertEquals("nac4", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getNac());
        assertEquals("jurisdiction-7", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getJurisdiction1());
        assertEquals("jurisdiction-8", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getJurisdiction2());
        assertEquals("4", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getVolume());
        assertEquals("400.00", paymentByAccountRequest.getPayment().getPaymentByAccountFees().get(1).getCalculatedAmount());
    }

    private CreditAccountPaymentRequest createPaymentByAccountRequest() {
        FeeDto fee1 = FeeDto.feeDtoWith()
            .id(123)
            .code("fee123")
            .version("1")
            .memoLine("memo-line")
            .naturalAccountCode("nac")
            .jurisdiction1("jurisdiction-1")
            .jurisdiction2("jurisdiction-2")
            .volume(1)
            .calculatedAmount(new BigDecimal("100.00"))
            .build();

        FeeDto fee2 = FeeDto.feeDtoWith()
            .id(234)
            .code("fee234")
            .version("2")
            .memoLine("memo-line2")
            .naturalAccountCode("nac2")
            .jurisdiction1("jurisdiction-3")
            .jurisdiction2("jurisdiction-4")
            .volume(2)
            .calculatedAmount(new BigDecimal("200.00"))
            .build();

        List<FeeDto> fees = new ArrayList<>();
        fees.add(fee1);
        fees.add(fee2);

        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("100.00"))
            .description("description")
            .ccdCaseNumber("ccd-case-number-1")
            .caseReference("case-reference")
            .fees(fees)
            .currency(CurrencyCode.GBP)
            .service("CMC")
            .customerReference("reference")
            .organisationName("org-name")
            .accountNumber("pba123456")
            .siteId("site-id")
            .build();
    }

    private PaymentDto createPaymentDto(Date dateCreated) {
        FeeDto fee1 = FeeDto.feeDtoWith()
            .id(345)
            .code("fee345")
            .version("3")
            .memoLine("memo-line-3")
            .naturalAccountCode("nac3")
            .jurisdiction1("jurisdiction-5")
            .jurisdiction2("jurisdiction-6")
            .volume(3)
            .calculatedAmount(new BigDecimal("300.00"))
            .build();

        FeeDto fee2 = FeeDto.feeDtoWith()
            .id(456)
            .code("fee456")
            .version("4")
            .memoLine("memo-line-4")
            .naturalAccountCode("nac4")
            .jurisdiction1("jurisdiction-7")
            .jurisdiction2("jurisdiction-8")
            .volume(4)
            .calculatedAmount(new BigDecimal("400.00"))
            .build();

        List<FeeDto> fees = new ArrayList<>();
        fees.add(fee1);
        fees.add(fee2);

        return PaymentDto.payment2DtoWith()
            .amount(new BigDecimal("300.00"))
            .reference("group-reference")
            .paymentReference("payment-reference")
            .dateCreated(dateCreated)
            .currency(CurrencyCode.GBP)
            .serviceName("Divorce")
            .caseReference("case-reference")
            .ccdCaseNumber("ccd-case-number")
            .customerReference("customer-reference")
            .accountNumber("pba654321")
            .siteId("BFA1")
            .fees(fees)
            .build();
    }
}
