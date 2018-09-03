package uk.gov.hmcts.payment.functional.tokens;
import org.jboss.aerogear.security.otp.Totp;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class OneTimePasswordFactory {
    private Totp totp;
    public String validOneTimePassword(String secret) {
        totp = new Totp(secret);
        return totp.now();
    }
}
