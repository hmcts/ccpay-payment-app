package uk.gov.hmcts.payment.api.service;

import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("!liberataMock")
public class AccountServiceImpl implements AccountService<AccountDto, String> {

    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    private LiberataService liberataService;

    @Autowired
    @Qualifier("restTemplateLiberata")
    private RestTemplate restTemplate;

    @Value("${liberata.api.account.url}")
    private String baseUrl;

    @Override
    @CircuitBreaker(name = "defaultCircuitBreaker")
    public AccountDto retrieve(String pbaCode) throws ResourceAccessException {
        LOG.info("AccountDto retrieve(String pbaCode) called with pbaCode: {}", pbaCode);
        return retrieveAsync(pbaCode).join();
    }

    @TimeLimiter(name = "retrievePbaAccountTimeLimiter")
    private CompletableFuture<AccountDto> retrieveAsync(String pbaCode) {
        return CompletableFuture.supplyAsync(() -> {
            if (pbaCode.equalsIgnoreCase("PBAFUNC12345")) {
                return AccountDto.accountDtoWith()
                    .accountNumber("PBAFUNC12345")
                    .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
                    .creditLimit(BigDecimal.valueOf(28879))
                    .availableBalance(BigDecimal.valueOf(30000))
                    .status(AccountStatus.ACTIVE)
                    .build();
            }

            String accessToken = liberataService.getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.getForObject(baseUrl + "/" + pbaCode, AccountDto.class, entity);
        });
    }
}
