package uk.gov.hmcts.payment.api.reports.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.model.DuplicateServiceRequestDto;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.StringJoiner;
import java.util.TimeZone;

@Component
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DuplicateServiceRequestReportConfig  {

    public static final String HEADER = "ccd, count";

    public static final String CSV_FILE_PREFIX = "hmcts_duplicate_service_requests";

    @Value("${duplicatesr.email.from:dummy}")
    private String from;
    @Value("${duplicatesr.email.to:dummy}")
    private String[] to;
    @Value("${duplicatesr.email.subject:dummy}")
    private String subject;
    @Value("${duplicatesr.email.message:dummy}")
    private String message;
    @Value("${duplicatesr.scheduler.enabled:false}")
    private boolean enabled;

    public String getCsvRecord(DuplicateServiceRequestDto duplicateServiceRequestDto){
        StringJoiner result = new StringJoiner("\n");

            StringJoiner sb = new StringJoiner(",")
                .add(duplicateServiceRequestDto.getCcd_case_number())
                .add(Integer.toString(duplicateServiceRequestDto.getCount()));
            result.add(sb.toString());

        return result.toString();
    }

}
