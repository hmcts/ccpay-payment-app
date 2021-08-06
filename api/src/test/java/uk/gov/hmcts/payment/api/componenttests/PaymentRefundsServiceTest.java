package uk.gov.hmcts.payment.api.componenttests;


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
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.PaymentRefundsService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        .paymentStatus(PaymentStatus.paymentStatusWith().name("Success").build())
        .build();
    ;
    @MockBean
    private Payment2Repository paymentRepository;
    @MockBean
    @Autowired()
    @Qualifier("restTemplateRefundsGroup")
    private RestTemplate restTemplate;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private PaymentRefundsService paymentRefundsService;

    @Before
    public void setup() {
        System.gc();
        header.put("Authorization", Collections.singletonList("Bearer 131313"));
    }

    @Test
    public void createSuccessfulRefund() throws Exception {

        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));

        RefundResponse mockRefundResponse = RefundResponse.RefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();


        ResponseEntity<RefundResponse> responseEntity = new ResponseEntity<>(mockRefundResponse, HttpStatus.CREATED);

        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(RefundResponse.class))).thenReturn(responseEntity);

        ResponseEntity<RefundResponse> refundResponse = paymentRefundsService.CreateRefund(paymentRefundRequest, header);

        assertEquals("RF-4321-4321-4321-4321", refundResponse.getBody().getRefundReference());

    }


    @Test(expected = InvalidRefundRequestException.class)
    public void createRefundWithFailedReference() throws Exception {

        Payment mockPaymentFailed = Payment.paymentWith().reference("RC-1234-1234-1234-1234")
            .paymentStatus(PaymentStatus.paymentStatusWith().name("Failed").build())
            .build();

        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentFailed));

        paymentRefundsService.CreateRefund(paymentRefundRequest, header);

    }


    @Test(expected = InvalidRefundRequestException.class)
    public void createRefundWithClientException() throws Exception {

        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));


        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(RefundResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        paymentRefundsService.CreateRefund(paymentRefundRequest, header);

    }

    @Test(expected = GatewayTimeoutException.class)
    public void createRefundWithServerException() throws Exception {

        Mockito.when(paymentRepository.findByReference(any())).thenReturn(Optional.ofNullable(mockPaymentSuccess));


        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(RefundResponse.class))).thenThrow(new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT));

        paymentRefundsService.CreateRefund(paymentRefundRequest, header);

    }


}
