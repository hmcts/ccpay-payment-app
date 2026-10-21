package uk.gov.hmcts.payment.api.mapper.liberata;

import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataIdentityResponse;
import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataTokenData;
import uk.gov.hmcts.payment.api.v1.model.exceptions.LiberataIdentityException;
import uk.gov.hmcts.payment.api.mapper.liberata.identity.AccessTokenDtoToTokenResponseMapper;

import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class AccessTokenDtoToTokenResponseMapperTest {

    @Test
    public void testToTokenResponse_mapsFieldsCorrectly() {
        val expiresAt = Instant.now().toString();
        val liberataTokenData =  new LiberataTokenData("access-token",expiresAt,1660000000L);
        val liberataIdentityResponse = new LiberataIdentityResponse("200",liberataTokenData);
        AccessTokenDtoToTokenResponseMapper mapper = new AccessTokenDtoToTokenResponseMapper();
        TokenResponse result = mapper.toTokenResponse(liberataIdentityResponse);
        Assertions.assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals(Instant.parse(expiresAt).toEpochMilli(), result.getExpiresIn());
        assertEquals(1660000000L, result.getCreatedAt());
        assertEquals(false, result.isExpired());
    }

    @Test
    public void testToTokenResponse_mapsFieldsCorrectlyAndExpired() {
        val expiresAt = "2026-07-24T13:50:18.701000Z";
        val liberataTokenData =  new LiberataTokenData("access-token",expiresAt,1660000000L);
        val liberataIdentityResponse = new LiberataIdentityResponse("200",liberataTokenData);
        AccessTokenDtoToTokenResponseMapper mapper = new AccessTokenDtoToTokenResponseMapper();
        TokenResponse result = mapper.toTokenResponse(liberataIdentityResponse);
        Assertions.assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals(Instant.parse(expiresAt).toEpochMilli(), result.getExpiresIn());
        assertEquals(1660000000L, result.getCreatedAt());
        assertEquals(true, result.isExpired());
    }

    @Test
    public void testToTokenResponse_throwsLiberataIdentityException_whenAccessTokenIsNull() {
        val expiresAt = Instant.now().toString();
        val liberataTokenData = new LiberataTokenData(null, expiresAt, 1660000000L);
        val liberataIdentityResponse = new LiberataIdentityResponse("200", liberataTokenData);
        AccessTokenDtoToTokenResponseMapper mapper = new AccessTokenDtoToTokenResponseMapper();
        Assertions.assertThrows(LiberataIdentityException.class, () -> mapper.toTokenResponse(liberataIdentityResponse));
    }

  @Test
  public void testToTokenResponse_throwsLiberataIdentityException_whenDatesAreMalformed() {
      val expiresAt = "malformed-date";
      val liberataTokenData = new LiberataTokenData("access-token", expiresAt, 1660000000L);
      val liberataIdentityResponse = new LiberataIdentityResponse("200", liberataTokenData);
      AccessTokenDtoToTokenResponseMapper mapper = new AccessTokenDtoToTokenResponseMapper();
      Assertions.assertThrows(LiberataIdentityException.class, () -> mapper.toTokenResponse(liberataIdentityResponse));
  }

    @Test
    public void testToTokenResponse_throwsLiberataIdentityExceptionIsNull() {
        val liberataTokenData = new LiberataTokenData("access-token", null, 1660000000L);
        val liberataIdentityResponse = new LiberataIdentityResponse("200", liberataTokenData);
        AccessTokenDtoToTokenResponseMapper mapper = new AccessTokenDtoToTokenResponseMapper();
        Assertions.assertThrows(LiberataIdentityException.class, () -> mapper.toTokenResponse(liberataIdentityResponse));
    }

    @Test
    public void testToTokenResponse_throwsLiberataIdentityException_whenDataAtIsNull() {
        val liberataIdentityResponse = new LiberataIdentityResponse("200", null);
        AccessTokenDtoToTokenResponseMapper mapper = new AccessTokenDtoToTokenResponseMapper();
        Assertions.assertThrows(LiberataIdentityException.class, () -> mapper.toTokenResponse(liberataIdentityResponse));
    }

}
