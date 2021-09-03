package uk.gov.hmcts.payment.api.controllers;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.security.SecureRandom;
import java.time.LocalDateTime;

public class PaymentReference {

    private static PaymentReference obj = null;

    private PaymentReference() {
    }

    public static PaymentReference getInstance() {
        if (obj == null) {
            obj = new PaymentReference();
        }

        return obj;
    }

    public String getNext() {
        SecureRandom random = new SecureRandom();

        DateTime dateTime = new DateTime(DateTimeZone.UTC);
        long dateTimeinMillis = dateTime.getMillis() / 100;

        String nextVal = String.format("%010d", dateTimeinMillis);
        return LocalDateTime.now().getYear() + "-" + nextVal + + (random.nextInt(89) + 10);
    }

}
