package uk.gov.hmcts.payment.api.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DuplicateServiceRequestKey {
    String feeCodes;
    String ccd;
    String service;
}
