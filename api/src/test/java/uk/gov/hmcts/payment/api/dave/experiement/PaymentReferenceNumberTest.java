package uk.gov.hmcts.payment.api.dave.experiement;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

@Slf4j
public class PaymentReferenceNumberTest {

    private static final int TEST_ARRAY_COUNT = 2000000;

    @Test
    public void existingServiceRequestGenerationTest() throws Exception {

        String paymentReference = getNext2();

        log.info("Payment Reference: {}", paymentReference);

        ArrayList<String> paymentReferences = new ArrayList<>();

        for (int i = 0; i < TEST_ARRAY_COUNT; i++) {
            paymentReferences.add(getNext2());
        }

        // Show first 100 sorted payment references
        Collections.sort(paymentReferences);
        for (int i = 0; i < 100; i++) {
            log.info("Payment Reference: {}", paymentReferences.get(i));
        }
        Integer uniquePaymentReferences = (int) paymentReferences.stream().distinct().count();
        log.info("Unique Payment References: {} out of {} = {}%", uniquePaymentReferences, TEST_ARRAY_COUNT, uniquePaymentReferences * 100 / TEST_ARRAY_COUNT);

        assert(true);
    }

    public String getNext() {
        SecureRandom random = new SecureRandom();

        DateTime dateTime = new DateTime(DateTimeZone.UTC);
        long dateTimeinMillis = dateTime.getMillis() / 100;

//        log.info("dateTimeinMillis: {}", dateTimeinMillis);
//        log.info("random.nextInt(89)+10: {}", random.nextInt(89)+10);
//        log.info("LocalDateTime.now().getYear(): {}", LocalDateTime.now().getYear());
//        log.info("String.format(\"%010d\", dateTimeinMillis): {}", String.format("%010d", dateTimeinMillis));

        String nextVal = String.format("%010d", dateTimeinMillis);
        return LocalDateTime.now().getYear() + "-" + nextVal + + (random.nextInt(89)+10);
    }

    public String getNext2() {
        SecureRandom random = new SecureRandom();

        //String nextVal = String.format("%010d", this.getCurrentTimeInEpochInNanoSecond());
        //return LocalDateTime.now().getYear() + "-" + nextVal + (random.nextInt(89)+10);
        return LocalDateTime.now().getYear() + "-" + this.getCurrentTimeInEpochInNanoSecond();
    }

    // getCurrent DateTime in Epoch till NanoSecond
    public String getCurrentTimeInEpochInNanoSecond(){
        Instant instant = Instant.now();
        long epochInNanoStr = instant.toEpochMilli();
        return String.valueOf(epochInNanoStr);
    }

}


