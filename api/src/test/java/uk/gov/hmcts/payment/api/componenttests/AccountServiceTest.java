package uk.gov.hmcts.payment.api.componenttests;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.service.AccountServiceImpl;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class AccountServiceTest {

    @Mock
    private WebClient webClient;

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

        mockWebClient(expectedDto, baseUrl + "/" + pbaCode);
        AccountDto actualDto = accountServiceImpl.retrieve(pbaCode);

        assertEquals(expectedDto, actualDto);
    }

    @Test
    public void retrieveMockAccountReturnsAccountDto() throws Exception {
        String pbaCode = "PBAFUNC12345";
        AccountDto expectedDto = new AccountDto(pbaCode, "CAERPHILLY COUNTY BOROUGH COUNCIL", new BigDecimal(28879),
            new BigDecimal(30000), AccountStatus.ACTIVE,null);

        mockWebClient(expectedDto, baseUrl + "/" + pbaCode);
        AccountDto actualDto = accountServiceImpl.retrieve(pbaCode);
        assertEquals(expectedDto, actualDto);
    }

    private void mockWebClient(AccountDto expectedDto, String url) {
        WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec<?> requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenAnswer(invocation -> requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenAnswer(invocation -> requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenAnswer(invocation -> responseSpec);
        when(responseSpec.bodyToMono(AccountDto.class)).thenAnswer(invocation -> Mono.just(expectedDto));
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

