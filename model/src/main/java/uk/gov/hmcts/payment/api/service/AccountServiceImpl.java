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

@Service
@Profile("!liberataMock")
public class AccountServiceImpl implements AccountService<AccountDto, String> {

    private static final Logger LOG = LoggerFactory.getLogger(AccountServiceImpl.class);
    @Autowired
    private OAuth2RestOperations restTemplate;

    @Value("${liberata.api.account.url}")
    private String baseUrl;

    @Override
     @HystrixCommand(commandKey = "retrievePbaAccount", commandProperties = {
        @HystrixProperty(name = "execution.timeout.enabled", value = "false")
    })
    public AccountDto retrieve(String pbaCode) {
        LOG.error("Calling liberata account service!!!");
        return restTemplate.getForObject(baseUrl + "/" + pbaCode, AccountDto.class);
    }
}
