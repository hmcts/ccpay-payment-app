package uk.gov.hmcts.payment.functional.s2s;

import org.jboss.aerogear.security.otp.Totp;
import org.springframework.stereotype.Component;

@Component
public class OneTimePasswordFactory {
    private Totp totp;

    public String validOneTimePassword(String secret) {
        totp = new Totp(secret);
        return totp.now();
    }
}
