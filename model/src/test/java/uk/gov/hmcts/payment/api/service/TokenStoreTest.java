package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenStoreTest {

    private final TokenStore tokenStore = new TokenStore();

    @Test
    void shouldReturnStoredToken() {
        TokenState tokenState = new TokenState("access-token", Instant.parse("2099-01-01T00:00:00Z"));

        tokenStore.set(tokenState);

        assertEquals(tokenState, tokenStore.get());
    }

    @Test
    void shouldReplaceStoredToken() {
        TokenState originalToken = new TokenState("original-token", Instant.parse("2099-01-01T00:00:00Z"));
        TokenState replacementToken = new TokenState("replacement-token", Instant.parse("2099-01-02T00:00:00Z"));

        tokenStore.set(originalToken);
        tokenStore.set(replacementToken);

        assertEquals(replacementToken, tokenStore.get());
    }

    @Test
    void shouldClearStoredTokenWhenNullIsSet() {
        TokenState tokenState = new TokenState("access-token", Instant.parse("2099-01-01T00:00:00Z"));

        tokenStore.set(tokenState);
        tokenStore.set(null);

        assertNull(tokenStore.get());
    }
}
