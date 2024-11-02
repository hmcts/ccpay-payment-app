package uk.gov.hmcts.payment.api.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;

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
    @HystrixCommand(commandKey = "retrievePbaAccount", commandProperties = {
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "15000"),
        @HystrixProperty(name = "fallback.enabled", value = "false")
    })
    public AccountDto retrieve(String pbaCode) {
        LOG.info("AccountDto retrieve(String pbaCode) called with pbaCode: {}", pbaCode);
        if (pbaCode.equalsIgnoreCase("PBAFUNC12345")) {
            return AccountDto.accountDtoWith()
                .accountNumber("PBAFUNC12345")
                .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
                .creditLimit(BigDecimal.valueOf(28879))
                .availableBalance(BigDecimal.valueOf(30000))
                .status(AccountStatus.ACTIVE)
                .build();
        }

        String accessToken = liberataService.getAccessToken("liberata-client");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        LOG.error("Calling Liberata API to retrieve account details for PBA code: {}", pbaCode);
        LOG.error("getAccessToken: {}", accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = String.format("%s/%s", baseUrl, pbaCode);
        return restTemplate.getForObject(url, AccountDto.class, entity);
    }

}
