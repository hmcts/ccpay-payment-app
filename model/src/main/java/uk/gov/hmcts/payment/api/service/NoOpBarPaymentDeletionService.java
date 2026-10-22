package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.util.LogSanitizer;

@Service
@ConditionalOnProperty(name = "bar.datasource.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpBarPaymentDeletionService implements BarPaymentDeletionService {

    private static final Logger LOG = LoggerFactory.getLogger(NoOpBarPaymentDeletionService.class);

    @Override
    public void deleteBarPaymentsByCcdCaseNumber(String ccdCaseNumber) {
        LOG.warn(
            "Digital BAR datasource is not configured; skipping BAR payment deletion for case reference: {}",
            LogSanitizer.sanitize(ccdCaseNumber)
        );
    }
}
