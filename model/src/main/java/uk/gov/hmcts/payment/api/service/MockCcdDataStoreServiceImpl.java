package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.payment.api.dto.CcdCaseDetailsDto;

@Service
@Profile("liberataMock")
public class MockCcdDataStoreServiceImpl implements CcdDataStoreService<CcdCaseDetailsDto, String> {

    private static final Logger LOG = LoggerFactory.getLogger(MockCcdDataStoreServiceImpl.class);

    @Override
    public CcdCaseDetailsDto retrieve(String ccdCaseReference) {
        LOG.info("Called mock liberata account service");
        if ("test-ok".equalsIgnoreCase(ccdCaseReference)) {
            return CcdCaseDetailsDto.ccdCaseDetailsDtoWith().build();
        }

        throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Unknown test CCD case reference");
    }
}
