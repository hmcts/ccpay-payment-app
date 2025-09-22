package uk.gov.hmcts.payment.api.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.poi.ss.formula.functions.T;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestPaymentDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestPaymentDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestOnlinePaymentBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.mapper.PBAStatusErrorMapper;
import uk.gov.hmcts.payment.api.model.IdempotencyKeys;
import uk.gov.hmcts.payment.api.model.IdempotencyKeysRepository;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentProvider;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;
import uk.gov.hmcts.payment.api.service.ReferenceDataServiceImpl;
import uk.gov.hmcts.payment.api.servicebus.TopicClientService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentGroupNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.ServiceRequestExceptionForNoAmountDue;
import uk.gov.hmcts.payment.api.v1.model.exceptions.ServiceRequestExceptionForNoMatchingAmount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(SpringRunner.class)
public class ServiceRequestDomainServiceTest {

    @Mock
    private ReferenceDataServiceImpl referenceDataService;

    @Mock
    ServiceRequestPaymentDtoDomainMapper serviceRequestPaymentDtoDomainMapper;

    @Mock
    ServiceRequestPaymentDomainDataEntityMapper serviceRequestPaymentDomainDataEntityMapper;

    @Spy
    private List<String> pbaConfig1ServiceNames;

    @Mock
    private ServiceRequestDtoDomainMapper serviceRequestDtoDomainMapper;

    @Mock
    PaymentGroupService paymentGroupService;

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Mock
    private ServiceRequestBo orderBo;

    @Mock
    DelegatingPaymentService<GovPayPayment, String> delegateGovPay;

    @Mock
    DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @Mock
    ServiceRequestDomainDataEntityMapper serviceRequestDomainDataEntityMapper;

    @Mock
    Payment2Repository paymentRepository;

    @Mock
    LaunchDarklyFeatureToggler featureToggler;

    @Mock
    FeePayApportionService feePayApportionService;

    @Mock
    AccountService<AccountDto, String> accountService;

    @Mock
    PaymentGroupDtoMapper paymentGroupDtoMapper;

    @Spy
    PBAStatusErrorMapper pbaStatusErrorMapper;

    @Mock
    IdempotencyKeysRepository idempotencyKeysRepository;

    @Mock
    PaymentDtoMapper paymentDtoMapper;

    @Mock
    private TopicClientService topicClientService;

    @InjectMocks
    private ServiceRequestDomainServiceImpl serviceRequestDomainService;

    @Before
    public void setup() {
    }

    @Test
    public void createOrderWithValidRequest() throws Exception {

        ServiceRequestDto orderDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(Collections.singletonList(getOrderFee()))
            .build();

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceCode("AA001")
            .serviceDescription("DIVORCE")
            .build();

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();

        when(referenceDataService.getOrganisationalDetail(any(),any(), any())).thenReturn(organisationalServiceDto);

        String orderReference = "2200-1619524583862";
        ServiceRequestResponseDto orderResponse = ServiceRequestResponseDto.serviceRequestResponseDtoWith()
                                            .serviceRequestReference(orderReference)
                                            .build();
        doReturn(orderResponse).when(orderBo).createServiceRequest(any());

        ServiceRequestResponseDto orderReferenceResult = serviceRequestDomainService.create(orderDto, header);

        assertThat(orderReference).isEqualTo(orderReferenceResult.getServiceRequestReference());

    }

