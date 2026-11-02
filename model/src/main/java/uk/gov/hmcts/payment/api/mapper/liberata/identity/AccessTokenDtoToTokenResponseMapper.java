package uk.gov.hmcts.payment.api.mapper.liberata.identity;

import lombok.val;
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
                Objects.nonNull(liberataIdentityResponse.getData() )&&
                Objects.nonNull(liberataIdentityResponse.getData().getToken()) &&
                Objects.nonNull(liberataIdentityResponse.getData().getExpiresAt() )
            ) {

                val createdAtMillis = liberataIdentityResponse.getData().getCreatedAt();
                val expiresAtMillis = Instant.parse(liberataIdentityResponse.getData().getExpiresAt()).toEpochMilli();
                return new TokenResponse(liberataIdentityResponse.getData().getToken(), expiresAtMillis, createdAtMillis);
            } else {
                throw new LiberataIdentityException("Failed to parse createdAt or expiresAt from LiberataIdentityResponse: " + liberataIdentityResponse);
            }
        } catch (Exception exception) {
            throw new LiberataIdentityException("Failed to parse createdAt or expiresAt from LiberataIdentityResponse: " + liberataIdentityResponse,exception);
        }
    }
}

