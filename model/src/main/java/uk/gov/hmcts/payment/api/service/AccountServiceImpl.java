package uk.gov.hmcts.payment.api.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;

@Service
@Profile("!liberataMock")
public class AccountServiceImpl implements AccountService<AccountDto, String> {

    private static final Logger LOG = LoggerFactory.getLogger(AccountServiceImpl.class);
    @Autowired
    private OAuth2RestOperations restTemplate;

    @Value("${liberata.api.account.url}")
    private String baseUrl;

    @Value("${liberata.api.functional.test.mock.account}")
    private String mockAccount;

    @Override
    @HystrixCommand(commandKey = "retrievePbaAccount", commandProperties = {
        @HystrixProperty(name = "execution.timeout.enabled", value = "false")
    })
    public AccountDto retrieve(String pbaCode) {
        LOG.info("Calling liberata account service!!!");
        if (mockAccount != null && mockAccount.equalsIgnoreCase(pbaCode)) {
            LOG.info("Returning Mock response for functional tests!!!");
            return AccountDto.accountDtoWith()
                .accountNumber("PBAFunctional")
                .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
                .creditLimit(BigDecimal.valueOf(28879))
                .availableBalance(BigDecimal.valueOf(30000))
                .status(AccountStatus.ACTIVE)
                .build();
        } else {
            LOG.info("Call to liberata started!!!");
            return restTemplate.getForObject(baseUrl + "/" + pbaCode, AccountDto.class);
        }
    }
}
