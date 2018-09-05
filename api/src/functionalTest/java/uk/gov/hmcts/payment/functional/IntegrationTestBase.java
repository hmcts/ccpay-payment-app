package uk.gov.hmcts.payment.functional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class IntegrationTestBase {

    @Value("${local.proxy.host:proxyout.reform.hmcts.net}")
    public String localProxyHost;

    @Value("${local.proxy.port:8080}")
    public String localProxyPort;

    @Value("${probate.user.role}")
    protected String probateUserRole;

    @Value("${probate.user.group}")
    protected String probateUserGroup;

    @Value("${probate.service.name}")
    protected String probateServiceName;

    @Value("${probate.service.secret}")
    protected String probateSecret;

    @Value("${cmc.user.role}")
    protected String cmcUserRole;

    @Value("${cmc.user.group}")
    protected String cmcUserGroup;

    @Value("${cmc.service.name}")
    protected String cmcServiceName;

    @Value("${cmc.service.secret}")
    protected String cmcSecret;

    @Test
    public void testProperties() {
        Assert.assertEquals(cmcUserRole, "citizen");
    }

}
