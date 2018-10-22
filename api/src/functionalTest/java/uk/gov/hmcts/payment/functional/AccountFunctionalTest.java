package uk.gov.hmcts.payment.functional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;

import static org.ff4j.utils.Util.assertNotNull;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class AccountFunctionalTest {
    @Autowired
    private IntegrationTestBase testProps;

    @Autowired(required = true)
    private PaymentsTestDsl dsl;

    @Test
    public void whenAccountExistsTestShouldReturn200() {
        AccountDto existingAccountDto = dsl.given().userId(testProps.paymentCmcTestUser, testProps.paymentCmcTestUserId, testProps.paymentCmcTestPassword, testProps.cmcUserGroup)
            .serviceId(testProps.cmcServiceName, testProps.cmcSecret)
            .when().getAccountInfomation(testProps.existingAccountNumber)
            .then().getAccount();

        assertNotNull(existingAccountDto);
    }

    @Test
    public void whenAccountFakeTestShouldReturn404() {
        dsl.given().userId(testProps.paymentCmcTestUser, testProps.paymentCmcTestUserId, testProps.paymentCmcTestPassword, testProps.cmcUserGroup)
            .serviceId(testProps.cmcServiceName, testProps.cmcSecret)
            .when().getAccountInfomation(testProps.fakeAccountNumber)
            .then().notFound();
    }
}
