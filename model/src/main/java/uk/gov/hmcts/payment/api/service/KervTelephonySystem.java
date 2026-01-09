package uk.gov.hmcts.payment.api.service;

import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@SuperBuilder
@NoArgsConstructor
@Component
public class KervTelephonySystem extends TelephonySystem {

    public static final String TELEPHONY_SYSTEM_NAME = "kerv";

    private static final String SYSTEM_NAME = TELEPHONY_SYSTEM_NAME;

    @Value("${pci-pal.kerv.grant.type}")
    private String kervGrantType;

    @Value("${pci-pal.kerv.tenant.name}")
    private String kervTenantName;

    @Value("${pci-pal.kerv.client.id}")
    private String kervClientId;

    @Value("${pci-pal.kerv.client.secret}")
    private String kervClientSecret;

    @Value("${pci-pal.kerv.get.tokens.url}")
    private String kervTokensURL;

    @Value("${pci-pal.kerv.launch.url}")
    private String kervLaunchURL;

    @Value("${pci-pal.kerv.view.id.url}")
    private String kervViewIdURL;

    @Value("${pci-pal.kerv.strategic.flow.id}")
    private String kervStrategicFlowId;

    @Value("${pci-pal.kerv.probate.flow.id}")
    private String kervProbateFlowId;

    @Value("${pci-pal.kerv.divorce.flow.id}")
    private String kervDivorceFlowId;

    @Value("${pci-pal.kerv.prl.flow.id}")
    private String kervPrlFlowId;

    @Value("${pci-pal.kerv.iac.flow.id}")
    private String kervIacFlowId;

    @Override
    public String getSystemName() {
        return SYSTEM_NAME;
    }
    @Override
    public String getGrantType() {
        return kervGrantType;
    }
    @Override
    public String getTenantName() {
        return kervTenantName;
    }
    @Override
    public String getClientId() {
        return kervClientId;
    }
    @Override
    public String getClientSecret() {
        return kervClientSecret;
    }
    @Override
    public String getTokensURL() {
        return kervTokensURL;
    }
    @Override
    public String getLaunchURL() {
        return kervLaunchURL;
    }
    @Override
    public String getViewIdURL() {
        return kervViewIdURL;
    }
    @Override
    public String getStrategicFlowId() {
        return kervStrategicFlowId;
    }
    @Override
    public String getProbateFlowId() {
        return kervProbateFlowId;
    }
    @Override
    public String getDivorceFlowId() {
        return kervDivorceFlowId;
    }
    @Override
    public String getPrlFlowId() {
        return kervPrlFlowId;
    }
    @Override
    public String getIacFlowId() {
        return kervIacFlowId;
    }

}
