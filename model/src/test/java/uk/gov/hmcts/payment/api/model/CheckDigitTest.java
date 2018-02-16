package uk.gov.hmcts.payment.api.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.SecureRandom;

@RunWith(MockitoJUnitRunner.class)
public class CheckDigitTest {

    private static final String[] referenceNumbers = {"RC-1518-7037-2684-7314", "RC-1518-7031-9753-3696", "RC-1518-7032-2860-7136"};

    @Test
    public void validCheckDigit() throws Exception {
        DateTime dateTime = new DateTime(DateTimeZone.UTC);
        long timeInMillis = dateTime.getMillis()/100;

        StringBuffer sb = new StringBuffer();
        sb.append(timeInMillis);

        // append the random 4 characters
        SecureRandom random = new SecureRandom();
        sb.append(String.format("%04d", random.nextInt(10000)));

        //System.out.println(Mod11Ck.calc(timestamp));
    }


}
