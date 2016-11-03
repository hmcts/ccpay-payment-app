package uk.gov.justice.payment.api.logging;

import ch.qos.logback.classic.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static ch.qos.logback.classic.Level.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.springframework.http.HttpMethod.GET;

public class InboundRequestLoggingFilterTest {

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
        new InboundRequestLoggingFilter(new FakeTicker(20)).doFilterInternal(
                requestFor(GET, "http://localhost/payments/1", "Some body"),
                responseWithStatus(404),
                mock(FilterChain.class)
        );

        testAppender.assertEvent(0, INFO, "Inbound request start, method: GET, uri: http://localhost/payments/1");
        testAppender.assertEvent(1, DEBUG, "Some body");
        testAppender.assertEvent(2, INFO, "Inbound request finish, status: 404, elapsedTime: 20");
    }

    @Test
    public void logsFailedRequest() throws Exception {
        try {
            FilterChain filterChain = mock(FilterChain.class);
            doThrow(new IOException()).when(filterChain).doFilter(any(), any());

            new InboundRequestLoggingFilter(new FakeTicker(20)).doFilterInternal(
                    requestFor(GET, "http://localhost/payments/1", "Some body"),
                    null,
                    filterChain
            );
        } catch (IOException e) {
            // expected
        }

        testAppender.assertEvent(0, INFO, "Inbound request start, method: GET, uri: http://localhost/payments/1");
        testAppender.assertEvent(1, DEBUG, "Some body");
        testAppender.assertEvent(2, ERROR, "Inbound request failed, elapsedTime: 20");
    }

    private HttpServletRequest requestFor(HttpMethod method, String url, String content) {
        MockHttpServletRequest request = new MockHttpServletRequest(method.toString(), url);
        request.setContent(content.getBytes());
        return request;
    }

    private MockHttpServletResponse responseWithStatus(int status) throws UnsupportedEncodingException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        return response;
    }

}