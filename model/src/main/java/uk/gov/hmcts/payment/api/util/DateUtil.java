package uk.gov.hmcts.payment.api.util;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.springframework.stereotype.Component;

@Component
public class DateUtil {

    DateTimeParser[] ISO_DATE_TIME_PARSERS = {
        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").getParser(),
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").getParser(),
        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").getParser(),
        DateTimeFormat.forPattern("yyyy-MM-dd HH").getParser(),
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH").getParser(),
        DateTimeFormat.forPattern("yyyy-MM-dd").getParser(),
    };

    public DateTimeFormatter getIsoDateTimeFormatter() {
        return new DateTimeFormatterBuilder().append(null, ISO_DATE_TIME_PARSERS).toFormatter();
    }

}
