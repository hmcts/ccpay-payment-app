package uk.gov.hmcts.payment.api.componenttests;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.RefundsFeeDto;
import uk.gov.hmcts.payment.api.dto.InternalRefundResponse;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.dto.ResubmitRefundRemissionRequest;
import uk.gov.hmcts.payment.api.dto.RetrospectiveRemissionRequest;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.idam.IdamUserIdResponse;
import uk.gov.hmcts.payment.api.exception.InvalidPartialRefundRequestException;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.IdamService;
import uk.gov.hmcts.payment.api.service.PaymentRefundsService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
public class PaymentRefundsServiceTest {

    final static MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
    PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
        .paymentReference("RC-1649-7555-9551-8774")
        .refundReason("RR037")
        .totalRefundAmount(BigDecimal.valueOf(118))
        .fees(
            Arrays.asList(
                RefundsFeeDto.refundFeeDtoWith()
                    .calculatedAmount(new BigDecimal("232"))
                    .apportionAmount(new BigDecimal("232"))
//                        .feeAmount(new BigDecimal("550.00"))
                    .code("FEE0326")
//                        .volume(1)
                    .id(610)
//                        .memoLine("Bar Cash")
//                        .naturalAccountCode("21245654433")
                    .version("2")
//                        .volume(1)
                    .refundAmount(new BigDecimal("118.00"))
                    .updatedVolume(1)
//                        .reference("REF_123")
                    .build()
            ))
        .contactDetails(ContactDetails.contactDetailsWith().notificationType(Notification.EMAIL.getNotification())
            .email("a@a.com").build())
        .build();


    Payment mockPaymentSuccess = Payment.paymentWith().reference("RC-1234-1234-1234-1234")
        .amount(BigDecimal.valueOf(100))
        .id(1)
        .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
        .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
        .paymentChannel(PaymentChannel.ONLINE)
        .paymentLink(PaymentFeeLink.paymentFeeLinkWith().fees(Arrays.asList(PaymentFee.feeWith().id(1).feeAmount(new BigDecimal(550)).volume(1).build())).build())
        .build();
    RetrospectiveRemissionRequest retrospectiveRemissionRequest =
        RetrospectiveRemissionRequest.retrospectiveRemissionRequestWith()
            .remissionReference("qwerty").contactDetails(
            ContactDetails.contactDetailsWith().notificationType(Notification.EMAIL.getNotification())
                .email("a@a.aa").build()).build();

    private static final IdamUserIdResponse IDAM_USER_ID_RESPONSE =
        IdamUserIdResponse.idamUserIdResponseWith().uid("1").givenName("XX").familyName("YY").name("XX YY")
            .roles(Arrays.asList("payments-refund-approver", "payments-refund")).sub("ZZ")
            .build();

    @MockBean
    private Payment2Repository paymentRepository;

    @MockBean
    private RemissionRepository remissionRepository;

    @MockBean
    private FeePayApportionRepository feePayApportionRepository;

    @MockBean
    @Autowired()
    @Qualifier("restTemplateRefundsGroup")
    private RestTemplate restTemplate;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private PaymentRefundsService paymentRefundsService;
    @MockBean
    private IdamService idamService;

    @Before
    public void setup() {
        header.put("Authorization", Collections.singletonList("Bearer 131313"));
    }

    @After
    public void tearDown() {
        header.clear();
    }

    @Test
    public void createSuccessfulRefund() throws Exception {

        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        InternalRefundResponse mockRefundResponse = InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();


        ResponseEntity<InternalRefundResponse> responseEntity = new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        ResponseEntity<RefundResponse> refundResponse = paymentRefundsService.createRefund(paymentRefundRequest, header);

        assertEquals("RF-4321-4321-4321-4321", refundResponse.getBody().getRefundReference());

    }

