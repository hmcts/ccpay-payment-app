package uk.gov.hmcts.payment.api.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;

@Service
@Profile("!liberataMock")
public class AccountServiceImpl implements AccountService<AccountDto, String> {

    private RestTemplate restTemplate;

    @Value("${liberata.api.account.url}")
    private String baseUrl;

    @Override
    @HystrixCommand(commandKey = "retrievePbaAccount", commandProperties = {
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "15000"),
        @HystrixProperty(name = "fallback.enabled", value = "false")
    })
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
