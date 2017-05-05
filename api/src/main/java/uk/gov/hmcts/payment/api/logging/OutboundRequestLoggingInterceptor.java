package uk.gov.hmcts.payment.api.logging;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Ticker.systemTicker;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

public class OutboundRequestLoggingInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(OutboundRequestLoggingInterceptor.class);
    private final Ticker ticker;

    public OutboundRequestLoggingInterceptor() {
        this(systemTicker());
    }

    OutboundRequestLoggingInterceptor(Ticker ticker) {
        this.ticker = ticker;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        Stopwatch stopwatch = createStarted(ticker);
        context.setAttribute("stopwatch", stopwatch);

        RequestLine requestLine = request.getRequestLine();
        LOG.info("Outbound request start", keyValue("method", requestLine.getMethod()), keyValue("uri", requestLine.getUri()));
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        Stopwatch stopwatch = (Stopwatch) context.getAttribute("stopwatch");
        LOG.info("Outbound request finish", keyValue("responseTime", stopwatch.elapsed(MILLISECONDS)), keyValue("responseStatus", response.getStatusLine().getStatusCode()));
    }
}
