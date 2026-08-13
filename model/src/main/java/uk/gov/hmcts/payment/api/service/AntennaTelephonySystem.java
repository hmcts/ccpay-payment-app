package uk.gov.hmcts.payment.api.service;

import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@SuperBuilder
@NoArgsConstructor
@Component
public class AntennaTelephonySystem extends TelephonySystem {

    public static final String TELEPHONY_SYSTEM_NAME = "antenna";
    private static final String SYSTEM_NAME = TELEPHONY_SYSTEM_NAME;
}
