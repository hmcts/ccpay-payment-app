package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import uk.gov.hmcts.payment.api.dto.PBAResponse;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PBAAccountsTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;
import static uk.gov.hmcts.payment.functional.service.RefDataTestService.approveOrganisation;
import static uk.gov.hmcts.payment.functional.service.RefDataTestService.postOrganisation;
import static uk.gov.hmcts.payment.functional.service.RefDataTestService.readFileContents;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests", "liberataMock"})
public class PBAAccountsFunctionalTest {

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    private static String USER_TOKEN_PUI_FINANCE_MANAGER;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;

    private static final String INPUT_FILE_PATH = "uk/gov/hmcts/payment/functional/pbaaccounts";

    @Before
    public void setUp() throws Exception {
    }

    @Test
    @Ignore("As we need support from Raj to Cover this test....")
    public void perform_pba_accounts_lookup() throws Exception {
        final String userPUIFinanceManagerToken = this.getFinanceManagerTokenForOrganisation();
        Response getPBAAccountsResponse = PBAAccountsTestService.getPBAAccounts(userPUIFinanceManagerToken, SERVICE_TOKEN);
        assertThat(getPBAAccountsResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PBAResponse pbaResponseDTO = getPBAAccountsResponse.getBody().as(PBAResponse.class);

    }

    private final String getFinanceManagerTokenForOrganisation() throws Exception {
        final User user = idamService.createUserWithRefDataEmailFormat(CMC_CASE_WORKER_GROUP,
            "pui-finance-manager");
        final String userPUIFinanceManagerToken = user.getAuthorisationToken();
        final String userEmail = user.getEmail();

        SERVICE_TOKEN = s2sTokenService.getS2sToken("payment_app", testProps.getPaymentAppS2SSecret());
        final String fileContentsTemplate = readFileContents(INPUT_FILE_PATH + "/" + "CreateOrganisation.json");
        System.out.println("The value of the File Contents Before Templating : " + fileContentsTemplate);
        final String fileContents = String.format(fileContentsTemplate,
            generateRandomString(13, true, false),
            generateRandomString(8, true, false),
            userEmail,
            generateRandomString(6, true, false),
            generateRandomString(6, true, false),
            generateRandomString(6, true, false));
        System.out.println("The value of the File Contents After Templating : " + fileContents);
        Response response = postOrganisation(SERVICE_TOKEN, testProps.getRefDataApiUrl(), fileContents);
        System.out.println("The value of the Body" + response.getBody().prettyPrint());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        String organisationIdentifier = response.jsonPath().getString("organisationIdentifier");
        System.out.println(organisationIdentifier);


        final String prdAdminToken = idamService.createUserWithCreateScope(CMC_CASE_WORKER_GROUP, "prd-admin").getAuthorisationToken();
        System.out.println("The value of the Admin Token : " + prdAdminToken);
        System.out.println("The value of the Service Token : " + SERVICE_TOKEN);
        Response updatedResponse = approveOrganisation(prdAdminToken, SERVICE_TOKEN, testProps.getRefDataApiUrl(), fileContents, organisationIdentifier);
        assertThat(updatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        return userPUIFinanceManagerToken;
    }


    public static String generateRandomString(final int length, final boolean useLetters, final boolean useNumbers) {
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }

}
