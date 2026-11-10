package uk.gov.hmcts.payment.api.util;

import java.util.regex.Pattern;

public final class LogSanitizer {

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cntrl}\\u2028\\u2029]");

    private LogSanitizer() {
    }

    public static String sanitize(String value) {
        return value == null ? null : CONTROL_CHARACTERS.matcher(value).replaceAll("?");
    }
}
