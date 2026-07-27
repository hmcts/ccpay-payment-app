package uk.gov.hmcts.payment.api.mapper.liberata.identity;

import org.junit.jupiter.api.Test;

import uk.gov.hmcts.payment.api.dto.liberata.identity.AccessTokenDto;
import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataIdentityResponse;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenDto;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

