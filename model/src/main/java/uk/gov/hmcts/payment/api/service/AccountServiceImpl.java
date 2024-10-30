package uk.gov.hmcts.payment.api.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Date;

@Service
@Profile("!liberataMock")
public class AccountServiceImpl implements AccountService<AccountDto, String> {

    private RestTemplate restTemplate;

    @Value("${liberata.api.account.url}")
    private String baseUrl;

    @Override
    @CircuitBreaker(name = "defaultCircuitBreaker")
    @TimeLimiter(name = "retrievePbaAccountTimeLimiter")
    public AccountDto retrieve(String pbaCode) {
        if(pbaCode.equalsIgnoreCase("PBAFUNC12345")){
            return AccountDto.accountDtoWith()
                .accountNumber("PBAFUNC12345")
                .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
                .creditLimit(BigDecimal.valueOf(28879))
                .availableBalance(BigDecimal.valueOf(30000))
                .status(AccountStatus.ACTIVE)
                .build();
        }
        return restTemplate.getForObject(baseUrl + "/" + pbaCode, AccountDto.class);
    }
}
