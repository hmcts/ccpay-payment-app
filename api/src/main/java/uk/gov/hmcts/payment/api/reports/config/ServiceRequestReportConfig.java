package uk.gov.hmcts.payment.api.reports.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.DuplicateServiceRequestKey;

import java.util.StringJoiner;
@Component
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ServiceRequestReportConfig {

    public static final String DUPLICATE_SR_HEADER = "fees, service request count, ccd, service";

    public static final String DUPLICATE_SR_CSV_FILE_PREFIX = "hmcts_duplicate_service_requests";

    @Value("${service.request.email.from:dummy}")
    private String from;
    @Value("${service.request.email.to:dummy}")
    private String[] to;
    @Value("${service.request.email.subject:dummy}")
    private String subject;
    @Value("${service.request.email.message:dummy}")
    private String message;
    @Value("${service.request.scheduler.enabled:false}")
    private boolean enabled;
    public String getDuplicateSRCsvRecord(DuplicateServiceRequestKey duplicateServiceRequestKey,
                                          Integer duplicateServiceRequestCount) {
        StringJoiner result = new StringJoiner("\n");
        StringJoiner sb = new StringJoiner(",")
            .add(duplicateServiceRequestKey.getFeeCodes())
            .add(duplicateServiceRequestCount.toString())
            .add(duplicateServiceRequestKey.getCcd())
            .add(duplicateServiceRequestKey.getService());
        result.add(sb.toString());
        return result.toString();
    }
}
