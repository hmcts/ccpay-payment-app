package uk.gov.hmcts.payment.api.v1.model.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.logstash.logback.marker.MapEntriesAppendingMarker;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAppender extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> events = new ArrayList<>();

    public TestAppender() {
        start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        events.add(eventObject);
    }

    public void assertEvent(int index, Level expectedLevel, String expectedMessage, Map<?, ?> arguments) {
        assertThat(event(index).getLevel()).isEqualTo(expectedLevel);
        assertThat(event(index).getFormattedMessage()).isEqualTo(expectedMessage);
        assertThat(event(index).getArgumentArray()[0]).isEqualTo(new MapEntriesAppendingMarker(arguments));
    }

    public ILoggingEvent event(int index) {
        return events.get(index);
    }
}
