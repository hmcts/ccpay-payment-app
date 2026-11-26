package uk.gov.hmcts.payment.api.mapper;

import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentByAccountRequest;
import uk.gov.hmcts.payment.api.dto.SupplementaryPaymentDto;
import uk.gov.hmcts.payment.api.dto.LiberataSupplementaryInfo;
import uk.gov.hmcts.payment.api.dto.LiberataSupplementaryDetails;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.email.Email;
import uk.gov.hmcts.payment.api.email.EmailFailedException;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.exception.IACServiceException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentProvider;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.IacService;
import uk.gov.hmcts.payment.api.service.PaymentService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PBAPaymentMapperTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private FeesService feesService;

    @Mock
    private IacService iacService;

    @Mock
    private EmailService emailService;

    private PBAPaymentMapper pbaPaymentMapper;

    @Before
    public void setUp() {
        pbaPaymentMapper = new PBAPaymentMapper(paymentService, feesService, iacService, emailService,
            "from@example.test", new String[]{"payments@example.test"},
            "The IAC response for payment reference {paymentReference} is missing");
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
        when(paymentService.getServiceNameByCode(anyString())).thenReturn("NON-IAC-SERVICE");

        // act
        uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest accountRequest = pbaPaymentMapper.mapToPaymentByAccountRequest(request, "group-ref", payment, pfl);

        // assert
        assertThat(accountRequest).isNotNull();
        PaymentByAccountRequest inner = accountRequest.getPaymentByAccountRequest();
        assertThat(inner).isNotNull();
        assertThat(inner.getPbaNumber()).isEqualTo("PBA-1");
        assertThat(inner.getPayment()).isNotNull();
        assertThat(inner.getPayment().getSurname()).isEqualTo("Not applicable, the service type is not IAC");
    }

    @Test
    public void mapToPaymentByAccountRequest_IAC_should_use_iac_service_surname() {
        // arrange
        Payment payment = Payment.paymentWith()
            .serviceType(PBAPaymentMapper.IAC_SERVICE_NAME)
            .reference("RC-2")
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

        when(paymentService.getServiceNameByCode(anyString())).thenReturn(PBAPaymentMapper.IAC_SERVICE_NAME);
        when(iacService.getIacSupplementaryInfo(anyList(), anyString())).thenReturn(new ResponseEntity<>(supplementary, HttpStatus.OK));

        // act
        uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest accountRequest = pbaPaymentMapper.mapToPaymentByAccountRequest(request, "group-ref", payment, pfl);

        // assert
        assertThat(accountRequest).isNotNull();
        PaymentByAccountRequest inner = accountRequest.getPaymentByAccountRequest();
        assertThat(inner).isNotNull();
        assertThat(inner.getPayment().getSurname()).isEqualTo("Smith");
        verifyNoInteractions(emailService);
    }

    @Test
    public void mapToPaymentByAccountRequest_IAC_missingResponseData_reportsBothMissingFields() {
        mockIacServiceName();
        ResponseEntity<SupplementaryPaymentDto> nullBodyResponse =
            new ResponseEntity<>((SupplementaryPaymentDto) null, HttpStatus.OK);
        ResponseEntity<SupplementaryPaymentDto> nullInfoResponse = new ResponseEntity<>(
            SupplementaryPaymentDto.supplementaryPaymentDtoWith().supplementaryInfo(null).build(), HttpStatus.OK);
        ResponseEntity<SupplementaryPaymentDto> emptyInfoResponse = new ResponseEntity<>(
            SupplementaryPaymentDto.supplementaryPaymentDtoWith().supplementaryInfo(Collections.emptyList()).build(), HttpStatus.OK);
        ResponseEntity<SupplementaryPaymentDto> nullFirstInfoResponse = new ResponseEntity<>(
            SupplementaryPaymentDto.supplementaryPaymentDtoWith()
                .supplementaryInfo(Collections.<LiberataSupplementaryInfo>singletonList(null)).build(), HttpStatus.OK);
        List<ResponseEntity<SupplementaryPaymentDto>> responses = Arrays.asList(
            null, nullBodyResponse, nullInfoResponse, emptyInfoResponse, nullFirstInfoResponse);

        for (ResponseEntity<SupplementaryPaymentDto> response : responses) {
            when(iacService.getIacSupplementaryInfo(anyList(), anyString())).thenReturn(response);
            assertMissingFields();
        }

        org.mockito.ArgumentCaptor<Email> emailCaptor = org.mockito.ArgumentCaptor.forClass(Email.class);
        verify(emailService, times(responses.size())).sendEmail(emailCaptor.capture());
        assertThat(emailCaptor.getAllValues()).allSatisfy(email -> {
            assertThat(email.getSubject()).isEqualTo("IAC PBA Payment Failed");
            assertThat(email.getMessage()).isEqualTo("The IAC response for payment reference {paymentReference} is missing");
        });
    }

    @Test
    public void mapToPaymentByAccountRequest_IAC_missingSurname_reportsOnlySurname() {
        mockIacServiceName();
        mockIacResponse(supplementaryInfo("ccd-1", "  "));

        assertMissingFields();

        org.mockito.ArgumentCaptor<Email> emailCaptor = org.mockito.ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(emailCaptor.capture());
        assertThat(emailCaptor.getValue().getSubject()).isEqualTo("IAC PBA Payment Failed");
        assertThat(emailCaptor.getValue().getMessage()).isEqualTo("The IAC response for payment reference {paymentReference} is missing");
    }

    @Test
    public void mapToPaymentByAccountRequest_IAC_missingCcdCaseNumber_reportsOnlyCcdCaseNumber() {
        mockIacServiceName();
        mockIacResponse(supplementaryInfo("  ", "Smith"));

        assertMissingFields();

        org.mockito.ArgumentCaptor<Email> emailCaptor = org.mockito.ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(emailCaptor.capture());
        assertThat(emailCaptor.getValue().getSubject()).isEqualTo("IAC PBA Payment Failed");
        assertThat(emailCaptor.getValue().getMessage()).isEqualTo("The IAC response for payment reference {paymentReference} is missing");
    }

    @Test
    public void mapToPaymentByAccountRequest_IAC_missingSupplementaryDetails_reportsSurnameOnly() {
        mockIacServiceName();
        LiberataSupplementaryInfo info = LiberataSupplementaryInfo.liberataSupplementaryInfoWith()
            .ccdCaseNumber("ccd-1")
            .supplementaryDetails(null)
            .build();
        mockIacResponse(info);

        assertMissingFields();
    }


    @Test
    public void mapToPaymentByAccountRequest_IAC_serviceExceptionIsPassedThrough() {
        mockIacServiceName();
        when(iacService.getIacSupplementaryInfo(anyList(), anyString()))
            .thenThrow(new EmailFailedException("IAC unavailable"));

        assertThatThrownBy(this::mapIacPayment)
            .isInstanceOf(EmailFailedException.class)
            .hasMessageContaining("IAC unavailable");
        verifyNoInteractions(emailService);
    }

    @Test
    public void getSurname_whenIacServiceThrowsException_wrapsItInEmailFailedException() {
        mockIacServiceName();
        when(iacService.getIacSupplementaryInfo(anyList(), anyString()))
            .thenThrow(new IllegalStateException("IAC unavailable"));

        assertThatThrownBy(this::mapIacPayment)
            .isInstanceOf(EmailFailedException.class)
            .hasMessageContaining("payment reference: group-ref")
            .hasMessageContaining("IAC unavailable");
        verifyNoInteractions(emailService);
    }

    @Test
    public void getSurname_whenFieldsAreMissing_substitutesConfiguredEmailMessageTokens() {
        mockIacServiceName();
        mockIacResponse(supplementaryInfo("ccd-1", null));
        pbaPaymentMapper = new PBAPaymentMapper(paymentService, feesService, iacService, emailService,
            "from@example.test", new String[]{"payments@example.test"},
            "Surname: {surname}<br>Case Reference: {caseReference}"
                + "<br>16 Digit Reference: {16digitReference}<br>Date/Time: {timestamp}"
                + "<br>Service: Immigration & Asylum (IAC)");

        assertThatThrownBy(this::mapIacPayment).isInstanceOf(IACServiceException.class);

        org.mockito.ArgumentCaptor<Email> emailCaptor = org.mockito.ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(emailCaptor.capture());
        assertThat(emailCaptor.getValue().getMessage())
            .startsWith("Surname: surname not found<br>Case Reference: ccd-1"
                + "<br>16 Digit Reference: ccd-1<br>Date/Time: ")
            .endsWith("<br>Service: Immigration & Asylum (IAC)")
            .doesNotContain("{surname}", "{caseReference}", "{16digitReference}", "{timestamp}");
    }

    @Test
    public void mapToPaymentByAccountRequest_IAC_emailFailureIsHandledByCatch() {
        mockIacServiceName();
        mockIacResponse(supplementaryInfo("ccd-1", null));
        doThrow(new EmailFailedException("SMTP unavailable"))
            .when(emailService).sendEmail(org.mockito.ArgumentMatchers.any(Email.class));

        assertThatThrownBy(this::mapIacPayment)
            .isInstanceOf(EmailFailedException.class)
            .hasMessageContaining("SMTP unavailable");
        verify(emailService).sendEmail(org.mockito.ArgumentMatchers.any(Email.class));
    }

    @Test
    public void mapToPaymentByAccountRequestFromServiceRequestPaymentDto_nonIAC_should_set_not_applicable_surname_and_map_fees() {
        // arrange
        Payment payment = Payment.paymentWith()
            .serviceType("CMC")
            .reference("RC-3")
            .build();

        PaymentFee fee = PaymentFee.feeWith()
            .id(111)
            .code("FEE0001")
            .version("1")
            .calculatedAmount(new BigDecimal("100.00"))
            .volume(2)
            .build();

        PaymentFeeLink pfl = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("group-ref-sr")
            .enterpriseServiceName("CMC")
            .orgId("SITE1")
            .caseReference("case-ref")
            .ccdCaseNumber("ccd-1")
            .fees(Collections.singletonList(fee))
            .build();

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .amount(new BigDecimal("100.00"))
            .currency("GBP")
            .customerReference("cust-ref")
            .accountNumber("PBA-3")
            .build();

        FeeVersionDto feeVersionDto = FeeVersionDto.feeVersionDtoWith()
            .memoLine("memo")
            .naturalAccountCode("NAC1")
            .build();


        Fee2Dto fee2Dto = Fee2Dto.fee2DtoWith().build();

        val jurisdiction1Dto = uk.gov.hmcts.fees2.register.api.contract.Jurisdiction1Dto.jurisdiction1TypeDtoWith().name("J1").build();
        fee2Dto.setJurisdiction1Dto(jurisdiction1Dto);

        Map<String, Fee2Dto> feeMap = new HashMap<>();
        feeMap.put("FEE0001", fee2Dto);

        when(paymentService.getServiceNameByCode(anyString())).thenReturn("NON-IAC-SERVICE");
        when(feesService.getFeeVersion("FEE0001", "1")).thenReturn(java.util.Optional.of(feeVersionDto));
        when(feesService.getFeesDtoMap()).thenReturn(feeMap);

        // act
        uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest accountRequest =
            pbaPaymentMapper.mapToPaymentByAccountRequestFromServiceRequestPaymentDto(serviceRequestPaymentDto,
                "ignored-group-ref", payment, pfl);

        // assert
        assertThat(accountRequest).isNotNull();
        PaymentByAccountRequest inner = accountRequest.getPaymentByAccountRequest();
        assertThat(inner).isNotNull();
        assertThat(inner.getPbaNumber()).isEqualTo("PBA-3");
        assertThat(inner.getPayment().getSurname()).isEqualTo("Not applicable, the service type is not IAC");
        assertThat(inner.getPayment().getFee()).hasSize(1);
        assertThat(inner.getPayment().getFee().get(0).getCode()).isEqualTo("FEE0001");
        assertThat(inner.getPayment().getFee().get(0).getVersion()).isEqualTo("1");
        assertThat(inner.getPayment().getFee().get(0).getId()).isEqualTo(111);
        assertThat(inner.getPayment().getFee().get(0).getCalculatedAmount()).isEqualTo("100.00");
        assertThat(inner.getPayment().getFee().get(0).getVolume()).isEqualTo("2");
    }

    @Test
    public void mapToPaymentByAccountRequestFromServiceRequestPaymentDto_IAC_should_use_iac_surname_and_default_volume_to_one() {
        // arrange
        Payment payment = Payment.paymentWith()
            .serviceType(PBAPaymentMapper.IAC_SERVICE_NAME)
            .reference("RC-4")
            .siteId("SITE1")
            .amount(new BigDecimal("25.00"))
            .caseReference("case-ref")
            .ccdCaseNumber("ccd-1")
            .pbaNumber("PBA-4")
            .organisationName("org")
            .customerReference("cust")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .currency("GBP")
            .build();

        PaymentFee fee = PaymentFee.feeWith()
            .id(222)
            .code("FEE0002")
            .version("2")
            .calculatedAmount(new BigDecimal("25.00"))
            .volume(0)
            .build();

        PaymentFeeLink pfl = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("group-ref-iac")
            .enterpriseServiceName(PBAPaymentMapper.IAC_SERVICE_NAME)
            .orgId("SITE1")
            .caseReference("case-ref")
            .ccdCaseNumber("ccd-1")
            .fees(Collections.singletonList(fee))
            .build();

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .amount(new BigDecimal("25.00"))
            .currency("GBP")
            .customerReference("cust")
            .accountNumber("PBA-4")
            .build();

        FeeVersionDto feeVersionDto = FeeVersionDto.feeVersionDtoWith()
            .memoLine("memo-2")
            .naturalAccountCode("NAC2")
            .build();

        Fee2Dto fee2Dto = Fee2Dto.fee2DtoWith().build();
        val jurisdiction1Dto = uk.gov.hmcts.fees2.register.api.contract.Jurisdiction1Dto.jurisdiction1TypeDtoWith().name("J2").build();
        fee2Dto.setJurisdiction1Dto(jurisdiction1Dto);

        Map<String, Fee2Dto> feeMap = new HashMap<>();
        feeMap.put("FEE0002", fee2Dto);

        LiberataSupplementaryDetails details = LiberataSupplementaryDetails.supplementaryDetailsWith().surname("Walker").build();
        LiberataSupplementaryInfo info = LiberataSupplementaryInfo.liberataSupplementaryInfoWith()
            .ccdCaseNumber("ccd-1")
            .supplementaryDetails(details)
            .build();
        SupplementaryPaymentDto supplementary = SupplementaryPaymentDto.supplementaryPaymentDtoWith()
            .supplementaryInfo(Collections.singletonList(info))
            .build();

        when(feesService.getFeeVersion("FEE0002", "2")).thenReturn(java.util.Optional.of(feeVersionDto));
        when(feesService.getFeesDtoMap()).thenReturn(feeMap);
        when(paymentService.getServiceNameByCode(anyString())).thenReturn(PBAPaymentMapper.IAC_SERVICE_NAME);
        when(iacService.getIacSupplementaryInfo(anyList(), anyString())).thenReturn(new ResponseEntity<>(supplementary, HttpStatus.OK));

        // act
        uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest accountRequest =
            pbaPaymentMapper.mapToPaymentByAccountRequestFromServiceRequestPaymentDto(serviceRequestPaymentDto,
                "group-ref-iac", payment, pfl);

        // assert
        assertThat(accountRequest).isNotNull();
        PaymentByAccountRequest inner = accountRequest.getPaymentByAccountRequest();
        assertThat(inner).isNotNull();
        assertThat(inner.getPayment().getSurname()).isEqualTo("Walker");
        assertThat(inner.getPayment().getFee()).hasSize(1);
        assertThat(inner.getPayment().getFee().get(0).getCode()).isEqualTo("FEE0002");
        assertThat(inner.getPayment().getFee().get(0).getVersion()).isEqualTo("2");
        assertThat(inner.getPayment().getFee().get(0).getId()).isEqualTo(222);
        assertThat(inner.getPayment().getFee().get(0).getCalculatedAmount()).isEqualTo("25.00");
        assertThat(inner.getPayment().getFee().get(0).getVolume()).isEqualTo("1");
    }

    private void mockIacServiceName() {
        when(paymentService.getServiceNameByCode(anyString())).thenReturn(PBAPaymentMapper.IAC_SERVICE_NAME);
    }

    private void mockIacResponse(LiberataSupplementaryInfo supplementaryInfo) {
        SupplementaryPaymentDto response = SupplementaryPaymentDto.supplementaryPaymentDtoWith()
            .supplementaryInfo(Collections.singletonList(supplementaryInfo))
            .build();
        when(iacService.getIacSupplementaryInfo(anyList(), anyString()))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
    }

    private LiberataSupplementaryInfo supplementaryInfo(String ccdCaseNumber, String surname) {
        LiberataSupplementaryDetails details = LiberataSupplementaryDetails.supplementaryDetailsWith()
            // String.replace rejects null replacement values; model an absent response field as blank.
            .surname(surname == null ? "" : surname)
            .build();
        return LiberataSupplementaryInfo.liberataSupplementaryInfoWith()
            .ccdCaseNumber(ccdCaseNumber)
            .supplementaryDetails(details)
            .build();
    }

    private void assertMissingFields() {
        assertThatThrownBy(this::mapIacPayment)
            .isInstanceOf(IACServiceException.class)
            .hasMessage(PBAPaymentMapper.IAC_ERROR_MSG);
    }

    private PaymentAccountRequest mapIacPayment() {
        Payment payment = Payment.paymentWith()
            .serviceType(PBAPaymentMapper.IAC_SERVICE_NAME)
            .payerName("a name")
            .reference("RC-test")
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
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .fees(Collections.emptyList())
            .build();
        return pbaPaymentMapper.mapToPaymentByAccountRequest(request, "group-ref", payment, paymentFeeLink);
    }
}
