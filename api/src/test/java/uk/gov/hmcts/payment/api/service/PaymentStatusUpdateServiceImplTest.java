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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.exception.RefundServiceUnavailableException;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.dto.servicerequest.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.model.PaymentFailureRepository;
import uk.gov.hmcts.payment.api.model.PaymentFailures;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.sql.Timestamp;
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

    @Test
     public void testPaymentFailureDBInsert(){

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.save(any())).thenReturn(paymentFailures);
        PaymentFailures paymentFailures = paymentStatusUpdateServiceImpl.insertBounceChequePaymentFailure(paymentStatusBouncedChequeDto);
        assertNotNull(paymentFailures);

    }

    @Test
    public void testFindFailureReferenceForPayment(){

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
        Optional<PaymentFailures> paymentFailuresResult=  paymentStatusUpdateServiceImpl.searchFailureReference(paymentStatusBouncedChequeDto);
        assertThat(paymentFailuresResult).isNotNull();
        assertEquals(1,paymentFailuresResult.stream().findFirst().get().getId());
    }

    @Test
    public void testSucessCanCelFailureRefund(){

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("service auth token");
        when(this.restTemplateRefundCancel.exchange(anyString(),
            eq(HttpMethod.PATCH),
            any(HttpEntity.class),
            eq(String.class), any(Map.class)))
            .thenReturn(new ResponseEntity(HttpStatus.OK));
        boolean cancelRefund = paymentStatusUpdateServiceImpl.cancelFailurePaymentRefund(paymentStatusBouncedChequeDto);

        assertTrue(cancelRefund);
    }


    @Test
    public void returnRefundServiceUnavailableExceptionWhenRefundReturnHttpServerErrorException(){

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto = getPaymentStatusBouncedChequeDto();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("service auth token");
        when(this.restTemplateRefundCancel.exchange(anyString(),
            eq(HttpMethod.PATCH),
            any(HttpEntity.class),
            eq(String.class), any(Map.class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.NOT_FOUND));
        assertThrows(RefundServiceUnavailableException.class, () ->paymentStatusUpdateServiceImpl.cancelFailurePaymentRefund(paymentStatusBouncedChequeDto));

    }

    @Test
    public void returnPaymentNotFoundExceptionWhenRefundReturnHttpClientErrorException(){

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto = getPaymentStatusBouncedChequeDto();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("ServiceAuthorization", "service-auth");
        when(authTokenGenerator.generate()).thenReturn("service auth token");
        when(this.restTemplateRefundCancel.exchange(anyString(),
            eq(HttpMethod.PATCH),
            any(HttpEntity.class),
            eq(String.class), any(Map.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertThrows(PaymentNotFoundException.class, () ->paymentStatusUpdateServiceImpl.cancelFailurePaymentRefund(paymentStatusBouncedChequeDto));

    }

    private PaymentStatusBouncedChequeDto getPaymentStatusBouncedChequeDto() {

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto = PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(555))
            .failureReference("FR12345")
            .failure_event_date_time(Timestamp.valueOf("2021-10-10 10:10:10"))
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .build();

        return paymentStatusBouncedChequeDto;
    }

}
