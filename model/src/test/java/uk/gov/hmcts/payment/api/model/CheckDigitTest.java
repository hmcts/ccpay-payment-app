package uk.gov.hmcts.payment.api.model;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.util.CheckDigitUtil;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CheckDigitTest {

    private static final String[] referenceNumbers = {"RC-1518-7779-6767-2175", "RC-1518-7780-1507-0475", "RC-1518-7780-5837-1355",
                                                            "RC-1518-7808-1810-9215", "RC-1518-7810-2720-8545"};

    @Test
    public void validatePaymentReferenceNumber_forValidCheckDigitTest() throws Exception {

        List<String> refs = Arrays.asList(referenceNumbers);

        refs.forEach(r -> {
            String[] parts = r.split("-");
            String timestamp = String.join("", parts[1], parts[2], parts[3], parts[4].substring(0, 3));
            String checkDigit = Character.toString(parts[4].charAt(3));
            CheckDigitUtil ck = new CheckDigitUtil(11);

            try {
                Assert.assertEquals(checkDigit, ck.calculate(timestamp));
            } catch (CheckDigitException e) {
                e.printStackTrace();
            }
        });
    }


}
