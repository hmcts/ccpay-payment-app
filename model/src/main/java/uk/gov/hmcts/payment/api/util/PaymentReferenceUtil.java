package uk.gov.hmcts.payment.api.util;

import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PaymentReferenceUtil {

    private final static String PAYMENT_REF_REGEX = "(?<=\\G.{4})";

    public String getNext() throws CheckDigitException {
        DateTime dateTime = new DateTime(DateTimeZone.UTC);
        long timeInMillis = dateTime.getMillis()/100;

        StringBuffer sb = new StringBuffer();
        sb.append(timeInMillis);

        // append the random 4 characters
        SecureRandom random = new SecureRandom();
        sb.append(String.format("%04d", random.nextInt(10000)));

        CheckDigit checkDigit = new LuhnCheckDigit();
        sb.append(checkDigit.calculate(sb.toString()));

        String[] parts = sb.toString().split(PAYMENT_REF_REGEX);

        return "RC-" + String.join("-", parts);
    }
}
