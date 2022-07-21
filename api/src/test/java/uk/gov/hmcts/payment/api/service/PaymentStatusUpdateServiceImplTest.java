package uk.gov.hmcts.payment.api.service;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.*;

import java.math.BigDecimal;

import java.util.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import static org.mockito.Mockito.when;

import uk.gov.hmcts.payment.api.dto.mapper.PaymentFailureReportMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.scheduler.Clock;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class PaymentStatusUpdateServiceImplTest {


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        clock = new Clock();
    }


    @Mock
    private PaymentFailureRepository paymentFailureRepository;

    @Mock
    private PaymentStatusDtoMapper paymentStatusDtoMapper;

    @InjectMocks
    private PaymentStatusUpdateServiceImpl paymentStatusUpdateServiceImpl;

   @Mock
   @Qualifier("restTemplateRefundCancel")
   private RestTemplate restTemplateRefundCancel;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
   private  PaymentFailures paymentFailures;

    @Mock
    private Payment2Repository paymentRepository;

    private Clock clock;

   @Spy
    private PaymentFailureReportMapper paymentFailureReportMapper;

    @Mock
    @Qualifier("restTemplateGetRefund")
    private RestTemplate restTemplateGetRefund;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("MM/dd/yyyy");

    @Test
     public void testPaymentFailureBounceChequeDBInsert(){
        Payment payment = getPayment();
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.save(any())).thenReturn(paymentFailures);
        PaymentFailures paymentFailures = paymentStatusUpdateServiceImpl.insertBounceChequePaymentFailure(paymentStatusBouncedChequeDto);
        assertNotNull(paymentFailures);

    }

    @Test
    public void testSuccessCancelFailureRefundForBounceCheque(){

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("service auth token");
        when(this.restTemplateRefundCancel.exchange(anyString(),
            eq(HttpMethod.PATCH),
            any(HttpEntity.class),
            eq(String.class), any(Map.class)))
            .thenReturn(new ResponseEntity(HttpStatus.OK));
        boolean cancelRefund = paymentStatusUpdateServiceImpl.cancelFailurePaymentRefund(paymentStatusBouncedChequeDto.getPaymentReference());

        assertTrue(cancelRefund);
    }


    @Test
    public void returnRefundServiceUnavailableExceptionWhenRefundReturnHttpServerErrorExceptionForBounceCheque(){

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto = getPaymentStatusBouncedChequeDto();
        String PaymentReference = paymentStatusBouncedChequeDto.getPaymentReference();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("service auth token");
        when(this.restTemplateRefundCancel.exchange(anyString(),
            eq(HttpMethod.PATCH),
            any(HttpEntity.class),
            eq(String.class), any(Map.class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.NOT_FOUND));
        boolean refund = paymentStatusUpdateServiceImpl.cancelFailurePaymentRefund(PaymentReference);
        assertTrue(refund);

    }

    @Test
    public void testPaymentFailureChargebackDBInsert(){
        Payment payment = getPayment();
        PaymentStatusChargebackDto paymentStatusChargebackDto =getPaymentStatusChargebackDto();
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        when(paymentStatusDtoMapper.ChargebackRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.save(any())).thenReturn(paymentFailures);
        PaymentFailures paymentFailures = paymentStatusUpdateServiceImpl.insertChargebackPaymentFailure(paymentStatusChargebackDto);
        assertNotNull(paymentFailures);

    }

    @Test
    public void testSuccessCancelFailureRefundForChargeback(){

        PaymentStatusChargebackDto paymentStatusChargebackDto =getPaymentStatusChargebackDto();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("service auth token");
        when(this.restTemplateRefundCancel.exchange(anyString(),
            eq(HttpMethod.PATCH),
            any(HttpEntity.class),
            eq(String.class), any(Map.class)))
            .thenReturn(new ResponseEntity(HttpStatus.OK));
        boolean cancelRefund = paymentStatusUpdateServiceImpl.cancelFailurePaymentRefund(paymentStatusChargebackDto.getPaymentReference());

        assertTrue(cancelRefund);
    }

    @Test
    public void returnListOfPaymentFailureWhenPaymentReferencePassed(){

        when(paymentFailureRepository.findByPaymentReferenceOrderByFailureEventDateTimeDesc(any())).thenReturn(Optional.of(getPaymentFailuresList()));
        List<PaymentFailures> paymentFailures  =  paymentStatusUpdateServiceImpl.searchPaymentFailure("RC-1637-5072-9888-4233");

        assertEquals("RC-1520-2505-0381-8145",paymentFailures.get(0).getPaymentReference());
    }

    @Test
    public void return404WhenPaymentFailureNotFound(){

        when(paymentFailureRepository.findByPaymentReferenceOrderByFailureEventDateTimeDesc(any())).thenReturn(Optional.empty());

        assertThrows(
            PaymentNotFoundException.class,
            () -> paymentStatusUpdateServiceImpl.searchPaymentFailure("RC-1637-5072-9888-4233")
        );
    }

    @Test
    public void testDeleteByPaymentReferenceWithException() {
        long value = 0;
        when(paymentFailureRepository.deleteByFailureReference(anyString())).thenReturn(value);
        assertThrows(
            PaymentNotFoundException.class,
            () -> paymentStatusUpdateServiceImpl.deleteByFailureReference("")
        );
    }

    @Test
    public void givenValidInputWhenUpdatePaymentFailureThenValidOutput() {
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentDate("2021-10-10T10:10:10")
                .representmentStatus("Yes")
                .build();

        PaymentFailures paymentFailure = PaymentFailures.paymentFailuresWith()
                .failureType("Bounced Cheque")
                .paymentReference("RC1234")
                .amount(BigDecimal.valueOf(555))
                .ccdCaseNumber("123456789")
                .failureReference("FR12345")
                .id(1)
                .reason("RR001")
                .representmentOutcomeDate(DateTime.parse(paymentStatusUpdateSecond.getRepresentmentDate()).withZone(
                        DateTimeZone.UTC)
                        .toDate())
                .representmentSuccess("Yes")
                .build();
        when(paymentFailureRepository.findByFailureReference(anyString())).thenReturn(Optional.of(paymentFailure));
        when(paymentFailureRepository.save(any())).thenReturn(paymentFailure);
        PaymentFailures result = paymentStatusUpdateServiceImpl.updatePaymentFailure("dummy", paymentStatusUpdateSecond);
        assertEquals(result.getRepresentmentSuccess(), paymentFailure.getRepresentmentSuccess());
        assertEquals(result.getRepresentmentOutcomeDate(), paymentFailure.getRepresentmentOutcomeDate());
    }

    @Test
    public void testSuccessPaymentFailureReport(){
         when(paymentFailureRepository.findByDatesBetween(any(),any())).thenReturn(getPaymentFailuresList());
        when(paymentRepository.findByReferenceIn(any())).thenReturn(getPaymentList());
        List<RefundDto> refundDtoeDtos = new ArrayList<>();
        refundDtoeDtos.add(getRefund());
        ResponseEntity<List<RefundDto>> responseEntity = new ResponseEntity<>(refundDtoeDtos, HttpStatus.OK);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplateGetRefund.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<List<RefundDto>>() {
            }))).thenReturn(responseEntity);

        Date fromDate =clock.getYesterdayDate();
        Date toDate = clock.getTodayDate();
        List<PaymentFailureReportDto> paymentFailureReportDto = paymentStatusUpdateServiceImpl.paymentFailureReport(fromDate,toDate,headers);

        Assert.assertEquals("RC-1520-2505-0381-8145",paymentFailureReportDto.get(0).getPaymentReference());
        Assert.assertEquals("RF-123=345=897",paymentFailureReportDto.get(0).getRefundReference());

    }

    @Test
    public void returnValidationErrorWhenEndDateIsBeforeStartDate(){

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        Date toDate =clock.getYesterdayDate();
        Date fromDate = clock.getTodayDate();
        assertThrows(
            ValidationErrorException.class,
            () -> paymentStatusUpdateServiceImpl.paymentFailureReport(fromDate,toDate,headers)
        );
    }

    @Test
    public void return404WhenPaymentNotFound(){

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        Date fromDate =clock.getYesterdayDate();
        Date toDate = clock.getTodayDate();
        assertThrows(
            PaymentNotFoundException.class,
            () -> paymentStatusUpdateServiceImpl.paymentFailureReport(fromDate,toDate,headers)
        );
    }

    @Test
    public void getRefundSuccess() throws Exception {

        List<RefundDto> refundDtoeDtos = new ArrayList<>();
        refundDtoeDtos.add(getRefund());
        ResponseEntity<List<RefundDto>> responseEntity = new ResponseEntity<>(refundDtoeDtos, HttpStatus.OK);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("test-token");

        List<String> paymentReference = new ArrayList<>();
        paymentReference.add("RC-1520-2505-0381-8145");
        when(restTemplateGetRefund.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<List<RefundDto>>() {
            }))).thenReturn(responseEntity);

       List<RefundDto> res =  paymentStatusUpdateServiceImpl.fetchRefundResponse(paymentReference, headers);
        Assert.assertEquals("RC-1520-2505-0381-8145",res.get(0).getPaymentReference());
    }

    @Test(expected = InvalidRefundRequestException.class)
    public void returnExcetionWhenRefundCalled() throws Exception {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("test-token");

        List<String> paymentReference = new ArrayList<>();
        paymentReference.add("RC-1520-2505-0381-8145");
        when(restTemplateGetRefund.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<List<RefundDto>>() {
            }))).thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error"));

        paymentStatusUpdateServiceImpl.fetchRefundResponse(paymentReference, headers);

    }

    private PaymentStatusBouncedChequeDto getPaymentStatusBouncedChequeDto() {

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto = PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(555))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .build();

        return paymentStatusBouncedChequeDto;
    }

    private PaymentStatusChargebackDto getPaymentStatusChargebackDto() {

        PaymentStatusChargebackDto paymentStatusChargebackDto = PaymentStatusChargebackDto.paymentStatusChargebackRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(555))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .hasAmountDebited("yes")
            .build();

        return paymentStatusChargebackDto;
    }

    private List<PaymentFailures> getPaymentFailuresList(){

        List<PaymentFailures> paymentFailuresList = new ArrayList<>();
        PaymentFailures paymentFailures = PaymentFailures.paymentFailuresWith()
            .id(1)
            .reason("test")
            .failureReference("Bounce Cheque")
            .ccdCaseNumber("123")
            .paymentReference("RC-1520-2505-0381-8145")
            .ccdCaseNumber("1234")
            .amount(BigDecimal.valueOf(555))
            .representmentSuccess("yes")
            .failureType("Chargeback")
            .additionalReference("AR12345")
            .build();

        paymentFailuresList.add(paymentFailures);
        return paymentFailuresList;

    }

    private Payment getPayment() {

        Payment payment = Payment.paymentWith()
            .id(1)
            .amount(BigDecimal.valueOf(555))
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
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentLink(PaymentFeeLink.paymentFeeLinkWith()
                .id(1)
                .paymentReference("2018-15202505035")
                .fees(Arrays.asList(PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build()))
                .payments(Arrays.asList(Payment.paymentWith().internalReference("abc")
                    .id(1)
                    .reference("RC-1632-3254-9172-5888")
                    .caseReference("123789")
                    .ccdCaseNumber("1234")
                    .amount(new BigDecimal(300))
                    .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
                    .build()))
                .callBackUrl("http//:test")
                .build())
            .build();

        return payment;
    }

    private List<Payment> getPaymentList() {

        List<Payment> paymentList =new ArrayList<>();
        Payment payment = Payment.paymentWith()
            .id(1)
            .amount(BigDecimal.valueOf(555))
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
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentLink(PaymentFeeLink.paymentFeeLinkWith()
                .id(1)
                .paymentReference("2018-15202505035")
                .fees(Arrays.asList(PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build()))
                .payments(Arrays.asList(Payment.paymentWith().internalReference("abc")
                    .id(1)
                    .reference("RC-1632-3254-9172-5888")
                    .caseReference("123789")
                    .ccdCaseNumber("1234")
                    .amount(new BigDecimal(300))
                    .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
                    .build()))
                .callBackUrl("http//:test")
                .build())
            .build();

        paymentList.add(payment);

        return paymentList;
    }

    private List<PaymentFailureReportDto> getPaymentFailureReport(){

        return Arrays.asList(PaymentFailureReportDto.paymentFailureReportDtoWith()
                .serviceName("abc")
                .representmentStatus("no")
                .refundReference("RF-123-345-567")
                .failureReason("test")
                .eventName("Chargeback")
                .paymentReference("RC-1520-2505-0381-8145")
                .failureReference("Bounce Cheque")
                .disputedAmount(BigDecimal.valueOf(555))
            .build());
    }

    private RefundDto getRefund(){
        DateTime currentDateTime = new DateTime();
        return RefundDto.buildRefundListDtoWith()
            .refundReference("RF-123=345=897")
            .amount(BigDecimal.valueOf(5))
            .paymentReference("RC-1520-2505-0381-8145")
            .dateUpdated(currentDateTime.toDate())
            .build();
    }

}
