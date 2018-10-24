package uk.gov.hmcts.payment.api.componenttests;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.controllers.AccountController;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class AccountControllerTest {

    private RestActions restActions;

    @Mock
    private AccountService<AccountDto, String> accountSericeMock;

    @InjectMocks
    private AccountController accountController;

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);


    @Test
    public void gettingExistingAccountNumberReturnsAccountDetails() {
        AccountDto expectedDto = new AccountDto("PBA1234", "accountName", new BigDecimal(100),
            new BigDecimal(100), "ACTIVE", new Date());
        when(accountSericeMock.retrieve("PBA1234")).thenReturn(expectedDto);

        AccountDto actualDto = accountController.getAccounts("PBA1234");

        assertEquals(expectedDto, actualDto);
    }

    @Test(expected = AccountNotFoundException.class)
    public void gettingNonExistingAccountNumberThrowsAccountNotFoundException() {
        when(accountSericeMock.retrieve("PBA4321")).thenThrow(AccountNotFoundException.class);
        accountController.getAccounts("PBA4321");
    }
}
