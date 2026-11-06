package uk.gov.hmcts.payment.api.util;

public final class LogSanitizer {

    private LogSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\n', '_').replace('\r', '_');
    }
}
