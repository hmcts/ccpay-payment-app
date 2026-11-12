package uk.gov.hmcts.payment.smoke.idam;

import feign.Feign;
import feign.FeignException;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.smoke.config.TestConfigProperties;
import uk.gov.hmcts.payment.smoke.config.ValidUser;
import uk.gov.hmcts.payment.smoke.idam.models.User;

import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Service
public class IdamService {

    public static final String BEARER = "Bearer ";
    public static final String GRANT_TYPE = "password";
    public static final String SCOPES = "openid profile roles";
    private static IdamApi idamApi;
    private final TestConfigProperties testConfig;

    @Autowired
    public IdamService(TestConfigProperties testConfig) {
        this.testConfig = testConfig;
        idamApi = Feign.builder()
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .target(IdamApi.class, testConfig.getIdamApiUrl());
    }

    public String authenticateUser(String username, String password) {
        IdamApi.TokenExchangeResponse tokenExchangeResponse = idamApi.exchangeCode(
            username,
            password,
            SCOPES,
            GRANT_TYPE,
            testConfig.getOauth2().getClientId(),
            testConfig.getOauth2().getClientSecret(),
            testConfig.getOauth2().getRedirectUrl()
        );

        return BEARER + tokenExchangeResponse.getAccessToken();
    }

}
