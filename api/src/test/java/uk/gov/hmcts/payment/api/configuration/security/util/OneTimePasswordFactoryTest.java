package uk.gov.hmcts.payment.api.configuration.security.util;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class OneTimePasswordFactoryTest {

    @Mock
    private Clock clock;

    private Totp totp;

    private OneTimePasswordFactory oneTimePasswordFactory;

    private String SHARED_SECRET = "AAAAAAAAAAAAAAAA";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        oneTimePasswordFactory = new OneTimePasswordFactory();
    }

    @Test
    public void testOtpNowShouldBeValid() {
        totp = new Totp(SHARED_SECRET);
        String otp = oneTimePasswordFactory.validOneTimePassword(SHARED_SECRET);
        assertTrue(totp.verify(otp));
    }

    @Test
    public void testOtpAfter59theSecondsShouldBeValid() {
        when(clock.getCurrentInterval()).thenReturn(addElapsedTime(59));
        totp = new Totp(SHARED_SECRET, clock);
        String otp = oneTimePasswordFactory.validOneTimePassword(SHARED_SECRET);
        assertTrue(totp.verify(otp));
    }

    @Test
    public void testOtpAfterOneMinuteShouldBeInvalid() {
        when(clock.getCurrentInterval()).thenReturn((DateTime.now().plusMinutes(1).getMillis()/1000)/30);
        totp = new Totp(SHARED_SECRET, clock);
        String otp = oneTimePasswordFactory.validOneTimePassword(SHARED_SECRET);
        assertFalse(totp.verify(otp));
    }

    private long addElapsedTime(int seconds) {
        long millis = DateTime.now().plusMinutes(1).minusSeconds(seconds).getMillis() / 1000;
        return millis / 30;
    }

}
