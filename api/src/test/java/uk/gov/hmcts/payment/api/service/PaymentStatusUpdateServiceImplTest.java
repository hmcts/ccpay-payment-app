package uk.gov.hmcts.payment.api.service;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.exception.InvalidPaymentFailureRequestException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class PaymentStatusUpdateServiceImplTest {


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
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

        assertEquals("RC-1637-5072-9888-4233",paymentFailures.get(0).getPaymentReference());
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
    public void returnBadRequestWhenDisputeAmountIsMoreThanPaymentAmountForChargeBack() {
        Payment payment = getPayment();
        PaymentStatusChargebackDto paymentStatusChargebackDto =getPaymentStatusChargebackDtoForBadRequest();
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        assertThrows(
            InvalidPaymentFailureRequestException.class,
            () -> paymentStatusUpdateServiceImpl.insertChargebackPaymentFailure(paymentStatusChargebackDto)
        );
    }

    @Test
    public void returnBadRequestWhenDisputeAmountIsMoreThanPaymentAmountForBounceCheque() {
        Payment payment = getPayment();
        PaymentStatusBouncedChequeDto paymentStatusBounceChequeDto =getPaymentStatusBounceChequeDtoForBadRequest();
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        assertThrows(
            InvalidPaymentFailureRequestException.class,
            () -> paymentStatusUpdateServiceImpl.insertBounceChequePaymentFailure(paymentStatusBounceChequeDto)
        );
    }

    @Test
    public void givenValidInputWhenUpdatePaymentFailureThenValidOutput() {
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentDate("2021-10-10T10:10:10")
                .representmentStatus(RepresentmentStatus.Yes)
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
    public void testUnprocessedPayment() {
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(888))
                .failureReference("FR8888")
                .eventDateTime("2022-10-10T10:10:10")
                .reason("RR001")
                .dcn("88")
                .poBoxNumber("8")
                .build();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        PaymentMetadataDto metadataDto =
                PaymentMetadataDto.paymentMetadataDtoWith().dcnReference("88").amount(new BigDecimal("777")).build();
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
                .ccdReference("9881231111111111")
                .allPaymentsStatus(uk.gov.hmcts.payment.api.dto.PaymentStatus.COMPLETE)
                .payments(Arrays.asList(metadataDto))
                .build();
        ResponseEntity responseEntity = new ResponseEntity(searchResponse, HttpStatus.OK);
        when(this.restTemplatePaymentGroup.exchange(anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(ResponseEntity.class), any(Map.class)))
                .thenReturn(responseEntity);
        PaymentFailures failure = PaymentFailures.paymentFailuresWith().dcn("88").build();
        when(paymentFailureRepository.save(any())).thenReturn(failure);

        PaymentFailures paymentFailure = paymentStatusUpdateServiceImpl.unprocessedPayment(unprocessedPayment, headers);

        assertEquals("88", paymentFailure.getDcn());
    }

    @Ignore
    @Test
    public void testInvalidUnprocessedPayment() {
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(888))
                .failureReference("FR8888")
                .eventDateTime("2022-10-10T10:10:10")
                .reason("RR001")
                .dcn("88")
                .poBoxNumber("8")
                .build();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        PaymentMetadataDto metadataDto =
                PaymentMetadataDto.paymentMetadataDtoWith().dcnReference("88").amount(new BigDecimal("889")).build();
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
                .ccdReference("9881231111111111")
                .allPaymentsStatus(uk.gov.hmcts.payment.api.dto.PaymentStatus.COMPLETE)
                .payments(Arrays.asList(metadataDto))
                .build();
        ResponseEntity responseEntity = new ResponseEntity(searchResponse, HttpStatus.OK);
        when(this.restTemplatePaymentGroup.exchange(anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(ResponseEntity.class), any(Map.class)))
                .thenReturn(responseEntity);

        Exception exception = assertThrows(
                InvalidPaymentFailureRequestException.class,
                () -> paymentStatusUpdateServiceImpl.unprocessedPayment(unprocessedPayment, headers)
        );
        String actualMessage = exception.getMessage();
        assertEquals("Failure amount cannot be more than payment amount", actualMessage);
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
            .paymentReference("RC-1637-5072-9888-4233")
            .ccdCaseNumber("123456")
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

    private PaymentStatusChargebackDto getPaymentStatusChargebackDtoForBadRequest() {

        PaymentStatusChargebackDto paymentStatusChargebackDto = PaymentStatusChargebackDto.paymentStatusChargebackRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(556))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .hasAmountDebited("yes")
            .build();

        return paymentStatusChargebackDto;
    }

    private PaymentStatusBouncedChequeDto getPaymentStatusBounceChequeDtoForBadRequest() {

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto = PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(556))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .build();

        return paymentStatusBouncedChequeDto;
    }


}
