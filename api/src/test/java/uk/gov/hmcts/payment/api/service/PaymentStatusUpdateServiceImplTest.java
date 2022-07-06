package uk.gov.hmcts.payment.api.service;

import org.junit.Before;
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
import uk.gov.hmcts.payment.api.dto.PaymentStatusChargebackDto;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import static org.mockito.Mockito.when;

import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.dto.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.model.PaymentFailureRepository;
import uk.gov.hmcts.payment.api.model.PaymentFailures;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import uk.gov.hmcts.payment.casepaymentorders.client.ServiceRequestCpoServiceClient;

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
    private AuthTokenGenerator authTokenGenerator;

    @Mock
   private  PaymentFailures paymentFailures;

    @Mock
    private ServiceRequestCpoServiceClient cpoServiceClient;

    public static final String S2S_TOKEN = "s2sToken";
    public static final String AUTH_TOKEN = "authToken";
    public static final String CASE_IDS = "caseId1, caseId2";

    @Test
     public void testPaymentFailureBounceChequeDBInsert(){

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.save(any())).thenReturn(paymentFailures);
        PaymentFailures paymentFailures = paymentStatusUpdateServiceImpl.insertBounceChequePaymentFailure(paymentStatusBouncedChequeDto);
        assertNotNull(paymentFailures);

    }

    @Test
    public void testFindFailureReferenceForBounceChequePayment(){

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto = getPaymentStatusBouncedChequeDto();

        PaymentFailures paymentFailures = PaymentFailures.paymentFailuresWith()
            .failureType("Bounced Cheque")
            .paymentReference("RC1234")
            .amount(BigDecimal.valueOf(555))
            .ccdCaseNumber("123456789")
            .failureReference("FR12345")
            .id(1)
            .reason("RR001")
            .build();

         when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.of(paymentFailures));
        Optional<PaymentFailures> paymentFailuresResult=  paymentStatusUpdateServiceImpl.searchFailureReference(paymentStatusBouncedChequeDto.getFailureReference());
        assertThat(paymentFailuresResult).isNotNull();
        assertEquals(Optional.of(1),Optional.of(paymentFailuresResult.stream().findFirst().get().getId()));
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

        PaymentStatusChargebackDto paymentStatusChargebackDto =getPaymentStatusChargebackDto();
        when(paymentStatusDtoMapper.ChargebackRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.save(any())).thenReturn(paymentFailures);
        PaymentFailures paymentFailures = paymentStatusUpdateServiceImpl.insertChargebackPaymentFailure(paymentStatusChargebackDto);
        assertNotNull(paymentFailures);

    }

    @Test
    public void testFindFailureReferenceForChargebackPayment(){

        PaymentStatusChargebackDto paymentStatusChargebackDto =getPaymentStatusChargebackDto();

        PaymentFailures paymentFailures = PaymentFailures.paymentFailuresWith()
            .failureType("Bounced Cheque")
            .paymentReference("RC1234")
            .amount(BigDecimal.valueOf(555))
            .ccdCaseNumber("123456789")
            .failureReference("FR12345")
            .id(1)
            .reason("RR001")
            .build();

        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.of(paymentFailures));
        Optional<PaymentFailures> paymentFailuresResult=  paymentStatusUpdateServiceImpl.searchFailureReference(paymentStatusChargebackDto.getFailureReference());
        assertThat(paymentFailuresResult).isNotNull();
        assertEquals(Optional.of(1),Optional.of(paymentFailuresResult.stream().findFirst().get().getId()));
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
    public void testDeleteByPaymentReference() {
        long value = 1;
        when(paymentFailureRepository.deleteByFailureReference(anyString())).thenReturn(value);
        paymentStatusUpdateServiceImpl.deleteByFailureReference("dummy");
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

}
