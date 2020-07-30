package uk.gov.hmcts.payment.api.util;

import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.payment.api.util.DateFormatter;

import java.text.ParseException;
import java.util.Date;

public class DateFormatterTest {

    DateFormatter dateFormatter = new DateFormatter();

    @Test
    public void shouldReturnNoErrors() {
        // no exception expected, none thrown: passes.
        Date formattedDate = dateFormatter.parseDate("07.07.2020");
        Assert.assertNotNull(formattedDate);
    }

    @Test
    public void shouldReturnErrorsWhenFormatIsNotCorrect() throws ParseException {
        // no exception expected, none thrown: passes.
        Date formattedDate = dateFormatter.parseDate("07/07.2020");
        Assert.assertNull(formattedDate);
    }
}
