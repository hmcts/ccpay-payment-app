package uk.gov.hmcts.payment.api.util;

import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class ReferenceUtil {

    private static final String PAYMENT_REF_REGEX = "(?<=\\G.{4})";

    public String getNext(String prefix) throws CheckDigitException {
        DateTime dateTime = new DateTime(DateTimeZone.UTC);
        long timeInMillis = dateTime.getMillis() / 10;

        StringBuilder sb = new StringBuilder();
        sb.append(timeInMillis);

        // append the random 3 digits
        int randomDigits = ThreadLocalRandom.current().nextInt(1000);
        sb.append(String.format("%03d", randomDigits));

        CheckDigit checkDigit = new LuhnCheckDigit();
        sb.append(checkDigit.calculate(sb.toString()));

        String[] parts = sb.toString().split(PAYMENT_REF_REGEX);

        return prefix + "-" + String.join("-", parts);
    }
}
