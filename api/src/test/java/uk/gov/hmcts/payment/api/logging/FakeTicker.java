package uk.gov.hmcts.payment.api.logging;

import com.google.common.base.Ticker;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class FakeTicker extends Ticker {
    private long elapsedNanos = 0;
    private final long increment;

    FakeTicker(int increment) {
        this.increment = MILLISECONDS.toNanos(increment);
    }

    @Override
    public long read() {
        elapsedNanos += increment;
        return elapsedNanos;
    }
}
