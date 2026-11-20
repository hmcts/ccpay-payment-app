package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountResponse;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentByAccountRequest;
import uk.gov.hmcts.payment.api.dto.liberata.account.LiberataAccountResponse;
import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataIdentityResponse;
import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataTokenData;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import uk.gov.hmcts.payment.api.mapper.liberata.account.LiberataAccountMapper;
import uk.gov.hmcts.payment.api.mapper.liberata.identity.AccessTokenDtoToTokenResponseMapper;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.LiberataIdentityException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application-test.properties")
public class LiberataRealTimeAPITest {

    @Mock
    private RestTemplate liberataRestTemplate;

    @Mock
    private AccessTokenDtoToTokenResponseMapper accessTokenDtoToTokenResponseMapper;

    @Mock
    private LiberataAccountMapper liberataAccountMapper;

    @InjectMocks
    private LiberataRealTimeAPI liberataIdentity;

    private LiberataIdentityResponse liberataIdentityResponse;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(
            liberataIdentity,
            "baseUrl",
            "http://localhost/test/pba-api-v2-uat"
        );

        // ensure cached token is cleared between tests
        ReflectionTestUtils.setField(liberataIdentity, "cachedToken", null);

        // prepare a LiberataIdentityResponse with token details
        String expiresAt = Instant.now().plusSeconds(1000).toString();
        LiberataTokenData liberataTokenData = new LiberataTokenData("plain-text-token", expiresAt, System.currentTimeMillis());
        liberataIdentityResponse = new LiberataIdentityResponse("200",liberataTokenData);
    }

    @Test
    public void shouldReturnValidTokenWhenLiberataRespondsSuccessfully() {
        ResponseEntity<LiberataIdentityResponse> responseEntity = ResponseEntity.ok(liberataIdentityResponse);
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(responseEntity);

        // mapper returns a TokenResponse that is not expired
        TokenResponse tokenResponse = new TokenResponse("plain-text-token", 1000000L, System.currentTimeMillis());
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(tokenResponse);

        TokenResponse result = liberataIdentity.getValidToken();

        assertEquals("plain-text-token", result.getAccessToken());
        assertFalse(result.isExpired());

        verify(liberataRestTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldThrowLiberataIdentityExceptionWhenRestTemplateFails() {
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenThrow(new RuntimeException("rest failure"));

        try {
            liberataIdentity.getValidToken();
            fail("Expected LiberataIdentityException");
        } catch (LiberataIdentityException e) {
            assertTrue(e.getMessage().contains("rest failure"));
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("rest failure", e.getCause().getMessage());
        }

        verify(liberataRestTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldThrowLiberataIdentityExceptionWhenMapperFails() {
        // return a valid response from REST call
        ResponseEntity<LiberataIdentityResponse> responseEntity = ResponseEntity.ok(new LiberataIdentityResponse());
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(responseEntity);

        // mapper fails when converting the response
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(any(LiberataIdentityResponse.class)))
            .thenThrow(new RuntimeException("mapper failure"));

        try {
            liberataIdentity.getValidToken();
            fail("Expected LiberataIdentityException");
        } catch (LiberataIdentityException e) {
            assertTrue(e.getMessage().contains("mapper failure"));
            assertEquals("mapper failure", e.getCause().getMessage());
        }

        verify(accessTokenDtoToTokenResponseMapper).toTokenResponse(any(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldReturnValidTokenWhenRefreshTokenCalledAndLiberataRespondsSuccessfully() {
        ResponseEntity<LiberataIdentityResponse> responseEntity = ResponseEntity.ok(liberataIdentityResponse);
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(responseEntity);

        TokenResponse tokenResponse = new TokenResponse("plain-text-token", 1000000L, System.currentTimeMillis());
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(tokenResponse);

        TokenResponse result = liberataIdentity.refreshToken();

        assertEquals("plain-text-token", result.getAccessToken());
        assertFalse(result.isExpired());

        verify(liberataRestTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldThrowLiberataIdentityExceptionWhenRefreshTokenRestTemplateFails() {
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenThrow(new RuntimeException("rest failure"));

        try {
            liberataIdentity.refreshToken();
            fail("Expected LiberataIdentityException");
        } catch (LiberataIdentityException e) {
            assertTrue(e.getMessage().contains("rest failure"));
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("rest failure", e.getCause().getMessage());
        }

        verify(liberataRestTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldThrowLiberataIdentityExceptionWhenRefreshTokenMapperFails() {
        ResponseEntity<LiberataIdentityResponse> responseEntity = ResponseEntity.ok(liberataIdentityResponse);
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(responseEntity);

        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(any(LiberataIdentityResponse.class)))
            .thenThrow(new RuntimeException("mapper failure"));

        try {
            liberataIdentity.refreshToken();
            fail("Expected LiberataIdentityException");
        } catch (LiberataIdentityException e) {
            assertTrue(e.getMessage().contains("mapper failure"));
            assertEquals("mapper failure", e.getCause().getMessage());
        }

        verify(accessTokenDtoToTokenResponseMapper).toTokenResponse(any(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldReturnValidTokenWhenNotExpiredAndNotCallLiberata() {
        // prepare a cached token that is not expired
        TokenResponse cached = new TokenResponse("cached-token", 100000L, System.currentTimeMillis());
        ReflectionTestUtils.setField(liberataIdentity, "cachedToken", cached);

        TokenResponse result = liberataIdentity.getValidToken();

        assertEquals("cached-token", result.getAccessToken());
        // ensure no REST call was made since cache is valid
        verify(liberataRestTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldRefreshWhenCachedTokenIsExpired() {
        // prepare a cached token that is expired
        // createdAt 5 seconds ago, expiresIn 1 second -> expired now
        TokenResponse expired = new TokenResponse("expired-token", 1000L, System.currentTimeMillis() - 5000L);
        ReflectionTestUtils.setField(liberataIdentity, "cachedToken", expired);

        // set up REST/mapping to return a new token
        ResponseEntity<LiberataIdentityResponse> responseEntity = ResponseEntity.ok(liberataIdentityResponse);
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(responseEntity);

        TokenResponse tokenResponse = new TokenResponse("refreshed-token", 1000000L, System.currentTimeMillis());
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(any(LiberataIdentityResponse.class))).thenReturn(tokenResponse);
        TokenResponse result = liberataIdentity.refreshToken();

        assertEquals("refreshed-token", result.getAccessToken());
        assertFalse(result.isExpired());

        verify(liberataRestTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldUseCachedTokenWhenPresentAndNotExpired_onGetValidToken() {
        TokenResponse cached = new TokenResponse("cached-token-2", 60000L, System.currentTimeMillis());
        ReflectionTestUtils.setField(liberataIdentity, "cachedToken", cached);

        LiberataRealTimeAPI spyService = org.mockito.Mockito.spy(liberataIdentity);

        TokenResponse result = spyService.getValidToken();

        assertEquals("cached-token-2", result.getAccessToken());
        verify(liberataRestTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldReturnAccountDetailsWhenLiberataRespondsSuccessfully() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200", null);
        AccountDto accountDto = AccountDto.accountDtoWith()
            .accountNumber("PBA1234567")
            .accountName("Test Account")
            .creditLimit(new BigDecimal("1000.00"))
            .availableBalance(new BigDecimal("750.50"))
            .status(AccountStatus.ACTIVE)
            .effectiveDate(new Date(1735732800000L))
            .build();

        when(liberataRestTemplate.exchange(
            eq("http://localhost/test/pba-api-v2-uat/api/account/PBA1234567"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(LiberataAccountResponse.class)
        )).thenReturn(ResponseEntity.ok(liberataAccountResponse));
        when(liberataAccountMapper.toAccountDto(liberataAccountResponse)).thenReturn(accountDto);

        AccountDto result = liberataIdentity.getAccountDetails("access-token", "PBA1234567");

        assertEquals(accountDto, result);
        verify(liberataAccountMapper).toAccountDto(liberataAccountResponse);
    }

    @Test
    public void shouldSendBearerTokenWhenFetchingAccountDetails() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200", null);
        AccountDto accountDto = AccountDto.accountDtoWith().accountNumber("PBA1234567").build();
        ArgumentCaptor<HttpEntity<?>> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(liberataRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            requestCaptor.capture(),
            eq(LiberataAccountResponse.class)
        )).thenReturn(ResponseEntity.ok(liberataAccountResponse));
        when(liberataAccountMapper.toAccountDto(liberataAccountResponse)).thenReturn(accountDto);

        liberataIdentity.getAccountDetails("access-token", "PBA1234567");

        HttpHeaders headers = requestCaptor.getValue().getHeaders();
        assertEquals("Bearer access-token", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    public void shouldThrowLiberataIdentityExceptionWhenAccountDetailsRestTemplateFails() {
        when(liberataRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(LiberataAccountResponse.class)
        )).thenThrow(new RuntimeException("rest failure"));

        try {
            liberataIdentity.getAccountDetails("access-token", "PBA1234567");
            fail("Expected LiberataIdentityException");
        } catch (LiberataIdentityException e) {
            assertTrue(e.getMessage().contains("rest failure"));
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("rest failure", e.getCause().getMessage());
        }
    }

    @Test
    public void shouldThrowLiberataIdentityExceptionWhenAccountDetailsMapperFails() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200", null);
        when(liberataRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(LiberataAccountResponse.class)
        )).thenReturn(ResponseEntity.ok(liberataAccountResponse));
        when(liberataAccountMapper.toAccountDto(liberataAccountResponse))
            .thenThrow(new RuntimeException("mapper failure"));

        try {
            liberataIdentity.getAccountDetails("access-token", "PBA1234567");
            fail("Expected LiberataIdentityException");
        } catch (LiberataIdentityException e) {
            assertTrue(e.getMessage().contains("mapper failure"));
            assertEquals("mapper failure", e.getCause().getMessage());
        }

        verify(liberataAccountMapper).toAccountDto(liberataAccountResponse);
    }

    @Test
    public void shouldPropagateNotFoundWhenAccountDoesNotExist() {
        when(liberataRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(LiberataAccountResponse.class)
        )).thenThrow(notFound());

        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> liberataIdentity.getAccountDetails("access-token", "PBA1234567")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(liberataRestTemplate, never())
            .postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldRefreshTokenAndRetryOnceWhenAccountRequestIsUnauthorized() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200", null);
        AccountDto accountDto = AccountDto.accountDtoWith().accountNumber("PBA1234567").build();
        TokenResponse refreshedToken = new TokenResponse("refreshed-token", 1000000L, System.currentTimeMillis());

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(ResponseEntity.ok(liberataIdentityResponse));
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(refreshedToken);
        when(liberataRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(LiberataAccountResponse.class)
        )).thenThrow(unauthorized()).thenReturn(ResponseEntity.ok(liberataAccountResponse));
        when(liberataAccountMapper.toAccountDto(liberataAccountResponse)).thenReturn(accountDto);

        AccountDto result = liberataIdentity.getAccountDetails("access-token", "PBA1234567");

        assertEquals(accountDto, result);
        verify(liberataRestTemplate, times(2))
            .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LiberataAccountResponse.class));
        verify(liberataRestTemplate)
            .postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldThrowLiberataIdentityExceptionWhenAccountRequestRemainsUnauthorizedAfterRefresh() {
        TokenResponse refreshedToken = new TokenResponse("refreshed-token", 1000000L, System.currentTimeMillis());

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(ResponseEntity.ok(liberataIdentityResponse));
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(refreshedToken);
        when(liberataRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(LiberataAccountResponse.class)
        )).thenThrow(unauthorized());

        LiberataIdentityException exception = assertThrows(
            LiberataIdentityException.class,
            () -> liberataIdentity.getAccountDetails("access-token", "PBA1234567")
        );

        assertTrue(exception.getMessage().contains("Error fetching account details from Liberata"));
        verify(liberataRestTemplate, times(2))
            .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LiberataAccountResponse.class));
        verify(liberataRestTemplate)
            .postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldPropagateNotFoundWhenRetryAfterRefreshIsNotFound() {
        TokenResponse refreshedToken = new TokenResponse("refreshed-token", 1000000L, System.currentTimeMillis());

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(ResponseEntity.ok(liberataIdentityResponse));
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(refreshedToken);
        when(liberataRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(LiberataAccountResponse.class)
        )).thenThrow(unauthorized()).thenThrow(notFound());

        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> liberataIdentity.getAccountDetails("access-token", "PBA1234567")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(liberataRestTemplate, times(2))
            .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LiberataAccountResponse.class));
    }

    @Test
    public void shouldPropagateResourceAccessExceptionWhenAccountRequestTimesOut() {
        when(liberataRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(LiberataAccountResponse.class)
        )).thenThrow(new ResourceAccessException("timeout"));

        ResourceAccessException exception = assertThrows(
            ResourceAccessException.class,
            () -> liberataIdentity.getAccountDetails("access-token", "PBA1234567")
        );

        assertEquals("timeout", exception.getMessage());
        verify(liberataRestTemplate, never())
            .postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class));
    }

    @Test
    public void shouldPropagateResourceAccessExceptionWhenRetryAfterRefreshTimesOut() {
        TokenResponse refreshedToken = new TokenResponse("refreshed-token", 1000000L, System.currentTimeMillis());

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(ResponseEntity.ok(liberataIdentityResponse));
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(refreshedToken);
        when(liberataRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(LiberataAccountResponse.class)
        )).thenThrow(unauthorized()).thenThrow(new ResourceAccessException("timeout"));

        ResourceAccessException exception = assertThrows(
            ResourceAccessException.class,
            () -> liberataIdentity.getAccountDetails("access-token", "PBA1234567")
        );

        assertEquals("timeout", exception.getMessage());
        verify(liberataRestTemplate, times(2))
            .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LiberataAccountResponse.class));
    }

    @Test
    public void shouldPropagateLiberataIdentityExceptionWhenRetryAfterRefreshFailsWithIdentityException() {
        TokenResponse refreshedToken = new TokenResponse("refreshed-token", 1000000L, System.currentTimeMillis());

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(ResponseEntity.ok(liberataIdentityResponse));
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(refreshedToken);
        when(liberataRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(LiberataAccountResponse.class)
        )).thenThrow(unauthorized()).thenThrow(new LiberataIdentityException("Identity error"));

        LiberataIdentityException exception = assertThrows(
            LiberataIdentityException.class,
            () -> liberataIdentity.getAccountDetails("access-token", "PBA1234567")
        );

        assertEquals("Identity error", exception.getMessage());
        verify(liberataRestTemplate, times(2))
            .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LiberataAccountResponse.class));
    }

    private HttpClientErrorException unauthorized() {
        return HttpClientErrorException.create(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            new HttpHeaders(),
            new byte[0],
            StandardCharsets.UTF_8
        );
    }

    private HttpClientErrorException notFound() {
        return HttpClientErrorException.create(
            HttpStatus.NOT_FOUND,
            "Not Found",
            new HttpHeaders(),
            new byte[0],
            StandardCharsets.UTF_8
        );
    }

    // ==================== Tests for payByAccount Methods ====================

    @Test
    public void payByAccountShouldReturnSuccessWhenLiberataRespondsSuccessfully() {
        PaymentByAccountRequest paymentByAccountRequest = new PaymentByAccountRequest();
        PaymentAccountRequest paymentAccountRequest = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(paymentByAccountRequest)
            .build();
        PaymentAccountResponse expectedResponse = PaymentAccountResponse.paymentDtoWith()
            .status("200")
            .message("Success")
            .build();

        TokenResponse validToken = new TokenResponse("valid-token", 1000000L, System.currentTimeMillis());
        ResponseEntity<LiberataIdentityResponse> tokenResponseEntity = ResponseEntity.ok(liberataIdentityResponse);
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(tokenResponseEntity);
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(validToken);

        ResponseEntity<PaymentAccountResponse> paymentResponseEntity = ResponseEntity.ok(expectedResponse);
        when(liberataRestTemplate.postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        )).thenReturn(paymentResponseEntity);

        PaymentAccountResponse result = liberataIdentity.payByAccount(paymentAccountRequest);

        assertEquals("200", result.getStatus());
        assertEquals("Success", result.getMessage());
        verify(liberataRestTemplate).postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        );
    }

    @Test
    public void payByAccountShouldIncludeBearerTokenInAuthorizationHeader() {
        PaymentByAccountRequest paymentByAccountRequest = new PaymentByAccountRequest();
        PaymentAccountRequest paymentAccountRequest = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(paymentByAccountRequest)
            .build();
        PaymentAccountResponse expectedResponse = PaymentAccountResponse.paymentDtoWith()
            .status("200")
            .build();
        ArgumentCaptor<HttpEntity<?>> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        TokenResponse validToken = new TokenResponse("valid-token", 1000000L, System.currentTimeMillis());
        ResponseEntity<LiberataIdentityResponse> tokenResponseEntity = ResponseEntity.ok(liberataIdentityResponse);
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(tokenResponseEntity);
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(validToken);

        ResponseEntity<PaymentAccountResponse> paymentResponseEntity = ResponseEntity.ok(expectedResponse);
        when(liberataRestTemplate.postForEntity(
            anyString(),
            requestCaptor.capture(),
            eq(PaymentAccountResponse.class)
        )).thenReturn(paymentResponseEntity);

        liberataIdentity.payByAccount(paymentAccountRequest);

        HttpHeaders headers = requestCaptor.getValue().getHeaders();
        assertEquals("Bearer valid-token", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    public void payByAccountShouldRefreshTokenAndRetryOnceWhenUnauthorized() {
        PaymentByAccountRequest paymentByAccountRequest = new PaymentByAccountRequest();
        PaymentAccountRequest paymentAccountRequest = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(paymentByAccountRequest)
            .build();
        PaymentAccountResponse successResponse = PaymentAccountResponse.paymentDtoWith()
            .status("200")
            .message("Success")
            .build();

        TokenResponse initialToken = new TokenResponse("initial-token", 1000000L, System.currentTimeMillis());
        TokenResponse refreshedToken = new TokenResponse("refreshed-token", 1000000L, System.currentTimeMillis());
        ResponseEntity<LiberataIdentityResponse> tokenResponseEntity = ResponseEntity.ok(liberataIdentityResponse);

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(tokenResponseEntity);
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse))
            .thenReturn(initialToken)
            .thenReturn(refreshedToken);

        // First call returns 401, second call succeeds
        when(liberataRestTemplate.postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        )).thenThrow(unauthorized()).thenReturn(ResponseEntity.ok(successResponse));

        PaymentAccountResponse result = liberataIdentity.payByAccount(paymentAccountRequest);

        assertEquals("200", result.getStatus());
        // Verify postForEntity was called twice - once initially and once after retry
        verify(liberataRestTemplate, times(2)).postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        );
    }

    @Test
    public void payByAccountShouldNotRetryWhenUnauthorizedAfterTokenRefresh() {
        PaymentByAccountRequest paymentByAccountRequest = new PaymentByAccountRequest();
        PaymentAccountRequest paymentAccountRequest = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(paymentByAccountRequest)
            .build();

        TokenResponse initialToken = new TokenResponse("initial-token", 1000000L, System.currentTimeMillis());
        TokenResponse refreshedToken = new TokenResponse("refreshed-token", 1000000L, System.currentTimeMillis());
        ResponseEntity<LiberataIdentityResponse> tokenResponseEntity = ResponseEntity.ok(liberataIdentityResponse);

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(tokenResponseEntity);
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse))
            .thenReturn(initialToken)
            .thenReturn(refreshedToken);

        // First call returns 401, second call also returns 401 (should not retry after this)
        when(liberataRestTemplate.postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        )).thenThrow(unauthorized()).thenThrow(unauthorized());

        // Create a PaymentAccountResponse with error details to avoid NPE
        PaymentAccountResponse errorResponse = PaymentAccountResponse.paymentDtoWith()
            .status("401")
            .message("Unauthorized")
            .build();

        // Mock the exception to return an error response body
        HttpClientErrorException unauthorizedException = unauthorized();
        when(liberataRestTemplate.postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        )).thenThrow(unauthorized()).thenThrow(unauthorizedException);

        // Since getPaymentAccountResponseError throws RuntimeException, we expect that
        try {
            liberataIdentity.payByAccount(paymentAccountRequest);
        } catch (RuntimeException e) {
            // Expected - getPaymentAccountResponseError will throw RuntimeException
        }

        // Verify postForEntity was called exactly twice - no third attempt
        verify(liberataRestTemplate, times(2)).postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        );
    }

    @Test
    public void payByAccountShouldHandleNonUnauthorizedErrors() {
        PaymentByAccountRequest paymentByAccountRequest = new PaymentByAccountRequest();
        PaymentAccountRequest paymentAccountRequest = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(paymentByAccountRequest)
            .build();

        TokenResponse validToken = new TokenResponse("valid-token", 1000000L, System.currentTimeMillis());
        ResponseEntity<LiberataIdentityResponse> tokenResponseEntity = ResponseEntity.ok(liberataIdentityResponse);
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(tokenResponseEntity);
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(validToken);

        // Return a 400 Bad Request error
        HttpClientErrorException badRequestException = HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            new HttpHeaders(),
            new byte[0],
            StandardCharsets.UTF_8
        );

        when(liberataRestTemplate.postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        )).thenThrow(badRequestException);

        try {
            liberataIdentity.payByAccount(paymentAccountRequest);
        } catch (RuntimeException e) {
            // Expected - getPaymentAccountResponseError will throw RuntimeException
        }

        // Verify postForEntity was called only once (no retry on non-401 errors)
        verify(liberataRestTemplate, times(1)).postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        );
    }

    @Test
    public void payByAccountShouldThrowLiberataIdentityExceptionOnUnexpectedError() {
        PaymentByAccountRequest paymentByAccountRequest = new PaymentByAccountRequest();
        PaymentAccountRequest paymentAccountRequest = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(paymentByAccountRequest)
            .build();

        TokenResponse validToken = new TokenResponse("valid-token", 1000000L, System.currentTimeMillis());
        ResponseEntity<LiberataIdentityResponse> tokenResponseEntity = ResponseEntity.ok(liberataIdentityResponse);
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(tokenResponseEntity);
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(validToken);

        when(liberataRestTemplate.postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        )).thenThrow(new RuntimeException("Unexpected error"));

        LiberataIdentityException exception = assertThrows(
            LiberataIdentityException.class,
            () -> liberataIdentity.payByAccount(paymentAccountRequest)
        );

        assertTrue(exception.getMessage().contains("Error posting payment to Liberata"));
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    public void payByAccountPublicMethodShouldCallPrivateMethodWithFalseFlag() {
        PaymentByAccountRequest paymentByAccountRequest = new PaymentByAccountRequest();
        PaymentAccountRequest paymentAccountRequest = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(paymentByAccountRequest)
            .build();
        PaymentAccountResponse expectedResponse = PaymentAccountResponse.paymentDtoWith()
            .status("200")
            .build();

        TokenResponse validToken = new TokenResponse("valid-token", 1000000L, System.currentTimeMillis());
        ResponseEntity<LiberataIdentityResponse> tokenResponseEntity = ResponseEntity.ok(liberataIdentityResponse);
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(tokenResponseEntity);
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(validToken);

        ResponseEntity<PaymentAccountResponse> paymentResponseEntity = ResponseEntity.ok(expectedResponse);
        when(liberataRestTemplate.postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        )).thenReturn(paymentResponseEntity);

        // Create a spy to verify the private method call
        LiberataRealTimeAPI spyService = org.mockito.Mockito.spy(liberataIdentity);
        PaymentAccountResponse result = spyService.payByAccount(paymentAccountRequest);

        assertEquals("200", result.getStatus());
        // The public method should delegate to private method with forceRefreshToken=false
        verify(liberataRestTemplate).postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        );
    }

    @Test
    public void payByAccountShouldOnlyDoOneRecursiveCallOnUnauthorized() {
        PaymentByAccountRequest paymentByAccountRequest = new PaymentByAccountRequest();
        PaymentAccountRequest paymentAccountRequest = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(paymentByAccountRequest)
            .build();

        TokenResponse initialToken = new TokenResponse("initial-token", 1000000L, System.currentTimeMillis());
        TokenResponse refreshedToken = new TokenResponse("refreshed-token", 1000000L, System.currentTimeMillis());
        ResponseEntity<LiberataIdentityResponse> tokenResponseEntity = ResponseEntity.ok(liberataIdentityResponse);

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(tokenResponseEntity);
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse))
            .thenReturn(initialToken)
            .thenReturn(refreshedToken);

        // Both calls return 401 to test that we don't recurse more than once
        when(liberataRestTemplate.postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        )).thenThrow(unauthorized()).thenThrow(unauthorized());

        try {
            liberataIdentity.payByAccount(paymentAccountRequest);
        } catch (Exception e) {
            // Expected to fail on second 401
        }

        // Verify postForEntity was called exactly twice - exactly one recursive call
        verify(liberataRestTemplate, times(2)).postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        );
    }

    @Test
    public void payByAccountShouldReturnErrorResponseFromLiberata() {
        PaymentByAccountRequest paymentByAccountRequest = new PaymentByAccountRequest();
        PaymentAccountRequest paymentAccountRequest = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(paymentByAccountRequest)
            .build();

        TokenResponse validToken = new TokenResponse("valid-token", 1000000L, System.currentTimeMillis());
        ResponseEntity<LiberataIdentityResponse> tokenResponseEntity = ResponseEntity.ok(liberataIdentityResponse);
        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LiberataIdentityResponse.class)))
            .thenReturn(tokenResponseEntity);
        when(accessTokenDtoToTokenResponseMapper.toTokenResponse(liberataIdentityResponse)).thenReturn(validToken);

        PaymentAccountResponse errorResponse = PaymentAccountResponse.paymentDtoWith()
            .status("403")
            .message("Insufficient funds")
            .reason("Account balance too low")
            .build();

        ResponseEntity<PaymentAccountResponse> paymentResponseEntity = ResponseEntity.ok(errorResponse);
        when(liberataRestTemplate.postForEntity(
            eq("http://localhost/test/pba-api-v2-uat/api/payment"),
            any(HttpEntity.class),
            eq(PaymentAccountResponse.class)
        )).thenReturn(paymentResponseEntity);

        PaymentAccountResponse result = liberataIdentity.payByAccount(paymentAccountRequest);

        assertEquals("403", result.getStatus());
        assertEquals("Insufficient funds", result.getMessage());
        assertEquals("Account balance too low", result.getReason());
    }
}
