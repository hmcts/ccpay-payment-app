package uk.gov.hmcts.payment.api.scheduler;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

public class ClockTest {

    private Clock clock;

    @Before
    public void setUp() {
        clock = new Clock();
    }

    @After
    public void tearDown() {
        clock = null;
    }

    @Test
    public void testGetYesterdayDate() {
        Date yesterday = clock.getYesterdayDate();
        assertThat(yesterday).isBefore(DateTime.now().toDate());
    }

    @Test
    public void testGetTodayDate() {
        Date today = clock.getTodayDate();
        assertThat(today).isInSameDayAs(DateTime.now().toString());
    }

    @Test
    public void testAtStartOfDay() {
        Date timeAtStartOfDay = DateTime.now().withTimeAtStartOfDay().toDate();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        Date result = clock.atStartOfDay(LocalDate.now().toString(), formatter);

        assertThat(result).isEqualTo(timeAtStartOfDay);
    }

    @Test
    public void testAtEndOfDay() {
        Date timeAtEndOfDay = DateTime.now().withTime(23, 59, 59, 000).toDate();
        System.out.println("timeAtEndOfDay : " + timeAtEndOfDay);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        Date result = clock.atEndOfDay(LocalDate.now().toString(), formatter);

        assertThat(result).isEqualTo(timeAtEndOfDay);
    }
}
