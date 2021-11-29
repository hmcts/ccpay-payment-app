package uk.gov.hmcts.payment.api.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.formula.functions.T;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.mapper.PBAStatusErrorMapper;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentGroupNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.ServiceRequestExceptionForNoAmountDue;
import uk.gov.hmcts.payment.api.v1.model.exceptions.ServiceRequestExceptionForNoMatchingAmount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class ServiceRequestDomainServiceTest {

    @InjectMocks
    private ServiceRequestDomainServiceImpl serviceRequestDomainService;

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
    ServiceRequestDomainDataEntityMapper serviceRequestDomainDataEntityMapper;

    @Mock
    Payment2Repository paymentRepository;

    @Mock
    LaunchDarklyFeatureToggler featureToggler;

    @Mock
    FeePayApportionService feePayApportionService;

    @Mock
    AccountService accountService;

    @Spy
    PBAStatusErrorMapper pbaStatusErrorMapper;

    @Mock
    IdempotencyKeysRepository idempotencyKeysRepository;

    @Mock
    PaymentDtoMapper paymentDtoMapper;

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


        serviceRequestDomainService.businessValidationForServiceRequests(getPaymentFeeLink(),serviceRequestPaymentDto);

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

        IdempotencyKeys idempotencyRecord = IdempotencyKeys
            .idempotencyKeysWith()
            .idempotencyKey("idempotencyKey")
            .requestBody(requestJson)
            .request_hashcode(123)   //save the hashcode
            .responseBody("")
            .responseCode(201)
            .build();

        ResponseEntity<?> responseEntity = new ResponseEntity<T>(HttpStatus.CREATED);

        serviceRequestDomainService.createIdempotencyRecord(objectMapper,"", "RC-ref",
            "{\"response_body\":\"response_body\"}", responseEntity,serviceRequestPaymentDto);
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

         //when(paymentDtoMapper.toPaymentStatusDto(any(),any(),any())).thenReturn(paymentStatusDto);

         AccountDto accountDto = AccountDto.accountDtoWith()
                 .accountNumber("1234")
                     .build();

         when(accountService.retrieve(any())).thenReturn(accountDto);

         serviceRequestDomainService.addPayments(getPaymentFeeLink(),"123",serviceRequestPaymentDto);
         //serviceRequestDomainService.addPayments(getPaymentFeeLink(),"123", serviceRequestPaymentDto);

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

        serviceRequestDomainService.create(onlineCardPaymentRequest,"","","");

    }


    @Test()
    public void sendMessageTopicCPORequest() throws Exception {

        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .casePaymentRequest(getCasePaymentRequest())
            .build();

        serviceRequestDomainService.sendMessageTopicCPO(serviceRequestDto);

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
        serviceRequestDomainService.isDuplicate("1607065108455502");
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
            .fees(Arrays.asList(PaymentFee.feeWith().
                amountDue(new BigDecimal(10)).
                calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0001").volume(1).build()))
            .build();
    }

    private CasePaymentRequest getCasePaymentRequest(){
        return CasePaymentRequest.casePaymentRequestWith().responsibleParty("party").action("action").build();
    }

}
