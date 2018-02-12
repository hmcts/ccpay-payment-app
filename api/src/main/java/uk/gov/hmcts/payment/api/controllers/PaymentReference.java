package uk.gov.hmcts.payment.api.controllers;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentReference {

    private static PaymentReference obj = null;

    private PaymentReference() {
    }

    public static PaymentReference getInstance() {
        if ( obj == null ) {
            obj = new PaymentReference();
        }

        return obj;
    }

    public String getNext() {
        SecureRandom random = new SecureRandom();

        DateTime dateTime = new DateTime(DateTimeZone.UTC);
        long dateTimeinMillis = dateTime.getMillis()/100;

        String nextVal = String.format("%010d", dateTimeinMillis);
        return LocalDateTime.now().getYear() + "-" + nextVal;
    }

}
