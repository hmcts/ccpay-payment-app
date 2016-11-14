package uk.gov.justice.payment.api.logging;

import ch.qos.logback.classic.Logger;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import static ch.qos.logback.classic.Level.*;
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
        MockFilterChain mockFilterChain = new MockFilterChain(new GenericServlet() {
            @Override
            public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
                CharStreams.toString(new InputStreamReader(servletRequest.getInputStream(), Charsets.UTF_8));
            }
        });

        new InboundRequestLoggingFilter(new FakeTicker(20)).doFilterInternal(
                requestFor(GET, "http://localhost/payments/1", "Some body"),
                responseWithStatus(404),
                mockFilterChain
        );

        testAppender.assertEvent(0, INFO, "Inbound request start, method: GET, uri: http://localhost/payments/1");
        testAppender.assertEvent(1, DEBUG, "Some body");
        testAppender.assertEvent(2, INFO, "Inbound request finish, status: 404, elapsedTime: 20");
    }

    @Test
    public void logsFailedRequest() throws Exception {
        try {
            MockFilterChain mockFilterChain = new MockFilterChain(new GenericServlet() {
                @Override
                public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
                    CharStreams.toString(new InputStreamReader(servletRequest.getInputStream(), Charsets.UTF_8));
                    throw new IOException("expected");
                }
            });

            new InboundRequestLoggingFilter(new FakeTicker(20)).doFilterInternal(
                    requestFor(GET, "http://localhost/payments/1", "Some body"),
                    responseWithStatus(200),
                    mockFilterChain
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