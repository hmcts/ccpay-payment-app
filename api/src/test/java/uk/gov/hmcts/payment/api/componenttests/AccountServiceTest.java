package uk.gov.hmcts.payment.api.componenttests;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.service.AccountServiceImpl;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class AccountServiceTest {
    @Mock
    private OAuth2RestOperations restTemplateMock;

    @InjectMocks
    private AccountServiceImpl accountServiceImpl;

    @Value("${liberata.api.account.url}")
    private String baseUrl;

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);

    @Test
    public void retrieveExistingAccountReturnsAccountDto() throws Exception {
        String pbaCode = "PBA1234";
        setField(accountServiceImpl, accountServiceImpl.getClass().getDeclaredField("baseUrl"), baseUrl);
        AccountDto expectedDto = new AccountDto(pbaCode, "accountName", new BigDecimal(100),
            new BigDecimal(100), AccountStatus.ACTIVE, new Date());
        when(restTemplateMock.getForObject(baseUrl + "/" + pbaCode, AccountDto.class)).thenReturn(expectedDto);
        assertEquals(expectedDto, accountServiceImpl.retrieve(pbaCode));
    }

    @Test
    public void retrieveMockAccountReturnsAccountDto() throws Exception {
        String pbaCode = "PBAFUNC12345";
        AccountDto expectedDto = new AccountDto(pbaCode, "CAERPHILLY COUNTY BOROUGH COUNCIL", new BigDecimal(28879),
            new BigDecimal(30000), AccountStatus.ACTIVE,null);
        when(restTemplateMock.getForObject(baseUrl + "/" + pbaCode, AccountDto.class)).thenReturn(expectedDto);
        assertEquals(expectedDto, accountServiceImpl.retrieve(pbaCode));
    }

    private void setField(Object object, Field fld, Object value) {
        try {
            fld.setAccessible(true);
            fld.set(object, value);
        } catch (IllegalAccessException e) {
            String fieldName = null == fld ? "n/a" : fld.getName();
            throw new RuntimeException("Failed to set " + fieldName + " of object", e);
        }
    }
}
