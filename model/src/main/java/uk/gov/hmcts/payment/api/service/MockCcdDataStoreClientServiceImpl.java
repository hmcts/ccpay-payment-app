package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

@Service
@Profile("ccdMock")
public class MockCcdDataStoreClientServiceImpl implements CcdDataStoreClientService<CaseDetails, String> {

    private static final Logger LOG = LoggerFactory.getLogger(MockCcdDataStoreClientServiceImpl.class);

    @Override
    public CaseDetails getCase(String userAuthToken, String serviceAuthToken, String ccdCaseReference) {
        LOG.info("Called mock ccd data store service");
        if ("test-ok".equalsIgnoreCase(ccdCaseReference)) {
            return CaseDetails.builder()
                .jurisdiction("jurisdiction")
                .build();
        }

        throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Unknown test CCD case reference");
    }
}