    @Test
    public void businessValidationForServiceRequests() throws Exception {

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
                .accountNumber("1234").
                 amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN)).
                    build();


        PaymentFeeLink paymentFeeLink = serviceRequestDomainService
                .businessValidationForServiceRequests(getPaymentFeeLink(), serviceRequestPaymentDto);

        assertNotNull(paymentFeeLink);
    }

    @Test
    public void businessValidationForServiceRequestsServiceRequestExceptionForNoMatchingAmount() throws Exception {

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
                .accountNumber("1234").
                 amount(new BigDecimal(99.94)).
                    build();
        try {
            serviceRequestDomainService.businessValidationForServiceRequests(getPaymentFeeLink(),serviceRequestPaymentDto);
        }catch (ServiceRequestExceptionForNoMatchingAmount e){
            assertThat(e.getMessage()).isEqualTo("The amount should be equal to serviceRequest balance");
        }
    }

    @Test
    public void businessValidationForServiceRequestsServiceRequestExceptionForNoAmountDue() throws Exception {

         ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
             .accountNumber("1234").
             amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN)).
             build();

         PaymentFeeLink paymentFeeLink = getPaymentFeeLink();
         paymentFeeLink.setFees(Arrays.asList(PaymentFee.feeWith().
             amountDue(new BigDecimal(0)).
             calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0001").volume(1).build()));

         try {
             serviceRequestDomainService.businessValidationForServiceRequests(paymentFeeLink,serviceRequestPaymentDto);
         }catch (ServiceRequestExceptionForNoAmountDue e){
             assertThat(e.getMessage()).isEqualTo("The serviceRequest has already been paid");
         }

    }

    @Test
    public void createIdempotencyRecordTest() throws Exception {

         ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
             .accountNumber("1234")
             .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
             .customerReference("cust_ref")
                 .build();

        ObjectMapper objectMapper = new ObjectMapper();

        String requestJson = objectMapper.writeValueAsString(serviceRequestPaymentDto);

        ResponseEntity<?> responseEntity = new ResponseEntity<T>(HttpStatus.CREATED);

        IdempotencyKeys idempotencyRecord = IdempotencyKeys
            .idempotencyKeysWith()
            .idempotencyKey("idempotencyKey")
            .requestBody(requestJson)
            .requestHashcode(123)   //save the hashcode
            .responseBody("")
            .responseStatus(IdempotencyKeys.ResponseStatusType.pending)
            .build();

        ResponseEntity idempotencyPendingRecordResponseEntity = serviceRequestDomainService.createIdempotencyRecord(objectMapper,"idempotencyKey", "RC-ref",
            "{\"response_body\":\"response_body\"}", idempotencyRecord.getResponseStatus(), responseEntity,serviceRequestPaymentDto);

        assertNotNull(idempotencyPendingRecordResponseEntity);
        AssertionsForClassTypes.assertThat(idempotencyPendingRecordResponseEntity.getStatusCode().equals(null));

        idempotencyRecord = IdempotencyKeys
            .idempotencyKeysWith()
            .idempotencyKey("idempotencyKey")
            .requestBody(requestJson)
            .requestHashcode(123)   //save the hashcode
            .responseBody("")
            .responseCode(201)
            .responseStatus(IdempotencyKeys.ResponseStatusType.completed)
            .build();

        ResponseEntity idempotencyCompletedRecordResponseEntity = serviceRequestDomainService.createIdempotencyRecord(objectMapper,"idempotencyKey", "RC-ref",
            "{\"response_body\":\"response_body\"}", idempotencyRecord.getResponseStatus(), responseEntity,serviceRequestPaymentDto);

        assertNotNull(idempotencyCompletedRecordResponseEntity);
        assertThat(idempotencyCompletedRecordResponseEntity.getStatusCode().equals(HttpStatus.CREATED));

        verify(idempotencyKeysRepository, times(2)).findById(any());
        verify(idempotencyKeysRepository, times(2)).saveAndFlush(any());
    }

    @Test
    public void createIdempotencyPendingRecordTest() throws Exception {

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("1234")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .customerReference("cust_ref")
            .build();

        ObjectMapper objectMapper = new ObjectMapper();

        String requestJson = objectMapper.writeValueAsString(serviceRequestPaymentDto);

        IdempotencyKeys idempotencyRecord = IdempotencyKeys
            .idempotencyKeysWith()
            .idempotencyKey("idempotencyKey")
            .requestBody(requestJson)
            .requestHashcode(123)   //save the hashcode
            .responseBody("")
            .responseStatus(IdempotencyKeys.ResponseStatusType.pending)
            .build();

        ResponseEntity<?> responseEntity = new ResponseEntity<T>(HttpStatus.CREATED);

        ResponseEntity idempotencyRecordResponseEntity = serviceRequestDomainService.createIdempotencyRecord(objectMapper, "", "RC-ref",
            "{\"response_body\":\"response_body\"}", idempotencyRecord.getResponseStatus(), responseEntity, serviceRequestPaymentDto);

        assertNotNull(idempotencyRecordResponseEntity);
        assertThat(idempotencyRecordResponseEntity.getStatusCode().equals(null));
        verify(idempotencyKeysRepository, times(1)).findById(any());
        verify(idempotencyKeysRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void addPaymentsTest() throws Exception {

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("1234")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
                .build();

        ServiceRequestPaymentBo serviceRequestPaymentBo = ServiceRequestPaymentBo.serviceRequestPaymentBoWith()
                 .paymentReference("RC-ref")
                     .build();

        Payment payment = Payment.paymentWith()
                 .paymentLink(getPaymentFeeLink())
                 .currency("GBP")
                .paymentMethod(PaymentMethod.paymentMethodWith().name("Online").build())
                 .paymentStatus(PaymentStatus.SUCCESS)
                     .build();

        Payment paymentFailed = Payment.paymentWith()
             .paymentLink(getPaymentFeeLink())
             .currency("GBP")
             .paymentMethod(PaymentMethod.paymentMethodWith().name("Online").build())
             .paymentStatus(PaymentStatus.FAILED)
             .build();

        PaymentReference paymentReference = PaymentReference.paymentReference()
             .caseReference("1234")
             .paymentAmount(BigDecimal.valueOf(24244.60))
             .accountNumber("12344")
             .paymentReference("ABC")
             .paymentMethod("ONLINE")
             .build();

        PaymentStatusDto paymentStatusDto = PaymentStatusDto.paymentStatusDto()
            .serviceRequestReference("ABC123")
            .ccdCaseNumber("12345")
            .serviceRequestAmount(BigDecimal.valueOf(12300.00))
            .serviceRequestStatus("PAID")
            .payment(paymentReference)
            .build();

        when(serviceRequestPaymentDtoDomainMapper.toDomain(any())).thenReturn(serviceRequestPaymentBo);

        when(serviceRequestPaymentDomainDataEntityMapper.toEntity(any(),any())).thenReturn(payment,paymentFailed);

        when(paymentFeeLinkRepository.findByPaymentReference(anyString())).thenReturn(Optional.of(getPaymentFeeLink()));

        when(paymentDtoMapper.toPaymentStatusDto(any(),any(),any(), any())).thenReturn(paymentStatusDto);

        AccountDto accountDto = AccountDto.accountDtoWith()
             .accountNumber("1234")
                 .build();

        when(accountService.retrieve("1234")).thenReturn(accountDto);

        PaymentGroupDto paymentGroupDto = new PaymentGroupDto();
        paymentGroupDto.setServiceRequestStatus("Paid");
        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(paymentGroupDto);

        ServiceRequestPaymentBo bo =
             serviceRequestDomainService.addPayments(getPaymentFeeLink(), "123", serviceRequestPaymentDto);

        assertNull(bo);
    }

    @Test
    public void addPaymentsTimeoutExceptionThrownTest() throws Exception {

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("1234")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .build();

        ServiceRequestPaymentBo serviceRequestPaymentBo = ServiceRequestPaymentBo.serviceRequestPaymentBoWith()
            .paymentReference("RC-ref")
            .build();

        Payment payment = Payment.paymentWith()
            .paymentLink(getPaymentFeeLink())
            .currency("GBP")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("Online").build())
            .paymentStatus(PaymentStatus.SUCCESS)
            .build();

        Payment paymentFailed = Payment.paymentWith()
            .paymentLink(getPaymentFeeLink())
            .currency("GBP")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("Online").build())
            .paymentStatus(PaymentStatus.FAILED)
            .build();

        PaymentReference paymentReference = PaymentReference.paymentReference()
            .caseReference("1234")
            .paymentAmount(BigDecimal.valueOf(24244.60))
            .accountNumber("12344")
            .paymentReference("ABC")
            .paymentMethod("ONLINE")
            .build();

        PaymentStatusDto paymentStatusDto = PaymentStatusDto.paymentStatusDto()
            .serviceRequestReference("ABC123")
            .ccdCaseNumber("12345")
            .serviceRequestAmount(BigDecimal.valueOf(12300.00))
            .serviceRequestStatus("PAID")
            .payment(paymentReference)
            .build();

        when(serviceRequestPaymentDtoDomainMapper.toDomain(any())).thenReturn(serviceRequestPaymentBo);

        when(serviceRequestPaymentDomainDataEntityMapper.toEntity(any(),any())).thenReturn(payment,paymentFailed);

        when(paymentFeeLinkRepository.findByPaymentReference(anyString())).thenReturn(Optional.of(getPaymentFeeLink()));

        when(paymentDtoMapper.toPaymentStatusDto(any(),any(),any(), any())).thenReturn(paymentStatusDto);

        when(accountService.retrieve(serviceRequestPaymentDto.getAccountNumber())).thenThrow(
            new ResourceAccessException("Error thrown"));

        AccountDto accountDto = AccountDto.accountDtoWith()
            .accountNumber("1234")
            .build();

        PaymentGroupDto paymentGroupDto = new PaymentGroupDto();
        paymentGroupDto.setServiceRequestStatus("Paid");
        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(paymentGroupDto);
        try{
            ServiceRequestPaymentBo bo =
                serviceRequestDomainService.addPayments(getPaymentFeeLink(), "123", serviceRequestPaymentDto);
        }catch(Exception ex){
            assertEquals(ex.getMessage(),"Unable to retrieve account information due to timeout");
        }

    }

    @Test
    public void addPaymentsExceptionThrownTest() throws Exception {

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("1234")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .build();

        ServiceRequestPaymentBo serviceRequestPaymentBo = ServiceRequestPaymentBo.serviceRequestPaymentBoWith()
            .paymentReference("RC-ref")
            .build();

        Payment payment = Payment.paymentWith()
            .paymentLink(getPaymentFeeLink())
            .currency("GBP")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("Online").build())
            .paymentStatus(PaymentStatus.SUCCESS)
            .build();

        Payment paymentFailed = Payment.paymentWith()
            .paymentLink(getPaymentFeeLink())
            .currency("GBP")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("Online").build())
            .paymentStatus(PaymentStatus.FAILED)
            .build();

        PaymentReference paymentReference = PaymentReference.paymentReference()
            .caseReference("1234")
            .paymentAmount(BigDecimal.valueOf(24244.60))
            .accountNumber("12344")
            .paymentReference("ABC")
            .paymentMethod("ONLINE")
            .build();

        PaymentStatusDto paymentStatusDto = PaymentStatusDto.paymentStatusDto()
            .serviceRequestReference("ABC123")
            .ccdCaseNumber("12345")
            .serviceRequestAmount(BigDecimal.valueOf(12300.00))
            .serviceRequestStatus("PAID")
            .payment(paymentReference)
            .build();

        when(serviceRequestPaymentDtoDomainMapper.toDomain(any())).thenReturn(serviceRequestPaymentBo);

        when(serviceRequestPaymentDomainDataEntityMapper.toEntity(any(),any())).thenReturn(payment,paymentFailed);

        when(paymentFeeLinkRepository.findByPaymentReference(anyString())).thenReturn(Optional.of(getPaymentFeeLink()));

        when(paymentDtoMapper.toPaymentStatusDto(any(),any(),any(), any())).thenReturn(paymentStatusDto);

        when(accountService.retrieve(serviceRequestPaymentDto.getAccountNumber())).thenThrow(
            new RuntimeException("unknown Exception"));

        AccountDto accountDto = AccountDto.accountDtoWith()
            .accountNumber("1234")
            .build();

        PaymentGroupDto paymentGroupDto = new PaymentGroupDto();
        paymentGroupDto.setServiceRequestStatus("Paid");
        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(paymentGroupDto);
        try{
            ServiceRequestPaymentBo bo =
                serviceRequestDomainService.addPayments(getPaymentFeeLink(), "123", serviceRequestPaymentDto);
        }catch(Exception ex){
            assertEquals(ex.getMessage(),"Unable to retrieve account information, please try again later");
        }

    }


    @Test
    public void addPaymentsHttpClientExceptionThrownTest() throws Exception {

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("1234")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .build();

        ServiceRequestPaymentBo serviceRequestPaymentBo = ServiceRequestPaymentBo.serviceRequestPaymentBoWith()
            .paymentReference("RC-ref")
            .build();

        Payment payment = Payment.paymentWith()
            .paymentLink(getPaymentFeeLink())
            .currency("GBP")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("Online").build())
            .paymentStatus(PaymentStatus.SUCCESS)
            .build();

        Payment paymentFailed = Payment.paymentWith()
            .paymentLink(getPaymentFeeLink())
            .currency("GBP")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("Online").build())
            .paymentStatus(PaymentStatus.FAILED)
            .build();

        PaymentReference paymentReference = PaymentReference.paymentReference()
            .caseReference("1234")
            .paymentAmount(BigDecimal.valueOf(24244.60))
            .accountNumber("12344")
            .paymentReference("ABC")
            .paymentMethod("ONLINE")
            .build();

        PaymentStatusDto paymentStatusDto = PaymentStatusDto.paymentStatusDto()
            .serviceRequestReference("ABC123")
            .ccdCaseNumber("12345")
            .serviceRequestAmount(BigDecimal.valueOf(12300.00))
            .serviceRequestStatus("PAID")
            .payment(paymentReference)
            .build();

        when(serviceRequestPaymentDtoDomainMapper.toDomain(any())).thenReturn(serviceRequestPaymentBo);

        when(serviceRequestPaymentDomainDataEntityMapper.toEntity(any(),any())).thenReturn(payment,paymentFailed);

        when(accountService.retrieve(serviceRequestPaymentDto.getAccountNumber())).thenThrow(
            HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found", null, null, null));

        AccountDto accountDto = AccountDto.accountDtoWith()
            .accountNumber("1234")
            .build();

        PaymentGroupDto paymentGroupDto = new PaymentGroupDto();
        paymentGroupDto.setServiceRequestStatus("Paid");
        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(paymentGroupDto);
        try{
            ServiceRequestPaymentBo bo =
                serviceRequestDomainService.addPayments(getPaymentFeeLink(), "123", serviceRequestPaymentDto);
        }catch(Exception ex){
            assertEquals(ex.getMessage(),"Account information could not be found");
        }

    }


    @Test
    public void createOnlineCardPaymentTest() throws Exception {

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .language("Eng")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .build();

        when(paymentFeeLinkRepository.findByPaymentReference(anyString())).thenReturn(Optional.of(getPaymentFeeLink()));

        ServiceRequestOnlinePaymentBo serviceRequestOnlinePaymentBo = ServiceRequestOnlinePaymentBo.serviceRequestOnlinePaymentBo()
                .paymentReference("RC-ref")
                    .build();

        when(serviceRequestDtoDomainMapper.toDomain(any(),any(),any())).thenReturn(serviceRequestOnlinePaymentBo);

        GovPayPayment govPayPayment = GovPayPayment.govPaymentWith()
                .paymentId("id")
                    .build();

        when(delegateGovPay.create(any(CreatePaymentRequest.class),anyString())).thenReturn(govPayPayment);

        Payment payment = Payment.paymentWith()
                .paymentLink(getPaymentFeeLink())
                .currency("GBP")
                .paymentStatus(PaymentStatus.CREATED)
                    .build();

        when(serviceRequestDomainDataEntityMapper.toPaymentEntity(any(),any(), any())).thenReturn(payment);

        when(paymentRepository.save(any())).thenReturn(payment);

        when(featureToggler.getBooleanValue(any(),any())).thenReturn(true);

        ResponseEntity<OnlineCardPaymentResponse> onlineCardPaymentResponse = serviceRequestDomainService.create(onlineCardPaymentRequest,"","","");

        assertNotNull(onlineCardPaymentResponse);
    }

    @Test
    public void createOnlineCardPaymentExistingSuccessPaymentFoundPayHubTest() throws Exception {

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .language("en")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .returnUrl("http://localhost:8080/paymentConfirmation")
            .build();

        PaymentProvider paymentProviderGovPay = PaymentProvider.paymentProviderWith().name("gov pay").build();
        PaymentProvider paymentProviderPciPal = PaymentProvider.paymentProviderWith().name("pci pal").build();

        Payment successfulPaymentPciPal = Payment.paymentWith().paymentChannel(PaymentChannel.ONLINE)
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .currency("GBP")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .paymentProvider(paymentProviderPciPal)
            .dateCreated(new Date())
            .reference("RC-1234-2345-3456-7654")
            .paymentLink(getPaymentFeeLink())
            .build();

        Payment successfulPayment = Payment.paymentWith().paymentChannel(PaymentChannel.ONLINE)
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .currency("GBP")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .paymentProvider(paymentProviderGovPay)
            .dateCreated(new Date())
            .reference("RC-1234-2345-3456-4567")
            .paymentLink(getPaymentFeeLink())
            .build();

        List<Payment> payments = Arrays.asList(successfulPaymentPciPal, successfulPayment);
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2024-1750000008178")
            .fees(Arrays.asList(PaymentFee.feeWith().amountDue(new BigDecimal(10))
                .calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0001").volume(1).build()))
            .payments(payments)
            .build();

        when(paymentFeeLinkRepository.findByPaymentReference(anyString())).thenReturn(Optional.of(paymentFeeLink));

        ResponseEntity<OnlineCardPaymentResponse> response = serviceRequestDomainService.create(
            onlineCardPaymentRequest,
            "",
            onlineCardPaymentRequest.getReturnUrl(),
            "");

        assertNotNull(response);
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(onlineCardPaymentRequest.getReturnUrl(), response.getHeaders().getLocation().toString());
        assertEquals("RC-1234-2345-3456-4567", successfulPayment.getReference());
        assertEquals("gov pay", successfulPayment.getPaymentProvider().getName());
        assertEquals("success", successfulPayment.getPaymentStatus().getName());
        assertEquals(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN), successfulPayment.getAmount());
        assertEquals("GBP", successfulPayment.getCurrency());
        assertEquals("card", successfulPayment.getPaymentMethod().getName());
        assertEquals(PaymentChannel.ONLINE, successfulPayment.getPaymentChannel());
        assertEquals("2024-1750000008178", paymentFeeLink.getPaymentReference());
        assertEquals(new BigDecimal(10), paymentFeeLink.getFees().get(0).getAmountDue());
        assertEquals(new BigDecimal("99.99"), paymentFeeLink.getFees().get(0).getCalculatedAmount());
        assertEquals("FEE0001", paymentFeeLink.getFees().get(0).getCode());
        assertEquals("1", paymentFeeLink.getFees().get(0).getVersion());
        assertEquals(1, paymentFeeLink.getFees().get(0).getVolume());
    }

    @Test
    public void createOnlineCardPaymentExistingSuccessPaymentFoundGovPayTest() throws Exception {

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .language("en")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .returnUrl("http://localhost:8080/paymentConfirmation")
            .build();

        PaymentProvider paymentProviderGovPay = PaymentProvider.paymentProviderWith().name("gov pay").build();
        PaymentProvider paymentProviderPciPal = PaymentProvider.paymentProviderWith().name("pci pal").build();

        Payment cancelledPayment = Payment.paymentWith().paymentChannel(PaymentChannel.ONLINE)
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .currency("GBP")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .paymentStatus(PaymentStatus.paymentStatusWith().name(PaymentStatus.CANCELLED.getName()).build())
            .paymentProvider(paymentProviderPciPal)
            .dateCreated(new Date())
            .reference("RC-1234-2345-3456-7654")
            .externalReference("72f8303f-47fa-4633-af89-54000e78283b")
            .paymentLink(getPaymentFeeLink())
            .build();

        Date hundredMinAgo = new Date(System.currentTimeMillis() - 100 * 60 * 1000);
        Payment createdPaymentOutdated = Payment.paymentWith().paymentChannel(PaymentChannel.ONLINE)
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .currency("GBP")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .paymentStatus(PaymentStatus.paymentStatusWith().name(PaymentStatus.CREATED.getName()).build())
            .paymentProvider(paymentProviderGovPay)
            .dateCreated(hundredMinAgo)
            .reference("RC-1234-2345-3456-9876")
            .externalReference("91af658a-d0ab-449e-9b30-5dd17082f2e8")
            .paymentLink(getPaymentFeeLink())
            .build();

        Payment createdPayment = Payment.paymentWith().paymentChannel(PaymentChannel.ONLINE)
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .currency("GBP")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .paymentStatus(PaymentStatus.paymentStatusWith().name(PaymentStatus.CREATED.getName()).build())
            .paymentProvider(paymentProviderGovPay)
            .dateCreated(new Date())
            .reference("RC-1234-2345-3456-4567")
            .externalReference("3c937cbb-d4c5-4b50-b3a0-5eb2f1f5dbec")
            .paymentLink(getPaymentFeeLink())
            .build();

        List<Payment> payments = Arrays.asList(cancelledPayment,createdPayment);
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2024-1750000008178")
            .fees(Arrays.asList(PaymentFee.feeWith().amountDue(new BigDecimal(10))
                .calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0001").volume(1).build()))
            .payments(payments)
            .build();

        when(paymentFeeLinkRepository.findByPaymentReference(anyString())).thenReturn(Optional.of(paymentFeeLink));

        GovPayPayment successfulGovPayPayment = GovPayPayment.govPaymentWith()
            .amount(9999)
            .state(new State("success", true, null, null))
            .description("description")
            .reference("RC-1234-2345-3456-4567")
            .paymentId("paymentId")
            .paymentProvider("gov pay")
            .returnUrl("http://localhost:8080/paymentConfirmation")
            .links(GovPayPayment.Links.linksWith().cancel(new Link("any", ImmutableMap.of(), "cancelHref", "any")).build())
            .build();

        when(delegateGovPay.retrieve(anyString())).thenReturn(successfulGovPayPayment);

        ResponseEntity<OnlineCardPaymentResponse> response = serviceRequestDomainService.create(
            onlineCardPaymentRequest,
            "",
            onlineCardPaymentRequest.getReturnUrl(),
            "");

        assertNotNull(response);
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(onlineCardPaymentRequest.getReturnUrl(), response.getHeaders().getLocation().toString());
        assertEquals("success", successfulGovPayPayment.getState().getStatus());
        assertEquals("RC-1234-2345-3456-4567", successfulGovPayPayment.getReference());
        assertEquals("RC-1234-2345-3456-4567", createdPayment.getReference());
        assertEquals("gov pay", createdPayment.getPaymentProvider().getName());
        assertEquals("created", createdPayment.getPaymentStatus().getName());
        assertEquals(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN), createdPayment.getAmount());
        assertEquals("GBP", createdPayment.getCurrency());
        assertEquals("card", createdPayment.getPaymentMethod().getName());
        assertEquals(PaymentChannel.ONLINE, createdPayment.getPaymentChannel());
        assertEquals("2024-1750000008178", paymentFeeLink.getPaymentReference());
        assertEquals(new BigDecimal(10), paymentFeeLink.getFees().get(0).getAmountDue());
        assertEquals(new BigDecimal("99.99"), paymentFeeLink.getFees().get(0).getCalculatedAmount());
        assertEquals("FEE0001", paymentFeeLink.getFees().get(0).getCode());
        assertEquals("1", paymentFeeLink.getFees().get(0).getVersion());
        assertEquals(1, paymentFeeLink.getFees().get(0).getVolume());
    }

    @Test
    public void createOnlineCardPaymentWithPaymentFeeLinksPaymentTest() throws Exception {

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .language("Eng")
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .build();
        PaymentFeeLink paymentFeeLinkMock = mock(PaymentFeeLink.class);

        when(paymentFeeLinkRepository.findByPaymentReference(anyString())).thenReturn(Optional.of(getPaymentFeeLinkWithPayments()));

        ServiceRequestOnlinePaymentBo serviceRequestOnlinePaymentBo = ServiceRequestOnlinePaymentBo.serviceRequestOnlinePaymentBo()
            .paymentReference("RC-ref")
            .build();

        when(serviceRequestDtoDomainMapper.toDomain(any(),any(),any())).thenReturn(serviceRequestOnlinePaymentBo);

        GovPayPayment govPayPayment = GovPayPayment.govPaymentWith()
            .paymentId("id")
            .build();

        when(delegateGovPay.create(any(CreatePaymentRequest.class),anyString())).thenReturn(govPayPayment);

        Payment payment = Payment.paymentWith()
            .paymentLink(getPaymentFeeLink())
            .currency("GBP")
            .paymentStatus(PaymentStatus.CREATED)
            .externalReference("paymentId")
            .build();

        when(serviceRequestDomainDataEntityMapper.toPaymentEntity(any(),any(), any())).thenReturn(payment);

        when(paymentRepository.save(any())).thenReturn(payment);

        when(delegatingPaymentService.retrieve(anyString())).thenReturn(paymentFeeLinkMock);

        when(delegateGovPay.retrieve(anyString())).thenReturn(getGovPayPayment());

        when(featureToggler.getBooleanValue(any(),any())).thenReturn(true);

        when(paymentFeeLinkMock.getPayments()).thenReturn(getPaymentFeeLinkWithPayments().getPayments());

        ResponseEntity<OnlineCardPaymentResponse> onlineCardPaymentResponse = serviceRequestDomainService.create(onlineCardPaymentRequest,"","","");

        assertNotNull(onlineCardPaymentResponse);

    }



    @Test()
    public void sendMessageTopicCPORequest() throws Exception {

        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .casePaymentRequest(getCasePaymentRequest())
            .build();

        Assertions.assertDoesNotThrow(() -> serviceRequestDomainService.sendMessageTopicCPO(serviceRequestDto,"ref"));

    }

    @Test
    public void findOrdersByCcdCaseNumber() throws Exception {
        when(paymentFeeLinkRepository.findByCcdCaseNumber(anyString())).thenReturn(Optional.of(Collections.singletonList(getPaymentFeeLink())));

        List<PaymentFeeLink> paymentFeeLinkList = serviceRequestDomainService.findByCcdCaseNumber("1607065108455502");

        assertThat(paymentFeeLinkList.get(0).getCcdCaseNumber()).isEqualTo("1607065108455502");
    }

    @Test
    public void isDuplicateTest() {
        when(paymentGroupService.findByPaymentGroupReference(any())).thenReturn(getPaymentFeeLink());
        boolean isDuplicate =serviceRequestDomainService.isDuplicate("1607065108455502");
        assertTrue(isDuplicate);
    }

    @Test
    public void findNoOrdersByCcdCaseNumber() throws Exception {
        when(paymentFeeLinkRepository.findByCcdCaseNumber(anyString())).thenReturn(Optional.of(Collections.emptyList()));

        try {
            List<PaymentFeeLink> paymentFeeLinkList = serviceRequestDomainService.findByCcdCaseNumber("1607065108455502");
        } catch (PaymentGroupNotFoundException e) {
            assertThat(e.getMessage()).isEqualTo("Order detail not found for given ccdcasenumber 1607065108455502");
        }
    }

    @Test
    public void testCanCancelPayment_AllConditionsMet() {
        // Arrange
        GovPayPayment payment = getGovPayPayment();

        // Act
        boolean result = serviceRequestDomainService.canCancelPayment(payment);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testCanCancelPayment_PaymentIsNull() {
        // Arrange

        // Act
        boolean result = serviceRequestDomainService.canCancelPayment(null);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testCanCancelPayment_LinksIsNull() {
        // Arrange
        GovPayPayment payment = getGovPayPayment();
        payment.setLinks(null);

        // Act
        boolean result = serviceRequestDomainService.canCancelPayment(payment);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testCanCancelPayment_LinkIsNull() {
        // Arrange
        GovPayPayment payment = getGovPayPayment();
        payment.getLinks().setCancel(null);

        // Act
        boolean result = serviceRequestDomainService.canCancelPayment(payment);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testCanCancelPayment_HrefIsNull() {
        // Arrange
        GovPayPayment payment = getGovPayPayment();
        payment.getLinks().getCancel().setHref(null);

        // Act
        boolean result = serviceRequestDomainService.canCancelPayment(payment);

        // Assert
        assertFalse(result);
    }


    @Test
    public void testCanCancelPayment_HrefIsEmpty() {
        // Arrange
        GovPayPayment payment = getGovPayPayment();
        payment.getLinks().getCancel().setHref("");

        // Act
        boolean result = serviceRequestDomainService.canCancelPayment(payment);

        // Assert
        assertFalse(result);
    }


    private ServiceRequestFeeDto getOrderFee() {
        return ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("92.19"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }

    private PaymentFeeLink getPaymentFeeLink() {
        return PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .orgId("org-id")
            .enterpriseServiceName("enterprise-service-name")
            .paymentReference("payment-ref")
            .ccdCaseNumber("1607065108455502")
            .callBackUrl("http://sercvice.com/callback")
            .fees(Arrays.asList(PaymentFee.feeWith().
                amountDue(new BigDecimal(10)).
                calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0001").volume(1).build()))
            .build();
    }

    private PaymentFeeLink getPaymentFeeLinkWithPayments() {
        List<Payment> payments = new LinkedList<>();
        Payment payment = new Payment();
        PaymentProvider paymentProvider = new PaymentProvider();
        paymentProvider.setName("gov pay");
        payment.setPaymentStatus(PaymentStatus.CREATED);
        payment.setPaymentProvider(paymentProvider);
        Date ninetyTwoAgo = new Date(System.currentTimeMillis() - 89 * 60 * 1000);
        payment.setDateCreated(ninetyTwoAgo);
        payment.setExternalReference("externalReference");
        payments.add(payment);

        return PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .orgId("org-id")
            .enterpriseServiceName("enterprise-service-name")
            .paymentReference("payment-ref")
            .payments(payments)
            .ccdCaseNumber("1607065108455502")
            .fees(Arrays.asList(PaymentFee.feeWith().
                amountDue(new BigDecimal(10)).
                calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0001").volume(1).build()))
            .build();
    }

    private CasePaymentRequest getCasePaymentRequest(){
        return CasePaymentRequest.casePaymentRequestWith().responsibleParty("party").action("action").build();
    }

    private GovPayPayment getGovPayPayment() {
        return GovPayPayment.govPaymentWith()
            .amount(300)
            .state(new State("created", false, null, null))
            .description("description")
            .reference("reference")
            .paymentId("paymentId")
            .paymentProvider("sandbox")
            .returnUrl("https://www.google.com")
            .links(GovPayPayment.Links.linksWith().cancel(new Link("any", ImmutableMap.of(), "cancelHref", "any")).build())
            .build();
    }

}
