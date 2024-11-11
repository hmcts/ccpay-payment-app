package uk.gov.hmcts.payment.api.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;


@Service
@Profile("!ccdMock")
@EnableFeignClients(basePackages = "uk.gov.hmcts.reform.ccd.client")
public class CcdDataStoreClientServiceImpl implements CcdDataStoreClientService<CaseDetails, String> {

    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

    @Override
    @CircuitBreaker(name = "defaultCircuitBreaker")
    public CaseDetails getCase(String userAuthToken, String serviceAuthToken, String ccdCaseReference) {
        // Get case details from ccd.
        return coreCaseDataApi.getCase(userAuthToken, serviceAuthToken, ccdCaseReference);
    }
}
