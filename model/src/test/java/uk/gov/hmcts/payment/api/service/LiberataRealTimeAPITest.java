package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.liberata.identity.AccessTokenDto;
import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataIdentityResponse;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenDto;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import uk.gov.hmcts.payment.api.mapper.liberata.identity.AccessTokenDtoToTokenResponseMapper;
import uk.gov.hmcts.payment.api.v1.model.exceptions.LiberataIdentityException;

import java.time.Instant;
import java.util.Arrays;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application-test.properties")
public class LiberataRealTimeAPITest {

    @Mock
    private RestTemplate liberataRestTemplate;

    @Mock
    private AccessTokenDtoToTokenResponseMapper accessTokenDtoToTokenResponseMapper;

    @InjectMocks
    private LiberataRealTimeAPI liberataIdentity;

    private LiberataIdentityResponse liberataIdentityResponse;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(
            liberataIdentity,
            "baseUrl",
            "http://localhost/test"
        );

        // ensure cached token is cleared between tests
        ReflectionTestUtils.setField(liberataIdentity, "cachedToken", null);

        // prepare a LiberataIdentityResponse with token details
        String createdAt = Instant.now().minusSeconds(10).toString();
        String expiresAt = Instant.now().plusSeconds(1000).toString();
        AccessTokenDto accessTokenDto = new AccessTokenDto("name", Arrays.asList("a"), expiresAt, 1, "type", null, createdAt, 1);
        TokenDto tokenDto = new TokenDto(accessTokenDto, "plain-text-token");
        liberataIdentityResponse = new LiberataIdentityResponse(tokenDto);
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
}
