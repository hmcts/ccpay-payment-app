package uk.gov.hmcts.payment.api.controllers;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentReference {

    private AtomicInteger atomicInteger;

    private static PaymentReference obj = null;

    private PaymentReference(int initialValue) {
        this.atomicInteger = new AtomicInteger(initialValue);
    }

    public static PaymentReference getInstance() {
        if (obj == null) {
            obj = new PaymentReference(1);
        }

        return obj;
    }

    public String getNext() {
        String nextVal = StringUtils.leftPad(Integer.toString(atomicInteger.getAndIncrement()), 8, '0');
        return LocalDateTime.now().getYear() + "-" + nextVal;
    }

}
