package uk.gov.justice.payment.api.logging;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Ticker.systemTicker;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class OutboundRequestLoggingInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(OutboundRequestLoggingInterceptor.class);
    private final Ticker ticker;

    public OutboundRequestLoggingInterceptor() {
        this(systemTicker());
    }

    OutboundRequestLoggingInterceptor(Ticker ticker) {
        this.ticker = ticker;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        logRequest(request, body);
        Stopwatch stopwatch = createStarted(ticker);
        try {
            ClientHttpResponse response = execution.execute(request, body);
            logResponse(response, stopwatch.elapsed(MILLISECONDS));
            return response;
        } catch (IOException | RuntimeException e) {
            logFailure(stopwatch.elapsed(MILLISECONDS));
            throw e;
        }
    }

    private void logRequest(HttpRequest request, byte[] body) throws UnsupportedEncodingException {
        LOG.info("Outbound request start, method: {}, uri: {}", request.getMethod(), request.getURI());

        if (LOG.isDebugEnabled()) {
            LOG.debug(new String(body, "UTF-8"));
        }
    }

    private void logResponse(ClientHttpResponse response, long elapsed) throws IOException {
        LOG.info("Outbound request finish, status: {}, elapsedTime: {}", response.getRawStatusCode(), elapsed);

        if (LOG.isDebugEnabled()) {
            LOG.debug(CharStreams.toString(new InputStreamReader(response.getBody())));
        }
    }

    private void logFailure(long elapsed) {
        LOG.error("Outbound request failed, elapsedTime: {}", elapsed);
    }
}
