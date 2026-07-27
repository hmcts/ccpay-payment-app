package uk.gov.hmcts.payment.api.mapper.liberata.identity;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataIdentityResponse;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import uk.gov.hmcts.payment.api.v1.model.exceptions.LiberataIdentityException;

import java.time.Instant;
import java.util.Objects;

@Component
public final class AccessTokenDtoToTokenResponseMapper {

    public TokenResponse toTokenResponse(LiberataIdentityResponse liberataIdentityResponse) {
        try {
            if (
                Objects.nonNull(liberataIdentityResponse.getToken().getAccessToken()) &&
                Objects.nonNull(liberataIdentityResponse.getToken().getAccessToken().getCreatedAt()) &&
                Objects.nonNull(liberataIdentityResponse.getToken().getAccessToken().getExpiresAt())
            ) {
                final Long createdAtMillis = Instant.parse(liberataIdentityResponse.getToken().getAccessToken().getCreatedAt()).toEpochMilli();
                final Long expiresAtMillis = Instant.parse(liberataIdentityResponse.getToken().getAccessToken().getExpiresAt()).toEpochMilli();
                return new TokenResponse(liberataIdentityResponse.getToken().getPlainTextToken(), expiresAtMillis, createdAtMillis);
            } else {
                throw new LiberataIdentityException("Failed to parse createdAt or expiresAt from LiberataIdentityResponse: " + liberataIdentityResponse);
            }
        } catch (Exception exception) {
            throw new LiberataIdentityException("Failed to parse createdAt or expiresAt from LiberataIdentityResponse: " + liberataIdentityResponse,exception);
        }
    }
}

