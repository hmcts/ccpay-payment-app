package uk.gov.hmcts.payment.api.componenttests;



import java.time.temporal.ChronoUnit;
import java.util.Date;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.InternalRefundResponse;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.dto.ResubmitRefundRemissionRequest;
import uk.gov.hmcts.payment.api.dto.RetroSpectiveRemissionRequest;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.RemissionRepository;
import uk.gov.hmcts.payment.api.service.PaymentRefundsService;

import uk.gov.hmcts.payment.api.util.RefundEligibilityUtil;

import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
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
        .paymentReference("RC-1234-1234-1234-1234")
        .refundReason("RESN1")
        .build();
    Payment mockPaymentSuccess = Payment.paymentWith().reference("RC-1234-1234-1234-1234")
        .amount(BigDecimal.valueOf(100))
        .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
        .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
        .paymentLink(PaymentFeeLink.paymentFeeLinkWith().fees(Arrays.asList(PaymentFee.feeWith().id(1).build())).build())
        .build();
    RetroSpectiveRemissionRequest retroSpectiveRemissionRequest = RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith()
        .remissionReference("qwerty").build();

    Date refundDate= Date.from(
        new Date().toInstant().minus(1, ChronoUnit.DAYS));
    Payment payment = Payment.paymentWith()
        .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
        .dateUpdated(new Date())
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
    private RefundEligibilityUtil refundEligibilityUtil;


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

        Mockito.when(refundEligibilityUtil.getRefundEligiblityStatus(payment)).thenReturn(refundDate);

        InternalRefundResponse mockRefundResponse = InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();


        ResponseEntity<InternalRefundResponse> responseEntity = new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        ResponseEntity<RefundResponse> refundResponse = paymentRefundsService.createRefund(paymentRefundRequest, header);

        assertEquals("RF-4321-4321-4321-4321", refundResponse.getBody().getRefundReference());

    }


    @Test(expected = PaymentNotSuccessException.class)
    public void createRefundWithFailedReference() throws Exception {

        Payment mockPaymentFailed = Payment.paymentWith().reference("RC-1234-1234-1234-1234")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("Failed").build())
            .build();

        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentFailed));

        paymentRefundsService.createRefund(paymentRefundRequest, header);

    }


    @Test(expected = InvalidRefundRequestException.class)
    public void createRefundWithClientException() throws Exception {

        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));

        Mockito.when(refundEligibilityUtil.getRefundEligiblityStatus(payment)).thenReturn(refundDate);


        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        paymentRefundsService.createRefund(paymentRefundRequest, header);

    }


    @Test(expected = HttpServerErrorException.class)
    public void createRefundWithServerException() throws Exception {

        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));

        Mockito.when(refundEligibilityUtil.getRefundEligiblityStatus(payment)).thenReturn(refundDate);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        paymentRefundsService.createRefund(paymentRefundRequest, header);

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

        Mockito.when(refundEligibilityUtil.getRefundEligiblityStatus(payment)).thenReturn(refundDate);

        Mockito.when(feePayApportionRepository.findByFeeId(any())).thenReturn(Optional.ofNullable(feePayApportion));

        Mockito.when(paymentRepository.findById(any())).thenReturn(Optional.ofNullable(payment));

        InternalRefundResponse mockRefundResponse = InternalRefundResponse.InternalRefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        ResponseEntity<InternalRefundResponse> responseEntity = new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(InternalRefundResponse.class))).thenReturn(responseEntity);

        ResponseEntity<RefundResponse> refundResponse = paymentRefundsService.createAndValidateRetroSpectiveRemissionRequest(retroSpectiveRemissionRequest.getRemissionReference(), header);

        assertEquals("RF-4321-4321-4321-4321", refundResponse.getBody().getRefundReference());


    }


    @Test(expected = RemissionNotFoundException.class)
    public void RemissionNotFoundException() throws Exception {

        Mockito.when(remissionRepository.findByRemissionReference(any())).thenReturn(Optional.empty());

        ResponseEntity<RefundResponse> refundResponse = paymentRefundsService.createAndValidateRetroSpectiveRemissionRequest(retroSpectiveRemissionRequest.getRemissionReference(), header);

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
            .build();
        ResponseEntity responseEntity = paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",resubmitRefundRemissionRequest);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
        verify(remissionRepository).save(any(Remission.class));
    }

    @Test(expected = PaymentNotFoundException.class)
    public void testUpdateRemissionWhenPaymentReferenceIsNotFound(){
        Mockito.when(paymentRepository.findByReference(any())).thenThrow(new PaymentNotFoundException());
        ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
            .resubmitRefundRemissionRequestWith()
            .amount(BigDecimal.valueOf(70))
            .refundReason("RR003")
            .feeId("100")
            .build();
        ResponseEntity responseEntity = paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",resubmitRefundRemissionRequest);

    }

    @Test(expected = InvalidRefundRequestException.class)
    public void testUpdateRemissionWhenRequestAmountIsGreaterThanPaymentAmount(){
        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));
        ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
            .resubmitRefundRemissionRequestWith()
            .amount(BigDecimal.valueOf(170))
            .refundReason("RR003")
            .feeId("100")
            .build();
        ResponseEntity responseEntity = paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",resubmitRefundRemissionRequest);
    }

    @Test
    public  void testUpdateRemissionAmountForRefundsOtherThanRetrospectiveRemission(){
        ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
            .resubmitRefundRemissionRequestWith()
            .amount(BigDecimal.valueOf(70))
            .refundReason("RR003")
            .feeId("100")
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
            .build();

        Mockito.when(remissionRepository.findByFeeId(anyInt())).thenReturn(Optional.of(remission));
        ResponseEntity responseEntity = paymentRefundsService.updateTheRemissionAmount("RC-1234-1234-1234-1234",resubmitRefundRemissionRequest);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
        verify(remissionRepository).save(any(Remission.class));
    }
}
