package uk.gov.hmcts.payment.functional.ccd;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.functional.ccd.request.IacDraftCaseCreateRequest;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;

@Service
public class CcdService {

    private static CcdApi ccdApi;
    private final TestConfigProperties testConfig;

    private static final Logger LOG = LoggerFactory.getLogger(CcdService.class);

    @Autowired
    public CcdService(TestConfigProperties testConfig) {
        this.testConfig = testConfig;
        ccdApi = Feign.builder()
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .target(CcdApi.class, testConfig.getCcdApiUrl());
    }

    public String createIacDraftCase(String userId, String authToken, String s2sToken) {
        String iacDraftCaseCreateToken = getIacDraftCaseCreateToken(userId, authToken, s2sToken);
        IacDraftCaseCreateRequest iacDraftCaseCreateRequest = iacDraftCaseCreateRequest(iacDraftCaseCreateToken);
        LOG.info("Create IAC Draft Case Request : {}", iacDraftCaseCreateRequest);

        CcdApi.IacCreateDraftCaseResponse iacDraftCase;
        try {
            iacDraftCase = ccdApi.createIacDraftCase(
                authToken,
                s2sToken,
                iacDraftCaseCreateRequest
            );
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to create IAC draft case in CCD", ex);
        }

        if (iacDraftCase == null || iacDraftCase.getId() == null) {
            throw new IllegalStateException("CCD create IAC draft case response did not contain a case id");
        }

        return iacDraftCase.getId();
    }

    public String getIacDraftCaseCreateToken(String userId, String authToken, String s2sToken) {
        CcdApi.iacDraftCaseCreateTokenResponse iacDraftCaseCreateTokenResponse;
        try {
            iacDraftCaseCreateTokenResponse = ccdApi.getIacDraftCaseCreateToken(
                userId,
                authToken,
                s2sToken
            );
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to get IAC draft case create token from CCD", ex);
        }

        if (iacDraftCaseCreateTokenResponse == null || iacDraftCaseCreateTokenResponse.getToken() == null) {
            throw new IllegalStateException("CCD IAC draft case create token response did not contain a token");
        }

        return iacDraftCaseCreateTokenResponse.getToken();
    }

    public IacDraftCaseCreateRequest iacDraftCaseCreateRequest(String createEventToken) {
        return IacDraftCaseCreateRequest.builder()
            .data(IacDraftCaseCreateRequest.Data.builder().build())
            .event(
                IacDraftCaseCreateRequest.Event.builder().build()
            )
            .eventToken(createEventToken)
            .build();
    }
}
