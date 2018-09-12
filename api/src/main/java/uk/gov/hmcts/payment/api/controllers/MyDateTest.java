package uk.gov.hmcts.payment.api.controllers;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

import java.sql.Date;
import java.time.ZoneId;


public class MyDateTest {

    public static void main(String[] args) {
        String startDateStr = "2018-09-12";
        String endDateStr = "2018-09-12";

        DateTimeParser[] parsers = {
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd").getParser(),
        };
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, parsers).toFormatter();

        LocalDateTime localStartDateTime = formatter.parseLocalDateTime(startDateStr); //DateTime.parse(startDateStr).toDateTimeISO().toLocalDateTime();
        System.out.println("LocalDateTime start: " + localStartDateTime);
        System.out.println("Java startDateTime: " + localStartDateTime.toDateTime());
        System.out.println("Java startDate: " + localStartDateTime.toDate());

        LocalDateTime localEndDateTime = formatter.parseLocalDateTime(endDateStr).plusDays(1).minusSeconds(1);
        System.out.println("LocalDateTime start: " + localEndDateTime);
        System.out.println("Java endDateTime: " + localEndDateTime.toDateTime());
        System.out.println("Java endDate: " + localEndDateTime.toDate(DateTimeZone.UTC.toTimeZone()));




    }
}
