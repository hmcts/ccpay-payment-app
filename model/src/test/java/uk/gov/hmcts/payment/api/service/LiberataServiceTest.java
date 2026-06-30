package uk.gov.hmcts.payment.api.service;

import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.PaymentByAccountPayment;
import uk.gov.hmcts.payment.api.dto.PaymentByAccountRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiberataServiceTest {

    private static final String TOKEN_URL = "https://liberata.example/token";
    private static final String PBA_URL = "https://liberata.example/payment";

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity<String>> tokenRequestCaptor;

    @Captor
    private ArgumentCaptor<HttpEntity<PaymentByAccountRequest>> pbaRequestCaptor;

    private LiberataService liberataService;

    @BeforeEach
    void setUp() {
        liberataService = new LiberataService();
        ReflectionTestUtils.setField(liberataService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(liberataService, "tokenUrl", TOKEN_URL);
        ReflectionTestUtils.setField(liberataService, "clientId", "client-id");
        ReflectionTestUtils.setField(liberataService, "clientSecret", "client-secret");
        ReflectionTestUtils.setField(liberataService, "username", "username");
        ReflectionTestUtils.setField(liberataService, "password", "password");
        ReflectionTestUtils.setField(liberataService, "pbaUrl", PBA_URL);
    }

    @Test
    void getAccessTokenReturnsTokenFromSuccessfulResponse() {
        JSONObject responseBody = new JSONObject();
        responseBody.put("access_token", "access-token");

        when(restTemplate.exchange(eq(TOKEN_URL), eq(HttpMethod.POST), tokenRequestCaptor.capture(), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        String accessToken = liberataService.getAccessToken();

        assertThat(accessToken).isEqualTo("access-token");

        HttpEntity<String> request = tokenRequestCaptor.getValue();
        assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
        assertThat(request.getBody())
            .isEqualTo("grant_type=password&username=username&password=password&client_id=client-id&client_secret=client-secret");
    }

    @Test
    void getAccessTokenThrowsWhenTokenResponseIsNotOk() {
        when(restTemplate.exchange(eq(TOKEN_URL), eq(HttpMethod.POST), tokenRequestCaptor.capture(), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(new JSONObject(), HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> liberataService.getAccessToken())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to obtain access token: 401 UNAUTHORIZED");
    }

    @Test
    void payByAccountUsesBearerTokenAndReturnsSuccessfulResponse() {
        JSONObject tokenResponseBody = new JSONObject();
        tokenResponseBody.put("access_token", "access-token");
        JSONObject paymentResponseBody = new JSONObject();
        paymentResponseBody.put("error_code", "0");
        PaymentByAccountRequest paymentRequest = paymentByAccountRequest();

        when(restTemplate.exchange(eq(TOKEN_URL), eq(HttpMethod.POST), tokenRequestCaptor.capture(), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(tokenResponseBody, HttpStatus.OK));
        when(restTemplate.exchange(eq(PBA_URL), eq(HttpMethod.POST), pbaRequestCaptor.capture(), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(paymentResponseBody, HttpStatus.OK));

        ResponseEntity<JSONObject> response = liberataService.payByAccount(paymentRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(paymentResponseBody);

        HttpEntity<PaymentByAccountRequest> request = pbaRequestCaptor.getValue();
        assertThat(request.getBody()).isSameAs(paymentRequest);
        assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer access-token");
    }

    @Test
    void payByAccountReturnsBadRequestResponse() {
        JSONObject responseBody = new JSONObject();

        mockAccessToken();
        when(restTemplate.exchange(eq(PBA_URL), eq(HttpMethod.POST), pbaRequestCaptor.capture(), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST));

        ResponseEntity<JSONObject> response = liberataService.payByAccount(paymentByAccountRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void payByAccountReturnsForbiddenResponse() {
        JSONObject responseBody = new JSONObject();
        responseBody.put("error_code", "3");

        mockAccessToken();
        when(restTemplate.exchange(eq(PBA_URL), eq(HttpMethod.POST), pbaRequestCaptor.capture(), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.FORBIDDEN));

        ResponseEntity<JSONObject> response = liberataService.payByAccount(paymentByAccountRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void payByAccountThrowsWhenPaymentResponseStatusIsUnexpected() {
        JSONObject responseBody = new JSONObject();
        responseBody.put("error_code", "9");

        mockAccessToken();
        when(restTemplate.exchange(eq(PBA_URL), eq(HttpMethod.POST), pbaRequestCaptor.capture(), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> liberataService.payByAccount(paymentByAccountRequest()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to make payment by account: 500 INTERNAL_SERVER_ERROR");
    }

    private void mockAccessToken() {
        JSONObject tokenResponseBody = new JSONObject();
        tokenResponseBody.put("access_token", "access-token");

        when(restTemplate.exchange(eq(TOKEN_URL), eq(HttpMethod.POST), tokenRequestCaptor.capture(), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(tokenResponseBody, HttpStatus.OK));
    }

    private PaymentByAccountRequest paymentByAccountRequest() {
        return PaymentByAccountRequest.paymentByAccountRequestWith()
            .pbaNumber("pba1234567")
            .payment(PaymentByAccountPayment.paymentByAccountPaymentWith()
                .paymentReference("RC-1234-5678-9012-3456")
                .amount("100.00")
                .currency("GBP")
                .serviceName("service")
                .build())
            .build();
    }
}