    @Test
    public void createRefundWithFailedReference() throws Exception {
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        Payment mockPaymentFailed = Payment.paymentWith().reference("RC-1234-1234-1234-1234")
            .amount(BigDecimal.valueOf(550))
            .id(1)
            .paymentStatus(PaymentStatus.paymentStatusWith().name("failed").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentLink(PaymentFeeLink.paymentFeeLinkWith().fees(Arrays.asList(PaymentFee.feeWith().id(1).volume(1).feeAmount(new BigDecimal(550)).build())).build())
            .build();

        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentFailed));

        /*Exception exception = assertThrows(
            PaymentNotSuccessException.class,
            () -> paymentRefundsService.createRefund(paymentRefundRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Refund can not be processed for unsuccessful payment"));*/

    }

    //@Test
    public void createRefundWithClientException() throws Exception {
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));


        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        Exception exception = assertThrows(
            InvalidRefundRequestException.class,
            () -> paymentRefundsService.createRefund(paymentRefundRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(""));
    }

    //@Test
    public void createRefundWithClientException1() throws Exception {
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        ResponseEntity<InternalRefundResponse> refundResponseResponseEntity = new ResponseEntity<>(null, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(refundResponseResponseEntity);

        Exception exception = assertThrows(
            HttpServerErrorException.class,
            () -> paymentRefundsService.createRefund(paymentRefundRequest, header)
        );

        String actualMessage = exception.getMessage();
       // assertTrue(actualMessage.contains(""));
    }


    //@Test
    public void createRefundWithServerException() throws Exception {
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        Exception exception = assertThrows(
            HttpServerErrorException.class,
            () -> paymentRefundsService.createRefund(paymentRefundRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("500 INTERNAL_SERVER_ERROR"));
    }


    @Test
    public void createSuccessfulRetroRemissionRefund() throws Exception {

        BigDecimal amount = new BigDecimal("11.99");
        PaymentFee fee = PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build();
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .paymentReference("2018-15202505035")
            .fees(Arrays.asList(fee))
            .build();

        Remission remission = Remission.remissionWith()
            .paymentFeeLink(paymentFeeLink)
            .remissionReference("qwerty")
            .fee(fee)
            .hwfAmount(amount)
            .hwfReference("poiuytrewq")
            .build();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .apportionAmount(amount)
            .paymentAmount(amount)
            .ccdCaseNumber("1234123412341234")
            .paymentLink(paymentFeeLink)
            .paymentId(1)
            .feeId(1)
            .id(1)
            .feeAmount(amount).build();
        Optional<List<FeePayApportion>> feeAppList = Optional.of(Arrays.asList(feePayApportion));
        Payment payment = Payment.paymentWith()
            .id(1)
            .amount(amount)
            .caseReference("caseReference")
            .description("retrieve payment mock test")
            .serviceType("Civil Money Claims")
            .siteId("siteID")
            .currency("GBP")
            .organisationName("organisationName")
            .customerReference("customerReference")
            .pbaNumber("pbaNumer")
            .reference("RC-1520-2505-0381-8145")
            .ccdCaseNumber("1234123412341234")
            .paymentLink(paymentFeeLink)
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .build();
        Mockito.when(remissionRepository.findByRemissionReference(any())).thenReturn(Optional.ofNullable(remission));

        Mockito.when(feePayApportionRepository.findByFeeId(any())).thenReturn(feeAppList);
        assertTrue(!feeAppList.isEmpty());

        Mockito.when(paymentRepository.findById(any())).thenReturn(Optional.ofNullable(payment));
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        InternalRefundResponse mockRefundResponse = InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        ResponseEntity<InternalRefundResponse> responseEntity = new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        ResponseEntity<RefundResponse> refundResponse = paymentRefundsService.createAndValidateRetrospectiveRemissionRequest(
            retrospectiveRemissionRequest, header);

        assertEquals("RF-4321-4321-4321-4321", refundResponse.getBody().getRefundReference());


    }


    @Test
    public void RemissionNotFoundException() throws Exception {

        Mockito.when(remissionRepository.findByRemissionReference(any())).thenReturn(Optional.empty());

        Exception exception = assertThrows(
            RemissionNotFoundException.class,
            () -> paymentRefundsService.createAndValidateRetrospectiveRemissionRequest(
                retrospectiveRemissionRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Remission not found for given remission reference"));
    }

    @Test
    public  void testUpdateRemissionAmount_updatesRemissionAmount(){
        BigDecimal amount = new BigDecimal("11.99");
        PaymentFee fee = PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build();
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .paymentReference("2018-15202505035")
            .fees(Arrays.asList(fee))
            .build();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .apportionAmount(amount)
            .paymentAmount(amount)
            .ccdCaseNumber("1234123412341234")
            .paymentLink(paymentFeeLink)
            .paymentId(1)
            .feeId(1)
            .id(1)
            .feeAmount(amount).build();
        Remission remission = Remission.remissionWith()
            .fee(PaymentFee.feeWith()
                .calculatedAmount(BigDecimal.valueOf(10))
                .build())
            .remissionReference("RM-1234-1234-1234-1234")
            .build();
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        Mockito.when(feePayApportionRepository.findByPaymentId(any())).thenReturn(Optional.of(Arrays.asList(feePayApportion)));

        Mockito.when(remissionRepository.findByFeeId(anyInt())).thenReturn(Optional.of(remission));
        ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
            .resubmitRefundRemissionRequestWith()
            .amount(BigDecimal.valueOf(5))
            .refundReason("RR036")
            .feeId("100")
            .totalRefundedAmount(BigDecimal.valueOf(15))
            .build();
        ResponseEntity responseEntity = paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",resubmitRefundRemissionRequest);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
        verify(remissionRepository).save(any(Remission.class));
    }

    /*
    To be updated in PAY-5368
    @Test
    public void testUpdateRemissionWhenPaymentReferenceIsNotFound(){
        Mockito.when(paymentRepository.findByReference(any())).thenThrow(new PaymentNotFoundException());
        ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
            .resubmitRefundRemissionRequestWith()
            .amount(BigDecimal.valueOf(70))
            .refundReason("RR003")
            .feeId("100")
            .build();
        Exception exception = assertThrows(
                PaymentNotFoundException.class,
                () -> paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",resubmitRefundRemissionRequest)
        );
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Refund can not be processed for unsuccessful payment"));
    }*/

    /*@Test
    public void testUpdateRemissionWhenRequestAmountIsGreaterThanPaymentAmount(){
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
            .resubmitRefundRemissionRequestWith()
            .amount(BigDecimal.valueOf(170))
            .refundReason("RR003")
            .feeId("100")
            .build();
        Exception exception = assertThrows(
                InvalidRefundRequestException.class,
                () -> paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",resubmitRefundRemissionRequest)
        );
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("This payment is not yet eligible for refund"));
    }*/

    @Test
    public  void testUpdateRemissionAmountForRefundsOtherThanRetrospectiveRemission(){
        ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
            .resubmitRefundRemissionRequestWith()
            .amount(BigDecimal.valueOf(70))
            .refundReason("RR003")
            .feeId("100")
            .totalRefundedAmount(BigDecimal.valueOf(80))
            .build();
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        ResponseEntity responseEntity = paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",resubmitRefundRemissionRequest);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
    }

//    @Test(expected = PaymentNotFoundException.class)
//    public void testUpdateRemissionAmountWithNullFeePayApportion(){
//
//        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
//        Mockito.when(feePayApportionRepository.findByPaymentId(any())).thenReturn(Optional.ofNullable(null));
//
//        ResponseEntity responseEntity = paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",BigDecimal.valueOf(10),"RR036");
//    }

    @Test
    public void testUpdateRemissionAmountWhenRemissionIsNotPresent(){
        BigDecimal amount = new BigDecimal("11.99");
        PaymentFee fee = PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build();
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .paymentReference("2018-15202505035")
            .fees(Arrays.asList(fee))
            .build();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .apportionAmount(amount)
            .paymentAmount(amount)
            .ccdCaseNumber("1234123412341234")
            .paymentLink(paymentFeeLink)
            .paymentId(1)
            .feeId(1)
            .id(1)
            .feeAmount(amount).build();
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        Mockito.when(feePayApportionRepository.findByPaymentId(any())).thenReturn(Optional.of(Arrays.asList(feePayApportion)));

        Mockito.when(remissionRepository.findByFeeId(anyInt())).thenReturn(Optional.ofNullable(null));
        ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
            .resubmitRefundRemissionRequestWith()
            .amount(BigDecimal.valueOf(70))
            .refundReason("RR036")
            .feeId("100")
            .totalRefundedAmount(BigDecimal.valueOf(0))
            .build();
        ResponseEntity responseEntity = paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",resubmitRefundRemissionRequest);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
        verify(remissionRepository,Mockito.times(0)).save(any(Remission.class));
    }

    @Test
    public  void testUpdateRemissionAmountWhenNewRemissionAmountIsLesserThanFeeAmount(){
        BigDecimal amount = new BigDecimal("11.99");
        PaymentFee fee = PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build();
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .paymentReference("2018-15202505035")
            .fees(Arrays.asList(fee))
            .build();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .apportionAmount(amount)
            .paymentAmount(amount)
            .ccdCaseNumber("1234123412341234")
            .paymentLink(paymentFeeLink)
            .paymentId(1)
            .feeId(1)
            .id(1)
            .feeAmount(amount).build();
        Remission remission = Remission.remissionWith()
            .fee(PaymentFee.feeWith()
                .calculatedAmount(BigDecimal.valueOf(75))
                .build())
            .remissionReference("RM-1234-1234-1234-1234")
            .build();
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        Mockito.when(feePayApportionRepository.findByPaymentId(any())).thenReturn(Optional.of(Arrays.asList(feePayApportion)));
        ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
            .resubmitRefundRemissionRequestWith()
            .amount(BigDecimal.valueOf(70))
            .refundReason("RR036")
            .feeId("100")
            .totalRefundedAmount(BigDecimal.valueOf(100))
            .build();

        Mockito.when(remissionRepository.findByFeeId(anyInt())).thenReturn(Optional.of(remission));
        ResponseEntity responseEntity = paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",resubmitRefundRemissionRequest);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
        verify(remissionRepository).save(any(Remission.class));
    }

    // @Test
    public void  partialRefundsValidationExceptionScenariosTest(){

        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));

        InternalRefundResponse mockRefundResponse = InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        ResponseEntity<InternalRefundResponse> responseEntity = new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        String expectedMessage;

        paymentRefundRequest.setTotalRefundAmount(BigDecimal.valueOf(0));

        expectedMessage = "You need to enter a refund amount";

        validateRefundException(expectedMessage);

        paymentRefundRequest.setTotalRefundAmount(BigDecimal.valueOf(550));

        paymentRefundRequest.getFees().get(0).setUpdatedVolume(0);

        expectedMessage = "You need to enter a valid number";

        validateRefundException(expectedMessage);

        paymentRefundRequest.getFees().get(0).setUpdatedVolume(1);

        paymentRefundRequest.setTotalRefundAmount(BigDecimal.valueOf(600));

        paymentRefundRequest.getFees().get(0).setRefundAmount(BigDecimal.valueOf(600));

        expectedMessage = "The amount you want to refund is more than the amount paid";

        validateRefundException(expectedMessage);

        paymentRefundRequest.setTotalRefundAmount(BigDecimal.valueOf(550));

        paymentRefundRequest.getFees().get(0).setRefundAmount(BigDecimal.valueOf(550));


        paymentRefundRequest.getFees().get(0).setUpdatedVolume(2);

        expectedMessage = "The quantity you want to refund is more than the available quantity";

        validateRefundException(expectedMessage);

        mockPaymentSuccess.getPaymentLink().getFees().get(0).setVolume(2);

        paymentRefundRequest.setTotalRefundAmount(BigDecimal.valueOf(5000));

        paymentRefundRequest.getFees().get(0).setApportionAmount(BigDecimal.valueOf(5000));
        paymentRefundRequest.getFees().get(0).setRefundAmount(BigDecimal.valueOf(5000));


        expectedMessage = "The Amount to Refund should be equal to the product of Fee Amount and quantity";

        validateRefundException(expectedMessage);

        mockPaymentSuccess.getPaymentLink().getFees().get(0).setVolume(1);

        paymentRefundRequest.setTotalRefundAmount(BigDecimal.valueOf(550));

        paymentRefundRequest.getFees().get(0).setRefundAmount(BigDecimal.valueOf(550));


        paymentRefundRequest.getFees().get(0).setCalculatedAmount(BigDecimal.valueOf(550));

        paymentRefundRequest.getFees().get(0).setApportionAmount(BigDecimal.valueOf(550));

    }

    private void validateRefundException(String expectedMessage){

        String actualMessage;

        Exception exception;

        exception = assertThrows(InvalidPartialRefundRequestException.class, () -> {
            paymentRefundsService.createRefund(paymentRefundRequest,header);
        });

        actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    // @Test
    public void givenNullContactDetails_whenCreateRefund_thenInvalidRefundRequestExceptionIsReceived() throws Exception {
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        InternalRefundResponse mockRefundResponse =
            InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        ResponseEntity<InternalRefundResponse> responseEntity =
            new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
            .paymentReference("RC-1234-1234-1234-1234")
            .refundReason("RESN1")
            .contactDetails(null)
            .build();

        Exception exception = assertThrows(
            InvalidRefundRequestException.class,
            () -> paymentRefundsService.createRefund(paymentRefundRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Contact Details should not be null or empty"));
    }

    @Test
    public void givenEmptyNotificationType_whenCreateRefund_thenInvalidRefundRequestExceptionIsReceived() throws Exception {
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        InternalRefundResponse mockRefundResponse =
            InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        ResponseEntity<InternalRefundResponse> responseEntity =
            new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
            .paymentReference("RC-1234-1234-1234-1234")
            .refundReason("RESN1")
            .contactDetails(ContactDetails.contactDetailsWith().notificationType("").build())
            .build();

      /*  Exception exception = assertThrows(
            InvalidRefundRequestException.class,
            () -> paymentRefundsService.createRefund(paymentRefundRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Notification Type should not be null or empty"));*/
    }

    @Test
    public void givenInvalidNotificationType_whenCreateRefund_thenInvalidRefundRequestExceptionIsReceived() throws Exception {
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        InternalRefundResponse mockRefundResponse =
            InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        ResponseEntity<InternalRefundResponse> responseEntity =
            new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
            .paymentReference("RC-1234-1234-1234-1234")
            .refundReason("RESN1")
            .contactDetails(ContactDetails.contactDetailsWith().notificationType("POST").build())
            .build();

        /*Exception exception = assertThrows(
            InvalidRefundRequestException.class,
            () -> paymentRefundsService.createRefund(paymentRefundRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Notification Type should be EMAIL or LETTER"));*/
    }

    @Test
    public void givenEmptyEmailId_whenCreateRefund_thenInvalidRefundRequestExceptionIsReceived() throws Exception {
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        InternalRefundResponse mockRefundResponse =
            InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        ResponseEntity<InternalRefundResponse> responseEntity =
            new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
            .paymentReference("RC-1234-1234-1234-1234")
            .refundReason("RESN1")
            .contactDetails(ContactDetails.contactDetailsWith().notificationType("EMAIL").email("").build())
            .build();

       /* Exception exception = assertThrows(
            InvalidRefundRequestException.class,
            () -> paymentRefundsService.createRefund(paymentRefundRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Email id should not be null or empty"));*/
    }

    @Test
    public void givenInvalidEmailId_whenCreateRefund_thenInvalidRefundRequestExceptionIsReceived() throws Exception {
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        InternalRefundResponse mockRefundResponse =
            InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        ResponseEntity<InternalRefundResponse> responseEntity =
            new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
            .paymentReference("RC-1234-1234-1234-1234")
            .refundReason("RESN1")
            .contactDetails(ContactDetails.contactDetailsWith().notificationType("EMAIL").email("sfgsd").build())
            .build();

        /*Exception exception = assertThrows(
            InvalidRefundRequestException.class,
            () -> paymentRefundsService.createRefund(paymentRefundRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Email id is not valid"));*/
    }

    @Test
    public void givenEmptyPostalCode_whenCreateRefund_thenInvalidRefundRequestExceptionIsReceived() throws Exception {
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        InternalRefundResponse mockRefundResponse =
            InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        ResponseEntity<InternalRefundResponse> responseEntity =
            new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
            .paymentReference("RC-1234-1234-1234-1234")
            .refundReason("RESN1")
            .contactDetails(ContactDetails.contactDetailsWith().notificationType("LETTER").postalCode("").build())
            .build();

        /*Exception exception = assertThrows(
            InvalidRefundRequestException.class,
            () -> paymentRefundsService.createRefund(paymentRefundRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Postal code should not be null or empty"));*/
    }

    //@Test
    @WithMockUser(authorities = "payments-refund")
    public void checkRefundAgainstRemissionTest(){

        RemissionDto remissionDto = RemissionDto.remissionDtoWith()
            .ccdCaseNumber("1111222233334444")
            .feeId(50).build();

        List<RemissionDto> remissionDtoList = new ArrayList<>();
        remissionDtoList.add(remissionDto);

        PaymentDto paymentDto =  PaymentDto.payment2DtoWith()
            .paymentReference("RC-2222-3333-4444-5555")
            .amount(BigDecimal.valueOf(100)).build();

        List<PaymentDto> paymentDtoList = new ArrayList<>();
        paymentDtoList.add(paymentDto);

        FeeDto feeDto = FeeDto.feeDtoWith().build();

        List<FeeDto> feeDtoList = new ArrayList<>();
        feeDto.setCcdCaseNumber("1111222233334444");
        feeDtoList.add(feeDto);

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .remissions(remissionDtoList)
            .payments(paymentDtoList)
            .fees(feeDtoList).build();

        List<PaymentGroupDto> paymentGroupDtoList = new ArrayList<>();
        paymentGroupDtoList.add(paymentGroupDto);

        PaymentGroupResponse paymentGroupResponse = PaymentGroupResponse.paymentGroupResponseWith()
            .paymentGroups(paymentGroupDtoList).build();

        List<PaymentGroupResponse> paymentGroupResponseList = new ArrayList<>();
        paymentGroupResponseList.add(paymentGroupResponse);

        RefundDto refundDto = RefundDto.buildRefundListDtoWith()
            .refundReference("RF-1111-2222-3333-4444")
            .paymentReference("RC-2222-3333-4444-5555")
            .feeIds("50")
            .refundStatus(RefundStatus.buildRefundStatusWith().name("Accepted").build())
            .reason("Retrospective remission")
            .ccdCaseNumber("1111222233334444")
            .amount(BigDecimal.valueOf(50)).build();

        RefundDto refundDto2 = RefundDto.buildRefundListDtoWith()
            .refundReference("RF-1111-2222-3333-4444")
            .paymentReference("RC-2222-3333-4444-5555")
            .feeIds("50")
            .refundStatus(RefundStatus.buildRefundStatusWith().name("Accepted").build())
            .reason("Retrospective remission")
            .ccdCaseNumber("1111222233334444")
            .amount(BigDecimal.valueOf(1000)).build();

        List<RefundDto> refundDtoList = new ArrayList<>();
        refundDtoList.add(refundDto);
        refundDtoList.add(refundDto2);

        RefundListDtoResponse mockRefundListDtoResponse = RefundListDtoResponse.buildRefundListWith()
            .refundList(refundDtoList).build();

        ResponseEntity<RefundListDtoResponse> responseEntity =
            new ResponseEntity<>(mockRefundListDtoResponse, HttpStatus.OK);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(RefundListDtoResponse.class))).thenReturn(responseEntity);

        paymentRefundsService.checkRefundAgainstRemission(header, paymentGroupResponse,"1111222233334444");

        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
        assertEquals(paymentGroupResponse.getPaymentGroups().get(0).getPayments().get(0).isIssueRefund(), false);
        assertEquals(paymentGroupResponse.getPaymentGroups().get(0).getRemissions().get(0).isAddRefund(), false);

    }

    //@Test
    @WithMockUser(authorities = "payments-refund-approver")
    public void checkRefundAgainstRemissionTest1(){

        RemissionDto remissionDto = RemissionDto.remissionDtoWith()
            .ccdCaseNumber("1111222233334444")
            .feeId(50).build();

        List<RemissionDto> remissionDtoList = new ArrayList<>();
        remissionDtoList.add(remissionDto);

        PaymentDto paymentDto =  PaymentDto.payment2DtoWith()
            .paymentReference("RC-2222-3333-4444-5555")
            .amount(BigDecimal.valueOf(100)).build();

        List<PaymentDto> paymentDtoList = new ArrayList<>();
        paymentDtoList.add(paymentDto);

        FeeDto feeDto = FeeDto.feeDtoWith().build();
        feeDto.setCcdCaseNumber("1111222233334444");

        List<FeeDto> feeDtoList = new ArrayList<>();
        feeDtoList.add(feeDto);

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .remissions(remissionDtoList)
            .payments(paymentDtoList)
            .fees(feeDtoList).build();

        List<PaymentGroupDto> paymentGroupDtoList = new ArrayList<>();
        paymentGroupDtoList.add(paymentGroupDto);

        PaymentGroupResponse paymentGroupResponse = PaymentGroupResponse.paymentGroupResponseWith()
            .paymentGroups(paymentGroupDtoList).build();

        List<PaymentGroupResponse> paymentGroupResponseList = new ArrayList<>();
        paymentGroupResponseList.add(paymentGroupResponse);

        RefundDto refundDto = RefundDto.buildRefundListDtoWith()
            .refundReference("RF-1111-2222-3333-4444")
            .paymentReference("RC-2222-3333-4444-5555")
            .ccdCaseNumber("1111222233334444")
            .feeIds("50")
            .refundStatus(RefundStatus.buildRefundStatusWith().name("Accepted").build())
            .reason("Retrospective remission")
            .amount(BigDecimal.valueOf(50)).build();

        RefundDto refundDto2 = RefundDto.buildRefundListDtoWith()
            .refundReference("RF-1111-2222-3333-4444")
            .paymentReference("RC-2222-3333-4444-5555")
            .ccdCaseNumber("1111222233334444")
            .feeIds("50")
            .refundStatus(RefundStatus.buildRefundStatusWith().name("Accepted").build())
            .reason("Retrospective remission")
            .amount(BigDecimal.valueOf(1000)).build();

        List<RefundDto> refundDtoList = new ArrayList<>();
        refundDtoList.add(refundDto);
        refundDtoList.add(refundDto2);

        RefundListDtoResponse mockRefundListDtoResponse = RefundListDtoResponse.buildRefundListWith()
            .refundList(refundDtoList).build();

        ResponseEntity<RefundListDtoResponse> responseEntity =
            new ResponseEntity<>(mockRefundListDtoResponse, HttpStatus.OK);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(RefundListDtoResponse.class))).thenReturn(responseEntity);

        paymentRefundsService.checkRefundAgainstRemission(header, paymentGroupResponse,"1111222233334444");

        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());

    }



    //@Test
    @WithMockUser(authorities = "payments-refund")
    public void checkRefundAgainstRemissionFeePayApprotionTest(){

        RemissionDto remissionDto = RemissionDto.remissionDtoWith()
            .ccdCaseNumber("1111222233334444")
            .feeId(50).build();


        List<RemissionDto> remissionDtoList = new ArrayList<>();
        remissionDtoList.add(remissionDto);

        PaymentDto paymentDto =  PaymentDto.payment2DtoWith()
            .paymentReference("RC-2222-3333-4444-5555")
            .reference("RC-2222-3333-4444-5555")

            .amount(BigDecimal.valueOf(100)).build();

        List<PaymentDto> paymentDtoList = new ArrayList<>();
        paymentDtoList.add(paymentDto);

        FeeDto feeDto = FeeDto.feeDtoWith().build();

        List<FeeDto> feeDtoList = new ArrayList<>();
        feeDtoList.add(feeDto);

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .remissions(remissionDtoList)
            .payments(paymentDtoList)
            .fees(feeDtoList).build();



        RefundDto refundDto = RefundDto.buildRefundListDtoWith()
            .refundReference("RF-1111-2222-3333-4444")
            .paymentReference("RC-2222-3333-4444-5555")
            .feeIds("50")
            .refundStatus(RefundStatus.buildRefundStatusWith().name("Accepted").build())
            .reason("Retrospective remission")
            .amount(BigDecimal.valueOf(50)).build();

        RefundDto refundDto2 = RefundDto.buildRefundListDtoWith()
            .refundReference("RF-1111-2222-3333-4444")
            .paymentReference("RC-2222-3333-4444-5555")
            .feeIds("50")
            .refundStatus(RefundStatus.buildRefundStatusWith().name("Accepted").build())
            .reason("Retrospective remission")
            .amount(BigDecimal.valueOf(1000)).build();

        List<RefundDto> refundDtoList = new ArrayList<>();
        refundDtoList.add(refundDto);
        refundDtoList.add(refundDto2);

        RefundListDtoResponse mockRefundListDtoResponse = RefundListDtoResponse.buildRefundListWith()
            .refundList(refundDtoList).build();

        ResponseEntity<RefundListDtoResponse> responseEntity =
            new ResponseEntity<>(mockRefundListDtoResponse, HttpStatus.OK);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        Payment mockPaymentSuccess1 = Payment.paymentWith().reference("RC-2222-3333-4444-5555")
            .amount(BigDecimal.valueOf(40))
            .build();
        when(paymentRepository.findByReference(anyString())).thenReturn(Optional.of(mockPaymentSuccess1));

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(RefundListDtoResponse.class))).thenReturn(responseEntity);

        paymentRefundsService.checkRefundAgainstRemissionFeeApportion(header, paymentGroupDto,"1111222233334444");

        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());


    }


    @Test
    public void createUnSuccessfulRetroRemissionRefund() throws Exception {

        BigDecimal amount = new BigDecimal("11.99");
        PaymentFee fee = PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build();
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .paymentReference("2018-15202505035")
            .fees(Arrays.asList(fee))
            .build();

        Remission remission = Remission.remissionWith()
            .paymentFeeLink(paymentFeeLink)
            .remissionReference("qwerty")
            .fee(fee)
            .hwfAmount(amount)
            .hwfReference("poiuytrewq")
            .build();
        List<FeePayApportion>  feeAppList = new ArrayList();

        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .apportionAmount(amount)
            .paymentAmount(amount)
            .ccdCaseNumber("1234123412341234")
            .paymentLink(paymentFeeLink)
            .paymentId(1)
            .feeId(1)
            .id(1)
            .feeAmount(amount).build();
        feeAppList.add(feePayApportion);
        Payment payment = Payment.paymentWith()
            .id(1)
            .amount(amount)
            .caseReference("caseReference")
            .description("retrieve payment mock test")
            .serviceType("Civil Money Claims")
            .siteId("siteID")
            .currency("GBP")
            .organisationName("organisationName")
            .customerReference("customerReference")
            .pbaNumber("pbaNumer")
            .reference("RC-1520-2505-0381-8145")
            .ccdCaseNumber("1234123412341234")
            .paymentLink(paymentFeeLink)
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .build();
        Mockito.when(remissionRepository.findByRemissionReference(any())).thenReturn(Optional.ofNullable(remission));

        Mockito.when(feePayApportionRepository.findByFeeId(any())).thenReturn((Optional.empty()));

        Mockito.when(paymentRepository.findById(any())).thenReturn(Optional.ofNullable(payment));
        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE);
        InternalRefundResponse mockRefundResponse = InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        ResponseEntity<InternalRefundResponse> responseEntity = new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);


        Exception exception = assertThrows(
            PaymentNotSuccessException.class,
            () -> paymentRefundsService.createAndValidateRetrospectiveRemissionRequest(
                retrospectiveRemissionRequest, header)
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Refund can be possible if payment is successful"));

    }



}
