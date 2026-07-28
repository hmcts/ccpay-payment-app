package uk.gov.hmcts.payment.api.mapper.liberata.identity;

import org.junit.jupiter.api.Test;

import uk.gov.hmcts.payment.api.dto.liberata.identity.AccessTokenDto;
import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataIdentityResponse;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenDto;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import uk.gov.hmcts.payment.api.v1.model.exceptions.LiberataIdentityException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccessTokenDtoToTokenResponseMapperTest {

    @Test
    public void testToTokenResponse_mapsFieldsCorrectly() {
        String createdAt = "2026-07-24T13:50:18.701000Z";
        String expiresAt = "2026-08-23T13:50:18.699000Z";
        String plainTextToken = "15|qtvgH4z8A0QJGUJCMQeBeMiYCwl6JxywfJ8ANOTf6d8c7ab0";

        AccessTokenDto accessTokenDto = new AccessTokenDto(
            "pba-api-token",
            List.of("*"),
            expiresAt,
            1,
            "App\\Models\\User",
            "2026-07-24T13:50:18.701000Z",
            createdAt,
            15
        );

        TokenDto tokenDto = new TokenDto(accessTokenDto, plainTextToken);
        LiberataIdentityResponse liberataIdentityResponse = new LiberataIdentityResponse(tokenDto);

        AccessTokenDtoToTokenResponseMapper mapper = new AccessTokenDtoToTokenResponseMapper();
        TokenResponse response = mapper.toTokenResponse(liberataIdentityResponse);

        long expectedCreatedMillis = Instant.parse(createdAt).toEpochMilli();
        long expectedExpiresMillis = Instant.parse(expiresAt).toEpochMilli();

        assertEquals(plainTextToken, response.getAccessToken());

        assertEquals(expectedExpiresMillis, response.getExpiresIn());
        assertEquals(expectedCreatedMillis, response.getCreatedAt());
    }

    @Test
    public void testToTokenResponse_throwsLiberataIdentityException_whenAccessTokenIsNull() {
        // accessToken is null -> should trigger the explicit LiberataIdentityException from the validation branch
        String plainTextToken = "plain-token";
        TokenDto tokenDto = new TokenDto(null, plainTextToken);
        LiberataIdentityResponse liberataIdentityResponse = new LiberataIdentityResponse(tokenDto);

        AccessTokenDtoToTokenResponseMapper mapper = new AccessTokenDtoToTokenResponseMapper();

        assertThrows(LiberataIdentityException.class, () -> mapper.toTokenResponse(liberataIdentityResponse));
    }

    @Test
    public void testToTokenResponse_throwsLiberataIdentityException_whenDatesAreMalformed() {
        // malformed dates should cause Instant.parse to throw and the mapper to rethrow LiberataIdentityException with the cause
        String createdAt = "not-a-valid-date";
        String expiresAt = "also-not-valid";
        String plainTextToken = "plain-token";

        AccessTokenDto accessTokenDto = new AccessTokenDto(
            "pba-api-token",
            List.of("*"),
            expiresAt,
            1,
            "App\\Models\\User",
            "2026-07-24T13:50:18.701000Z",
            createdAt,
            15
        );

        TokenDto tokenDto = new TokenDto(accessTokenDto, plainTextToken);
        LiberataIdentityResponse liberataIdentityResponse = new LiberataIdentityResponse(tokenDto);

        AccessTokenDtoToTokenResponseMapper mapper = new AccessTokenDtoToTokenResponseMapper();

        LiberataIdentityException ex = assertThrows(LiberataIdentityException.class, () -> mapper.toTokenResponse(liberataIdentityResponse));
        // ensure the root cause is a DateTimeParseException from Instant.parse
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof DateTimeParseException);
    }

}
