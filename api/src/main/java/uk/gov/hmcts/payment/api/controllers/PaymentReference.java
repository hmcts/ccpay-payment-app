package uk.gov.hmcts.payment.api.controllers;

import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentReference {

    private AtomicInteger atomicInteger;

    private static PaymentReference obj = null;

    private PaymentReference(int initialValue) {
        this.atomicInteger = new AtomicInteger(initialValue);
    }

    public static PaymentReference getInstance() {
        // First day of the CURRENT YEAR
        LocalDate date = LocalDate.of(LocalDateTime.now().getYear(), Month.JANUARY, 01);

        LocalDate now = LocalDate.now();
        if ( obj == null || now.equals(date.with(TemporalAdjusters.firstDayOfYear())) ) {
            obj = new PaymentReference(1);
        }

        return obj;
    }

    public String getNext() {
        SecureRandom random = new SecureRandom();
        String nextVal = String.format("%08d", random.nextInt(100000000));
        return LocalDateTime.now().getYear() + "-" + nextVal;
    }

}
