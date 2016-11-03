package uk.gov.justice.payment.api.logging;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Ticker.systemTicker;
import static java.util.concurrent.TimeUnit.MILLISECONDS;


@Order(value = Ordered.HIGHEST_PRECEDENCE)
@Component
public class InboundRequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(InboundRequestLoggingFilter.class);

    private final Ticker ticker;

    public InboundRequestLoggingFilter() {
        this(systemTicker());
    }

    InboundRequestLoggingFilter(Ticker ticker) {
        this.ticker = ticker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Stopwatch stopwatch = createStarted(ticker);

        try {
            filterChain.doFilter(logRequest(request), response);
            logResponse(response, stopwatch.elapsed(MILLISECONDS));
        } catch (ServletException | IOException | RuntimeException e) {
            logFailure(stopwatch.elapsed(MILLISECONDS));
            throw e;
        }
    }

    private HttpServletRequest logRequest(HttpServletRequest request) throws IOException {
        LOG.info("Inbound request start, method: {}, uri: {}", request.getMethod(), request.getRequestURI());

        if (LOG.isDebugEnabled()) {
            ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
            LOG.debug(IOUtils.toString(cachedRequest.getInputStream()));
            return cachedRequest;
        } else {
            return request;
        }
    }

    private void logResponse(HttpServletResponse response, long elapsed) {
        LOG.info("Inbound request finish, status: {}, elapsedTime: {}", response.getStatus(), elapsed);
    }

    private void logFailure(long elapsed) {
        LOG.error("Inbound request failed, elapsedTime: {}", elapsed);
    }
}
