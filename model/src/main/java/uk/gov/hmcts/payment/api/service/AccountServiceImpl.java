package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.AccountDto;

@Service
@Profile("!liberataMock")
public class AccountServiceImpl implements AccountService<AccountDto, String> {

    @Autowired
    private OAuth2RestOperations restTemplate;

    @Value("${liberata.api.account.url}")
    private String baseUrl;

//    @Override
//     @HystrixCommand(commandKey = "retrievePbaAccount", commandProperties = {
//        @HystrixProperty(name = "execution.timeout.enabled", value = "false")
//    })
    public AccountDto retrieve(String pbaCode) {
        return restTemplate.getForObject(baseUrl + "/" + pbaCode, AccountDto.class);
    }
}
