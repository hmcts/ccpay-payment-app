package uk.gov.hmcts.payment.functional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@TestComponent
public class IntegrationTestBase {

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

    @Value("${payments.cmc.test.user:cmcuser@domain.com}")
    protected String paymentCmcTestUser;

    @Value("${payments.cmc.test.user.id:148906}")
    protected String paymentCmcTestUserId;

    @Value("${payments.cmc.test.user.password:dummy}")
    protected String paymentCmcTestPassword;

    public  <T> T translateException(CallableWithException<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    interface CallableWithException<T> {
        T call() throws Exception;
    }

}
