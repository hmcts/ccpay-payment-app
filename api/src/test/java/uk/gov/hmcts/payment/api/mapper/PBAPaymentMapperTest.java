package uk.gov.hmcts.payment.api.mapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentByAccountRequest;
import uk.gov.hmcts.payment.api.dto.SupplementaryPaymentDto;
import uk.gov.hmcts.payment.api.dto.LiberataSupplementaryInfo;
import uk.gov.hmcts.payment.api.dto.LiberataSupplementaryDetails;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentProvider;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.IacService;
import uk.gov.hmcts.payment.api.service.PaymentService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PBAPaymentMapperTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private FeesService feesService;

    @Mock
    private IacService iacService;

    private PBAPaymentMapper pbaPaymentMapper;

    @Before
    public void setUp() {
        pbaPaymentMapper = new PBAPaymentMapper(paymentService, feesService, iacService);
    }

    @Test
    public void getPaymentDtoFromModel_should_map_fields_correctly() {
        // arrange
        Payment payment = Payment.paymentWith()
            .reference("RC-123")
            .serviceType("CMC")
            .siteId("SITE1")
            .amount(new BigDecimal("123.45"))
            .caseReference("case-ref")
            .ccdCaseNumber("ccd-1")
            .pbaNumber("PBA123")
            .organisationName("org")
            .customerReference("cust-ref")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .currency("GBP")
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .dateCreated(new Date())
            .dateUpdated(new Date())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .bankedDate(new Date())
            .giroSlipNo("G123")
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .externalReference("ext-ref")
            .build();

        CreditAccountPaymentRequest request = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("123.45"))
            .description("desc")
            .service("CMC")
            .currency(CurrencyCode.GBP)
            .customerReference("cust-ref")
            .organisationName("org")
            .accountNumber("PBA123")
            .siteId("SITE1")
            .fees(Collections.emptyList())
            .build();

        // act
        uk.gov.hmcts.payment.api.contract.PaymentDto dto = pbaPaymentMapper.getPaymentDtoFromModel(payment, "group-ref", request);

        // assert
        assertThat(dto).isNotNull();
        assertThat(dto.getPaymentReference()).isEqualTo("RC-123");
        assertThat(dto.getPaymentGroupReference()).isEqualTo("group-ref");
        assertThat(dto.getServiceName()).isEqualTo("CMC");
        assertThat(dto.getSiteId()).isEqualTo("SITE1");
        assertThat(dto.getAmount()).isEqualTo(new BigDecimal("123.45"));
        assertThat(dto.getCaseReference()).isEqualTo("case-ref");
        assertThat(dto.getCcdCaseNumber()).isEqualTo("ccd-1");
        assertThat(dto.getAccountNumber()).isEqualTo("PBA123");
        assertThat(dto.getOrganisationName()).isEqualTo("org");
        assertThat(dto.getCustomerReference()).isEqualTo("cust-ref");
        assertThat(dto.getChannel()).isEqualTo("online");
        assertThat(dto.getCurrency()).isEqualTo(CurrencyCode.GBP);
        assertThat(dto.getStatus()).isEqualTo("created");
        assertThat(dto.getMethod()).isEqualTo("card");
        assertThat(dto.getBankedDate()).isNotNull();
        assertThat(dto.getGiroSlipNo()).isEqualTo("G123");
        assertThat(dto.getExternalReference()).isEqualTo("ext-ref");
    }

    @Test
    public void mapToPaymentByAccountRequest_nonIAC_should_set_not_applicable_surname() {
        // arrange
        Payment payment = Payment.paymentWith()
            .serviceType("CMC")
            .reference("RC-1")
            .build();

        CreditAccountPaymentRequest request = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("10.00"))
            .description("desc")
            .service("CMC")
            .currency(CurrencyCode.GBP)
            .customerReference("cust")
            .organisationName("org")
            .accountNumber("PBA-1")
            .siteId("SITE1")
            .fees(Collections.emptyList())
            .build();

        PaymentFeeLink pfl = PaymentFeeLink.paymentFeeLinkWith().fees(Collections.emptyList()).build();

        // act
        uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest accountRequest = pbaPaymentMapper.mapToPaymentByAccountRequest(request, "group-ref", payment, pfl);

        // assert
        assertThat(accountRequest).isNotNull();
        PaymentByAccountRequest inner = accountRequest.getPaymentByAccountRequest();
        assertThat(inner).isNotNull();
        assertThat(inner.getPbaNumber()).isEqualTo("PBA-1");
        assertThat(inner.getPayment()).isNotNull();
        assertThat(inner.getPayment().getSurname()).isEqualTo("Not applicable, the servide type is not IAC");
    }

    @Test
    public void mapToPaymentByAccountRequest_IAC_should_use_iac_service_surname() {
        // arrange
        Payment payment = Payment.paymentWith()
            .serviceType("IAC")
            .reference("RC-2")
            .serviceType("IAC")
            .siteId("SITE1")
            .amount(new BigDecimal("15.00"))
            .caseReference("case-ref")
            .ccdCaseNumber("ccd-1")
            .pbaNumber("PBA-2")
            .organisationName("org")
            .customerReference("cust")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .currency("GBP")
            .build();

        CreditAccountPaymentRequest request = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("15.00"))
            .description("desc")
            .service("IAC")
            .currency(CurrencyCode.GBP)
            .customerReference("cust")
            .organisationName("org")
            .accountNumber("PBA-2")
            .siteId("SITE1")
            .fees(Collections.emptyList())
            .build();

        PaymentFeeLink pfl = PaymentFeeLink.paymentFeeLinkWith().fees(Collections.emptyList()).build();

        // build supplementary dto with surname
        LiberataSupplementaryDetails details = LiberataSupplementaryDetails.supplementaryDetailsWith().surname("Smith").build();
        LiberataSupplementaryInfo info = LiberataSupplementaryInfo.liberataSupplementaryInfoWith()
            .ccdCaseNumber("ccd-1")
            .supplementaryDetails(details)
            .build();

        SupplementaryPaymentDto supplementary = SupplementaryPaymentDto.supplementaryPaymentDtoWith()
            .supplementaryInfo(Collections.singletonList(info))
            .build();

        when(paymentService.getServiceNameByCode(anyString())).thenReturn("IAC-SERVICE");
        when(iacService.getIacSupplementaryInfo(anyList(), anyString())).thenReturn(new ResponseEntity<>(supplementary, HttpStatus.OK));

        // act
        uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest accountRequest = pbaPaymentMapper.mapToPaymentByAccountRequest(request, "group-ref", payment, pfl);

        // assert
        assertThat(accountRequest).isNotNull();
        PaymentByAccountRequest inner = accountRequest.getPaymentByAccountRequest();
        assertThat(inner).isNotNull();
        assertThat(inner.getPayment().getSurname()).isEqualTo("Smith");
    }
}

