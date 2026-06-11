package uk.gov.hmcts.payment.api.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class TokenStore {

    private final AtomicReference<TokenState> tokenRef = new AtomicReference<>();

    public TokenState get() {
        return tokenRef.get();
    }

    public void set(TokenState token) {
        tokenRef.set(token);
    }
}
