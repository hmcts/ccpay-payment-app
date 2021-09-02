package uk.gov.hmcts.payment.api.scheduler;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Component
public class Clock {

    public Date getYesterdayDate() {
        return Date.from(LocalDate.now().minusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public Date getTodayDate() {
        return Date.from(LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public Date atStartOfDay(String dateString, DateTimeFormatter formatter) {
        return Date.from(LocalDate.parse(dateString, formatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public Date atEndOfDay(String dateString, DateTimeFormatter formatter) {
        return Date.from(LocalDate.parse(dateString, formatter).atStartOfDay().plusDays(1).minusSeconds(1)
            .atZone(ZoneId.systemDefault()).toInstant());
    }
}
