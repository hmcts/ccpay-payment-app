package uk.gov.hmcts.payment.api.logging;

import ch.qos.logback.classic.Logger;
import java.net.URI;
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
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static ch.qos.logback.classic.Level.INFO;
import static net.logstash.logback.argument.StructuredArguments.keyValue;
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

        OutboundRequestLoggingInterceptor interceptor = new OutboundRequestLoggingInterceptor(new FakeTicker(20));

        interceptor.process(new BasicHttpRequest("GET", "http://www.google.com"), context);
        interceptor.process(new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("any", 0, 0), 200, "any")), context);

        testAppender.assertEvent(0, INFO, "Outbound request start", keyValue("method", "GET"), keyValue("uri", "http://www.google.com"));
        testAppender.assertEvent(1, INFO, "Outbound request finish", keyValue("responseTime", 20L), keyValue("responseStatus", 200));
    }

    private MockClientHttpRequest requestFor(HttpMethod httpMethod, String uri) {
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setMethod(httpMethod);
        request.setURI(URI.create(uri));
        return request;
    }
}
