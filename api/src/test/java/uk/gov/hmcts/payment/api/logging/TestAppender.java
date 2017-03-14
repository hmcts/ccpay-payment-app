package uk.gov.hmcts.payment.api.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;
import net.logstash.logback.argument.StructuredArgument;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class TestAppender extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> events = new ArrayList<>();

    TestAppender() {
        start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        events.add(eventObject);
    }

    public void assertEvent(int index, Level expectedLevel, String expectedMessage, StructuredArgument... arguments) {
        assertThat(event(index).getLevel()).isEqualTo(expectedLevel);
        assertThat(event(index).getFormattedMessage()).isEqualTo(expectedMessage);
        assertThat(firstNonNull(event(index).getArgumentArray(), new StructuredArgument[0])).isEqualTo(arguments);
    }

    public ILoggingEvent event(int index) {
        return events.get(index);
    }
}
