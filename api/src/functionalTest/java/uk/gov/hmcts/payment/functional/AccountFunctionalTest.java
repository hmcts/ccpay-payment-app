package uk.gov.hmcts.payment.functional;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Assume;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;

import java.util.ArrayList;
import java.util.List;

import static org.ff4j.utils.Util.assertNotNull;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class AccountFunctionalTest {
    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentsTestDsl dsl;

    @Autowired
    private IdamService idamService;
    @Autowired
    private S2sTokenService s2sTokenService;

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
    private static List<String> userEmails = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(!testProps.baseTestUrl.contains("payment-api-pr-"));
        if (!TOKENS_INITIALIZED) {
            User user = idamService.createUserWith("citizen");
            USER_TOKEN = user.getAuthorisationToken();
            userEmails.add(user.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void whenAccountExistsTestShouldReturn200() {
        AccountDto existingAccountDto = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().getAccountInfomation(testProps.getExistingAccountNumber())
            .then().getAccount();

        assertNotNull(existingAccountDto);
    }

    @Test
    public void whenAccountFakeTestShouldReturn404() {
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().getAccountInfomation(testProps.getFakeAccountNumber())
            .then().notFound();
    }

    @AfterClass
    public static void tearDown()
    {
        if (!userEmails.isEmpty()) {
            // delete idam test user
            userEmails.forEach(IdamService::deleteUser);
        }
    }
}
