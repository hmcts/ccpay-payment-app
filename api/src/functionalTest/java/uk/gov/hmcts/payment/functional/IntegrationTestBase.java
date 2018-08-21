package uk.gov.hmcts.payment.functional;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class IntegrationTestBase {

    @Value("${probate.user.id}")
    protected String probateUserId;

    @Value("${probate.service.name}")
    protected String probateServiceName;

    @Value("${probate.service.secret}")
    protected String probateSecret;
}
