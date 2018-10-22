package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.payment.api.dto.AccountDto;

@Service
public class AccountServiceImpl implements AccountService<AccountDto, String> {

    @Autowired
    private OAuth2RestOperations restTemplate;

    @Value("${liberata.api.account.url}")
    private String baseUrl;

    @Override
    public AccountDto retrieve(String pbaCode) throws HttpClientErrorException {
        return restTemplate
            .getForObject(baseUrl + "/" + pbaCode, AccountDto.class);
    }
}
