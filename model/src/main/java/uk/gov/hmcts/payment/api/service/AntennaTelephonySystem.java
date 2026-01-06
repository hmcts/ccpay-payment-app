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

    private final String systemName = TELEPHONY_SYSTEM_NAME;

    @Value("${pci-pal.antenna.grant.type}")
    private String antennaGrantType;

    @Value("${pci-pal.antenna.tenant.name}")
    private String antennaTenantName;

    @Value("${pci-pal.antenna.client.id}")
    private String antennaClientId;

    @Value("${pci-pal.antenna.client.secret}")
    private String antennaClientSecret;

    @Value("${pci-pal.antenna.get.tokens.url}")
    private String antennaTokensURL;

    @Value("${pci-pal.antenna.launch.url}")
    private String antennaLaunchURL;

    @Value("${pci-pal.antenna.view.id.url}")
    private String antennaViewIdURL;

    @Value("${pci-pal.antenna.strategic.flow.id}")
    private String antennaStrategicFlowId;

    @Value("${pci-pal.antenna.probate.flow.id}")
    private String antennaProbateFlowId;

    @Value("${pci-pal.antenna.divorce.flow.id}")
    private String antennaDivorceFlowId;

    @Value("${pci-pal.antenna.prl.flow.id}")
    private String antennaPrlFlowId;

    @Value("${pci-pal.antenna.iac.flow.id}")
    private String antennaIacFlowId;

    @Override
    public String getSystemName() {
        return systemName;
    }
    @Override
    public String getGrantType() {
        return antennaGrantType;
    }
    @Override
    public String getTenantName() {
        return antennaTenantName;
    }
    @Override
    public String getClientId() {
        return antennaClientId;
    }
    @Override
    public String getClientSecret() {
        return antennaClientSecret;
    }
    @Override
    public String getTokensURL() {
        return antennaTokensURL;
    }
    @Override
    public String getLaunchURL() {
        return antennaLaunchURL;
    }
    @Override
    public String getViewIdURL() {
        return antennaViewIdURL;
    }
    @Override
    public String getStrategicFlowId() {
        return antennaStrategicFlowId;
    }
    @Override
    public String getProbateFlowId() {
        return antennaProbateFlowId;
    }
    @Override
    public String getDivorceFlowId() {
        return antennaDivorceFlowId;
    }
    @Override
    public String getPrlFlowId() {
        return antennaPrlFlowId;
    }
    @Override
    public String getIacFlowId() {
        return antennaIacFlowId;
    }

}
