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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.payment.api.dto.mapper.PaymentFailureReportMapper;
import uk.gov.hmcts.payment.api.dto.PaymentStatusUpdateSecond;
import uk.gov.hmcts.payment.api.dto.RepresentmentStatus;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.dto.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.exception.InvalidPaymentFailureRequestException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.scheduler.Clock;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import javax.persistence.Tuple;
import java.util.Optional;



@RunWith(MockitoJUnitRunner.class)
public class PaymentStatusUpdateServiceImplTest {

    private Date startDate;
    private Date endDate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        clock = new Clock();
        startDate = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
        endDate = new Date();
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
    @Qualifier("restTemplatePaymentGroup")
    private RestTemplate restTemplatePaymentGroup;

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

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
    @Test
     public void testPaymentFailureBounceChequeDBInsert() throws ParseException {
        Payment payment = getPayment();
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any(), any())).thenReturn(paymentFailures);
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
    public void testPaymentFailureChargebackDBInsert() throws ParseException {
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
    public void returnBadRequestWhenDisputeAmountIsMoreThanPaymentAmountForChargeBack() throws ParseException {
        Payment payment = getPayment();
        PaymentStatusChargebackDto paymentStatusChargebackDto =getPaymentStatusChargebackDtoForBadRequest();
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        assertThrows(
            InvalidPaymentFailureRequestException.class,
            () -> paymentStatusUpdateServiceImpl.insertChargebackPaymentFailure(paymentStatusChargebackDto)
        );
    }

    @Test
    public void returnBadRequestWhenDisputeAmountIsMoreThanPaymentAmountForBounceCheque() throws ParseException {
        Payment payment = getPayment();
        PaymentStatusBouncedChequeDto paymentStatusBounceChequeDto =getPaymentStatusBounceChequeDtoForBadRequest();
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        assertThrows(
            InvalidPaymentFailureRequestException.class,
            () -> paymentStatusUpdateServiceImpl.insertBounceChequePaymentFailure(paymentStatusBounceChequeDto)
        );
    }

    @Test
    public void givenValidInputWhenUpdatePaymentFailureThenValidOutput() throws ParseException {
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentDate("2021-10-10T10:10:10")
                .representmentStatus(RepresentmentStatus.Yes)
                .build();


        String dateInString = "2021-10-09T10:10:10";
        Date date = formatter.parse(dateInString);
        PaymentFailures paymentFailure = PaymentFailures.paymentFailuresWith()
                .failureType("Bounced Cheque")
                .paymentReference("RC1234")
                .amount(BigDecimal.valueOf(555))
                .ccdCaseNumber("123456789")
                .failureReference("FR12345")
                .id(1)
                .reason("RR001")
                .failureEventDateTime(date)
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
        ResponseEntity<RefundPaymentFailureReportDtoResponse> responseEntity = new ResponseEntity<>(getFailureRefund(), HttpStatus.OK);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplateGetRefund.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<RefundPaymentFailureReportDtoResponse>() {
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
    public void getRefundSuccess() {

        ResponseEntity<RefundPaymentFailureReportDtoResponse> responseEntity = new ResponseEntity<>(getFailureRefund(), HttpStatus.OK);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("test-token");

        List<String> paymentReference = new ArrayList<>();
        paymentReference.add("RC-1520-2505-0381-8145");
        when(restTemplateGetRefund.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<RefundPaymentFailureReportDtoResponse>() {
            }))).thenReturn(responseEntity);

        RefundPaymentFailureReportDtoResponse res =  paymentStatusUpdateServiceImpl.fetchRefundResponse(paymentReference);
        Assert.assertEquals("RC-1520-2505-0381-8145",res.getPaymentFailureDto().get(0).getPaymentReference());
    }

    @Test(expected = InvalidRefundRequestException.class)
    public void returnExcetionWhenRefundCalled() {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("test-token");

        List<String> paymentReference = new ArrayList<>();
        paymentReference.add("RC-1520-2505-0381-8145");
        when(restTemplateGetRefund.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<RefundPaymentFailureReportDtoResponse>() {
            }))).thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error"));

        paymentStatusUpdateServiceImpl.fetchRefundResponse(paymentReference);

    }

    @Test
    public void testSuccessPaymentFailureReportWhereRefundNotMatched(){
        when(paymentFailureRepository.findByDatesBetween(any(),any())).thenReturn(getPaymentFailuresList());
        when(paymentRepository.findByReferenceIn(any())).thenReturn(getPaymentList());
        ResponseEntity<RefundPaymentFailureReportDtoResponse> responseEntity = new ResponseEntity<>(getFailureRefundForNotMatched(), HttpStatus.OK);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplateGetRefund.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<RefundPaymentFailureReportDtoResponse>() {
            }))).thenReturn(responseEntity);

        Date fromDate =clock.getYesterdayDate();
        Date toDate = clock.getTodayDate();
        List<PaymentFailureReportDto> paymentFailureReportDto = paymentStatusUpdateServiceImpl.paymentFailureReport(fromDate,toDate,headers);

        Assert.assertEquals("RC-1520-2505-0381-8145",paymentFailureReportDto.get(0).getPaymentReference());
       // Assert.assertEquals("RF-123=345=897",paymentFailureReportDto.get(0).getRefundReference());

    }

    @Test
    public void testUnprocessedPayment() throws ParseException {
        String dateInString = "2021-10-09T10:10:10";
        Date date = formatter.parse(dateInString);
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(777))
                .failureReference("FR8888")
                .eventDateTime("2022-10-10T10:10:10")
                .reason("RR001")
                .dcn("88")
                .poBoxNumber("8")
                .build();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        PaymentMetadataDto metadataDto =
                PaymentMetadataDto.paymentMetadataDtoWith().dcnReference("88").amount(new BigDecimal("888")).dateBanked(date).build();
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
                .ccdReference("9881231111111111")
                .allPaymentsStatus(uk.gov.hmcts.payment.api.dto.PaymentStatus.COMPLETE)
                .payments(Arrays.asList(metadataDto))
                .build();
        ResponseEntity responseEntity = new ResponseEntity(searchResponse, HttpStatus.OK);
        when(this.restTemplatePaymentGroup.exchange(any(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(SearchResponse.class),any(HashMap.class)))
                .thenReturn(responseEntity);
        PaymentFailures failure = PaymentFailures.paymentFailuresWith().dcn("88").build();
        when(paymentFailureRepository.save(any())).thenReturn(failure);

        PaymentFailures paymentFailure = paymentStatusUpdateServiceImpl.unprocessedPayment(unprocessedPayment, headers);

        assertEquals("88", paymentFailure.getDcn());
    }

    @Test
    public void testInvalidUnprocessedPayment() {
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(889))
                .failureReference("FR8888")
                .eventDateTime("2022-10-10T10:10:10")
                .reason("RR001")
                .dcn("88")
                .poBoxNumber("8")
                .build();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        PaymentMetadataDto metadataDto =
                PaymentMetadataDto.paymentMetadataDtoWith().dcnReference("88").amount(new BigDecimal("888")).build();
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
                .ccdReference("9881231111111111")
                .allPaymentsStatus(uk.gov.hmcts.payment.api.dto.PaymentStatus.COMPLETE)
                .payments(Arrays.asList(metadataDto))
                .build();
        ResponseEntity responseEntity = new ResponseEntity(searchResponse, HttpStatus.OK);
        when(this.restTemplatePaymentGroup.exchange(any(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(SearchResponse.class), any(HashMap.class)))
                .thenReturn(responseEntity);

        Exception exception = assertThrows(
                InvalidPaymentFailureRequestException.class,
                () -> paymentStatusUpdateServiceImpl.unprocessedPayment(unprocessedPayment, headers)
        );
        String actualMessage = exception.getMessage();
        assertEquals("Failure amount cannot be more than payment amount", actualMessage);
    }

    @Test
    public void testPaymentFailureBounceChequeDBInsertIncorrectPaymentMethod() throws ParseException {
        Payment payment = getPaymentPBAMethod();
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        Exception exception = assertThrows(
            InvalidPaymentFailureRequestException.class,
            () -> paymentStatusUpdateServiceImpl.insertBounceChequePaymentFailure(paymentStatusBouncedChequeDto)
        );
        String actualMessage = exception.getMessage();
        assertEquals("Incorrect payment method", actualMessage);

    }

    @Test
    public void testPaymentFailureBounceChequeDBInsertLessPaymentFailureAmount() throws ParseException {
        Payment payment = getPayment();
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto = getPaymentStatusBouncedChequeDtoForLessAmount();
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        Exception exception = assertThrows(
            InvalidPaymentFailureRequestException.class,
            () -> paymentStatusUpdateServiceImpl.insertBounceChequePaymentFailure(paymentStatusBouncedChequeDto)
        );
        String actualMessage = exception.getMessage();
        assertEquals("Dispute amount can not be less than payment amount", actualMessage);

    }

    @Test
    public void testPaymentFailureBounceChequeDBInsertEventDatePast() throws ParseException {
        Payment payment = getPayment();
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto = getPaymentStatusBouncedChequeDtoForEventDatePast();
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        Exception exception = assertThrows(
            InvalidPaymentFailureRequestException.class,
            () -> paymentStatusUpdateServiceImpl.insertBounceChequePaymentFailure(paymentStatusBouncedChequeDto)
        );
        String actualMessage = exception.getMessage();
        assertEquals("Failure event date can not be prior to banked date", actualMessage);

    }

    @Test
    public void givenInvalidInputWhenUpdatePaymentFailureThenThrowsException() throws ParseException {
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentDate("2021-10-08T10:10:10")
            .representmentStatus(RepresentmentStatus.Yes)
            .build();

        String dateInString = "2021-10-09T10:10:10";
        Date date = formatter.parse(dateInString);
        PaymentFailures paymentFailure = PaymentFailures.paymentFailuresWith()
            .failureType("Bounced Cheque")
            .paymentReference("RC1234")
            .amount(BigDecimal.valueOf(555))
            .ccdCaseNumber("123456789")
            .failureReference("FR12345")
            .id(1)
            .reason("RR001")
            .failureEventDateTime(date)
            .representmentOutcomeDate(DateTime.parse(paymentStatusUpdateSecond.getRepresentmentDate()).withZone(
                    DateTimeZone.UTC)
                .toDate())
            .representmentSuccess("Yes")
            .build();
        when(paymentFailureRepository.findByFailureReference(anyString())).thenReturn(Optional.of(paymentFailure));

        Exception exception = assertThrows(
            InvalidPaymentFailureRequestException.class,
            () -> paymentStatusUpdateServiceImpl.updatePaymentFailure("dummy", paymentStatusUpdateSecond)
        );
        String actualMessage = exception.getMessage();
        assertEquals("Representment date can not be prior to failure event date", actualMessage);
    }


    @Test
    public void testTelephonyPaymentsReport_Success() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");

        List<Tuple> telephonyPaymentsList = Arrays.asList(
            new CustomTupleForTelephonyPaymentsReport("Service1", "CCD123", "PAY123", "FEE001", new Date(), new BigDecimal("100.00"), "Success"),
            new CustomTupleForTelephonyPaymentsReport("Service2", "CCD456", "PAY456", "FEE002", new Date(), new BigDecimal("200.00"), "Failed")
        );

        when(paymentRepository.findAllByDateCreatedBetweenAndPaymentChannel(startDate, endDate, PaymentChannel.TELEPHONY))
           .thenReturn(telephonyPaymentsList);

        List<TelephonyPaymentsReportDto> actualReport = paymentStatusUpdateServiceImpl.telephonyPaymentsReport(startDate, endDate, headers);

        assertEquals(2, actualReport.size());
        assertEquals("Service1", actualReport.get(0).getServiceName());
        assertEquals("Service2", actualReport.get(1).getServiceName());
        verify(paymentRepository, times(1)).findAllByDateCreatedBetweenAndPaymentChannel(startDate, endDate, PaymentChannel.TELEPHONY);
    }

    @Test
    public void testTelephonyPaymentsReport_StartDateAfterEndDate() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        Date invalidStartDate = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24);

        ValidationErrorException exception = assertThrows(ValidationErrorException.class, () -> {
            paymentStatusUpdateServiceImpl.telephonyPaymentsReport(invalidStartDate, endDate, headers);
        });

        assertEquals("Error occurred in the report ", exception.getMessage());
    }


    private PaymentStatusBouncedChequeDto getPaymentStatusBouncedChequeDto() {

        return PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(555))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .build();
    }

    private PaymentStatusBouncedChequeDto getPaymentStatusBouncedChequeDtoForLessAmount() {

        return PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(500))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .build();
    }

    private PaymentStatusBouncedChequeDto getPaymentStatusBouncedChequeDtoForEventDatePast() {

        return PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(555))
            .failureReference("FR12345")
            .eventDateTime("2021-10-08T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .build();
    }

    private PaymentStatusChargebackDto getPaymentStatusChargebackDto() {

        return PaymentStatusChargebackDto.paymentStatusChargebackRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(555))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .hasAmountDebited("yes")
            .build();
    }

    private List<PaymentFailures> getPaymentFailuresList(){

        List<PaymentFailures> paymentFailuresList = new ArrayList<>();
        PaymentFailures paymentFailures = PaymentFailures.paymentFailuresWith()
            .id(1)
            .reason("test")
            .failureReference("Bounce Cheque")
            .paymentReference("RC-1520-2505-0381-8145")
            .ccdCaseNumber("123456")
            .amount(BigDecimal.valueOf(100))
            .representmentSuccess("yes")
            .failureType("Chargeback")
            .additionalReference("AR12345")
            .build();

        paymentFailuresList.add(paymentFailures);
        return paymentFailuresList;

    }

    private Payment getPayment() throws ParseException {
        String dateInString = "2021-10-09T10:10:10";
        Date date = formatter.parse(dateInString);
        return Payment.paymentWith()
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
            .dateUpdated(date)
            .bankedDate(date)
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("cheque").build())
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
    }

    private Payment getPaymentPBAMethod() throws ParseException {
        String dateInString = "2021-10-09T10:10:10";
        Date date = formatter.parse(dateInString);
        return Payment.paymentWith()
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
            .dateUpdated(date)
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
    }

    private PaymentStatusChargebackDto getPaymentStatusChargebackDtoForBadRequest() {

        return PaymentStatusChargebackDto.paymentStatusChargebackRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(600))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .hasAmountDebited("yes")
            .build();
    }

    private PaymentStatusBouncedChequeDto getPaymentStatusBounceChequeDtoForBadRequest() {

        return PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(556))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .build();
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
            .refundDate(currentDateTime.toDate().toString())
            .build();
    }

    private RefundDto getRefundForNotMatched(){
        DateTime currentDateTime = new DateTime();
        return RefundDto.buildRefundListDtoWith()
            .refundReference("RF-123=345=897")
            .amount(BigDecimal.valueOf(5))
            .paymentReference("RC-1520-2505-0381-8146")
            .refundDate(currentDateTime.toDate().toString())
            .build();
    }

    private RefundPaymentFailureReportDtoResponse getFailureRefund(){
        return   RefundPaymentFailureReportDtoResponse.buildPaymentFailureListWith().paymentFailureDto(Arrays.asList(getRefund())).build();

    }

    private RefundPaymentFailureReportDtoResponse getFailureRefundForNotMatched(){
        return   RefundPaymentFailureReportDtoResponse.buildPaymentFailureListWith().paymentFailureDto(Arrays.asList(getRefundForNotMatched())).build();

    }

}
