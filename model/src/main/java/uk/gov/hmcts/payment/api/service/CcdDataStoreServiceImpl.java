package uk.gov.hmcts.payment.api.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.CcdCaseDetailsDto;

@Service
@Profile("!ccdMock")
public class CcdDataStoreServiceImpl implements CcdDataStoreService<CcdCaseDetailsDto, String> {

    @Autowired
    private OAuth2RestOperations restTemplate;

    @Value("${ccd-data-store.api.url}")
    private String baseUrl;

    @Override
    @HystrixCommand(commandKey = "retrieveCcdCaseInformation", commandProperties = {
        @HystrixProperty(name = "execution.timeout.enabled", value = "false")
    })
    public CcdCaseDetailsDto retrieve(String ccdCaseReference) {
        return restTemplate.getForObject(baseUrl + "/cases/" + ccdCaseReference, CcdCaseDetailsDto.class);
    }
}
