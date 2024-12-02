package uk.gov.hmcts.payment.api.componenttests;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.service.AccountServiceImpl;
import uk.gov.hmcts.payment.api.service.LiberataService;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AccountServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LiberataService liberataService;

    @InjectMocks
    private AccountServiceImpl accountServiceImpl;

    @Value("${liberata.api.account.url}")
    private String baseUrl;

    @RegisterExtension
    static WireMockExtension wireMockRule = WireMockExtension.newInstance()
        .options(WireMockConfiguration.wireMockConfig().port(9190))
        .build();

    @Test
    void retrieveExistingAccountReturnsAccountDto() throws Exception {
        String pbaCode = "PBA1234";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("accessToken");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        setField(accountServiceImpl, accountServiceImpl.getClass().getDeclaredField("baseUrl"), baseUrl);
        AccountDto expectedDto = new AccountDto(pbaCode, "accountName", new BigDecimal(100),
            new BigDecimal(100), AccountStatus.ACTIVE, new Date());
        when(liberataService.getAccessToken()).thenReturn("accessToken");
        when(restTemplate.getForObject(baseUrl + "/" + pbaCode, AccountDto.class, entity)).thenReturn(expectedDto);
        assertEquals(expectedDto, accountServiceImpl.retrieve(pbaCode));
    }

    @Test
    void retrieveMockAccountReturnsAccountDto() throws Exception {
        String pbaCode = "PBAFUNC12345";
        AccountDto expectedDto = new AccountDto(pbaCode, "CAERPHILLY COUNTY BOROUGH COUNCIL", new BigDecimal(28879),
            new BigDecimal(30000), AccountStatus.ACTIVE, null);
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
