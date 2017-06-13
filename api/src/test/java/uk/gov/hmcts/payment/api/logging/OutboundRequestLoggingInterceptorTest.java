package uk.gov.hmcts.payment.api.logging;

import ch.qos.logback.classic.Logger;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static ch.qos.logback.classic.Level.INFO;
import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.apache.http.protocol.HttpCoreContext.HTTP_TARGET_HOST;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

public class OutboundRequestLoggingInterceptorTest {

    private TestAppender testAppender = new TestAppender();

    @Before
    public void addAppender() throws Exception {
        ((Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME)).addAppender(testAppender);
    }

    @After
    public void removeAppender() throws Exception {
        ((Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME)).detachAppender(testAppender);
    }

    @Test
    public void logsRequestAndResponseFields() throws Exception {
        HttpContext context = new BasicHttpContext();
        context.setAttribute(HTTP_TARGET_HOST, "http://www.google.com");

        OutboundRequestLoggingInterceptor interceptor = new OutboundRequestLoggingInterceptor(new FakeTicker(20));

        interceptor.process(new BasicHttpRequest("GET", "/something"), context);
        interceptor.process(new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("any", 0, 0), 200, "any")), context);

        testAppender.assertEvent(0, INFO, "Outbound request start",
            keyValue("method", "GET"),
            keyValue("url", "http://www.google.com/something")
        );
        testAppender.assertEvent(1, INFO, "Outbound request finish",
            keyValue("method", "GET"),
            keyValue("url", "http://www.google.com/something"),
            keyValue("responseTime", 20L),
            keyValue("responseStatus", 200)
        );
    }
}
