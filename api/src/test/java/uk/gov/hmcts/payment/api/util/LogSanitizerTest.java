package uk.gov.hmcts.payment.api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogSanitizerTest {

    @Test
    void preservesTextWithoutControlCharacters() {
        assertEquals("case-type-123", LogSanitizer.sanitize("case-type-123"));
    }

    @Test
    void replacesControlCharactersAndUnicodeLineSeparators() {
        assertEquals("A??????B", LogSanitizer.sanitize("A\r\n\t\u0085\u2028\u2029B"));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(LogSanitizer.sanitize(null));
    }
}
