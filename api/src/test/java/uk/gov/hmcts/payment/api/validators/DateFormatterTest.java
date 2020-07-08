package uk.gov.hmcts.payment.api.validators;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

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
