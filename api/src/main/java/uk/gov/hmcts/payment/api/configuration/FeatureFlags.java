package uk.gov.hmcts.payment.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class FeatureFlags {

    @Value("${feature.payments.search}")
    private boolean paymentSearch = false;

    @Value("${feature.payments.service.callback}")
    private boolean paymentServiceCallback = false;

    @Value("${feature.credit.account.payment.liberata.check}")
    private boolean creditAccountPaymentLiberataCheck = true;

    @Value("${feature.check.liberata.account.for.all.services}")
    private boolean checkLiberataAccountForAllServices = false;

    @Value("${feature.duplicate.payment.check}")
    private boolean duplicatePaymentCheck = true;

    @Value("${feature.case.reference.validation}")
    private boolean caseRefValidation = false;

    @Value("${feature.bulk.scan.payments.check}")
    private boolean isBulkScanPaymentsEnabled = true;

    @Value("${feature.bulk.scan.payments.check.pay.bubble}")
    private boolean isBulkScanPayBubbleEnabled = true;

    @Value("${feature.payments.cancel}")
    private boolean paymentCancel = true;

    @Value("${feature.discontinued.fees}")
    private boolean discontinuedFeesFeatureEnabled = true;

    private final Map<String, Boolean> featureFlags;

    public FeatureFlags() {
        featureFlags = new HashMap<>();
        featureFlags.put("payment_search", paymentSearch);
        featureFlags.put("payment_service_callback", paymentServiceCallback);
        featureFlags.put("credit_account_payment_liberata_check", creditAccountPaymentLiberataCheck);
        featureFlags.put("check_liberata_account_for_all_services", checkLiberataAccountForAllServices);
        featureFlags.put("duplicate_payment_check", duplicatePaymentCheck);
        featureFlags.put("case_ref_validation", caseRefValidation);
        featureFlags.put("is_bulk_scan_payments_enabled", isBulkScanPaymentsEnabled);
        featureFlags.put("is_bulk_scan_pay_bubble_enabled", isBulkScanPayBubbleEnabled);
        featureFlags.put("payment_cancel", paymentCancel);
        featureFlags.put("discontinued_fees_feature_enabled", discontinuedFeesFeatureEnabled);
    }

    public boolean check(String flag) {
        return featureFlags.get(flag);
    }

}
