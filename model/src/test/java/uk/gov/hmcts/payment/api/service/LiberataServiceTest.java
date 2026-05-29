package uk.gov.hmcts.payment.api.service;

import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiberataServiceTest {

    @Mock
    private RestTemplate restTemplateLaravel;

    private LiberataService liberataService;

    @BeforeEach
    void setUp() {
        liberataService = new LiberataService();
        ReflectionTestUtils.setField(liberataService, "restTemplateLaravel", restTemplateLaravel);
        ReflectionTestUtils.setField(liberataService, "laravelTokenUrl", "http://laravel.com/api/auth/token");
        ReflectionTestUtils.setField(liberataService, "laravelRefreshUrl", "http://laravel.com/api/auth/refresh");
        ReflectionTestUtils.setField(liberataService, "laravelEmailAddress", "user@example.com");
        ReflectionTestUtils.setField(liberataService, "laravelPassword", "password");
    }

    @Test
    void shouldFetchTokenWithCredentialsWhenNoCachedToken() {
        JSONObject body = tokenBody("test-token", "2099-01-01T23:59:59.246000Z");
        when(restTemplateLaravel.exchange(eq("http://laravel.com/api/auth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        String token = liberataService.getAccessToken();

        assertEquals("test-token", token);
        verify(restTemplateLaravel, times(1))
            .exchange(eq("http://laravel.com/api/auth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class));
    }

    @Test
    void shouldReturnCachedTokenWhenNotExpired() {
        JSONObject body = tokenBody("test-token", "2099-01-01T22:59:59.000000Z");
        when(restTemplateLaravel.exchange(eq("http://laravel.com/api/auth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        String firstToken = liberataService.getAccessToken();
        String secondToken = liberataService.getAccessToken();

        assertEquals("test-token", firstToken);
        assertEquals("test-token", secondToken);
        verify(restTemplateLaravel, times(1))
            .exchange(eq("http://laravel.com/api/auth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class));
    }

    @Test
    void shouldRefreshTokenWhenExpiredButInGracePeriod() {
        String oneSecondBeforeNowUtc = LocalDateTime.now(ZoneOffset.UTC)
            .minusSeconds(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"));
        JSONObject expiredTokenBody = tokenBody("expired-token", oneSecondBeforeNowUtc);
        JSONObject refreshedTokenBody = refreshedTokenBody("refreshed-token", "2099-01-01T00:00:00.000000Z", null);

        when(restTemplateLaravel.exchange(eq("http://laravel.com/api/auth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(expiredTokenBody, HttpStatus.OK));

        when(restTemplateLaravel.exchange(eq("http://laravel.com/api/auth/refresh"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(refreshedTokenBody, HttpStatus.OK));

        String firstToken = liberataService.getAccessToken(); //get expired token into store
        String refreshedToken = liberataService.getAccessToken();

        assertEquals("expired-token", firstToken);
        assertEquals("refreshed-token", refreshedToken);
        verify(restTemplateLaravel, times(1))
            .exchange(eq("http://laravel.com/api/auth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class));
        verify(restTemplateLaravel, times(1))
            .exchange(eq("http://laravel.com/api/auth/refresh"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class));
    }


    @Test
    void shouldRequestNewTokenWhenRefreshedTokenIsExpired() {
        String oneSecondBeforeNowUtc = LocalDateTime.now(ZoneOffset.UTC)
            .minusSeconds(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"));
        JSONObject expiredTokenBody = tokenBody("expired-token", oneSecondBeforeNowUtc);
        JSONObject refreshExpiredResponseBody = refreshedTokenBody(null, null, "Unauthenticated.");
        JSONObject newTokenBody = tokenBody("new-token", "2099-01-01T00:00:00.000000Z");

        when(restTemplateLaravel.exchange(eq("http://laravel.com/api/auth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(expiredTokenBody, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(newTokenBody, HttpStatus.OK));

        when(restTemplateLaravel.exchange(eq("http://laravel.com/api/auth/refresh"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(refreshExpiredResponseBody, HttpStatus.OK));

        String firstToken = liberataService.getAccessToken(); //gets expired token into store
        String requestedNewToken = liberataService.getAccessToken();

        assertEquals("expired-token", firstToken);
        assertEquals("new-token", requestedNewToken);
        verify(restTemplateLaravel, times(2))
            .exchange(eq("http://laravel.com/api/auth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class));
        verify(restTemplateLaravel, times(1))
            .exchange(eq("http://laravel.com/api/auth/refresh"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class));
    }

    @Test
    void shouldUseCurrentTokenWhenRefreshedTokenRequestIsNotYetExpired() {
        String oneSecondBeforeNowUtc = LocalDateTime.now(ZoneOffset.UTC)
            .minusSeconds(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"));
        JSONObject expiredTokenBody = tokenBody("expired-token", oneSecondBeforeNowUtc);
        JSONObject refreshExpiredResponseBody = refreshedTokenBody(null, null, "Token is not yet expired.");
        JSONObject newTokenBody = tokenBody("new-token", "2099-01-01T00:00:00.123456Z");

        when(restTemplateLaravel.exchange(eq("http://laravel.com/api/auth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(expiredTokenBody, HttpStatus.OK));


        when(restTemplateLaravel.exchange(eq("http://laravel.com/api/auth/refresh"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class)))
            .thenReturn(new ResponseEntity<>(refreshExpiredResponseBody, HttpStatus.OK));

        String firstToken = liberataService.getAccessToken(); //gets expired token into store
        String requestedNewToken = liberataService.getAccessToken();

        assertEquals("expired-token", firstToken);
        assertEquals("expired-token", requestedNewToken); // refresh insists that the expired token is not expired (impossible scenario?).
        verify(restTemplateLaravel, times(1))
            .exchange(eq("http://laravel.com/api/auth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class));
        verify(restTemplateLaravel, times(1))
            .exchange(eq("http://laravel.com/api/auth/refresh"), eq(HttpMethod.POST), any(HttpEntity.class), eq(JSONObject.class));
    }


    private JSONObject tokenBody(String token, String expiresAt) {
        JSONObject body = new JSONObject();
        body.put("token.plainTextToken", token);
        body.put("token.expires_at", expiresAt);
        return body;
    }

    private JSONObject refreshedTokenBody(String token, String expiresAt, String message) {
        JSONObject body = new JSONObject();
        body.put("token", token);
        body.put("expires_at", expiresAt);
        body.put("message", message);
        return body;
    }
}
