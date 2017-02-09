package uk.gov.justice.payment.api.logging;

import ch.qos.logback.classic.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static ch.qos.logback.classic.Level.*;
import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

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
        new OutboundRequestLoggingInterceptor(new FakeTicker(20)).intercept(
                requestFor(GET, "http://www.google.com"), withBody("Request payload"),
                (req, body) -> new MockClientHttpResponse(withBody("Response payload"), OK)
        );

        testAppender.assertEvent(0, INFO, "Outbound request start", keyValue("method", "GET"), keyValue("uri", "http://www.google.com"));
        testAppender.assertEvent(1, DEBUG, "Request payload");
        testAppender.assertEvent(2, INFO, "Outbound request finish", keyValue("responseTime", 20L), keyValue("responseStatus", 200));
        testAppender.assertEvent(3, DEBUG, "Response payload");
    }

    @Test
    public void logsFailedRequest() throws Exception {
        try {
            new OutboundRequestLoggingInterceptor(new FakeTicker(20)).intercept(
                    requestFor(GET, "http://www.google.com"), withBody("Request payload"),
                    (req, body) -> {
                        throw new IOException();
                    }
            );
        } catch (IOException e) {
            // expected
        }

        testAppender.assertEvent(0, INFO, "Outbound request start", keyValue("method", "GET"), keyValue("uri", "http://www.google.com"));
        testAppender.assertEvent(1, DEBUG, "Request payload");
        testAppender.assertEvent(2, ERROR, "Outbound request failed", keyValue("responseTime", 20L));
    }

    private MockClientHttpRequest requestFor(HttpMethod httpMethod, String uri) {
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setMethod(httpMethod);
        request.setURI(URI.create(uri));
        return request;
    }

    private byte[] withBody(String bodyPayload) {
        return bodyPayload.getBytes();
    }
}