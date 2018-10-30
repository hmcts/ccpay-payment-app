package uk.gov.hmcts.payment.functional.idam;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.idam.IdamApi.CreateUserRequest;
import uk.gov.hmcts.payment.functional.idam.IdamApi.Role;
import uk.gov.hmcts.payment.functional.idam.IdamApi.TokenExchangeResponse;
import uk.gov.hmcts.payment.functional.idam.IdamApi.UserGroup;
import uk.gov.hmcts.payment.functional.idam.models.User;

import java.util.Base64;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.payment.functional.idam.IdamApi.CreateUserRequest.*;

@Service
public class IdamService {
    private static final Logger LOG = LoggerFactory.getLogger(IdamService.class);

    public static final String CMC_CITIZEN_GROUP = "cmc-private-beta";
    public static final String CMC_CASE_WORKER_GROUP = "caseworker-cmc-solicitor";

    public static final String BEARER = "Bearer ";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String CODE = "code";
    public static final String BASIC = "Basic ";

    private final IdamApi idamApi;
    private final TestConfigProperties testConfig;

    @Autowired
    public IdamService(TestConfigProperties testConfig) {
        this.testConfig = testConfig;
        idamApi = Feign.builder()
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .target(IdamApi.class, testConfig.getIdamApiUrl());
    }


    public User createUserWith(String userGroup, String... roles) {
        String email = nextUserEmail();
        CreateUserRequest userRequest = userRequest(email, userGroup, roles);
        idamApi.createUser(userRequest);

        String accessToken = authenticateUser(email, testConfig.getTestUserPassword());

        return User.userWith()
            .authorisationToken(accessToken)
            .email(email)
            .build();
    }

    public String authenticateUser(String username, String password) {
        String authorisation = username + ":" + password;
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        IdamApi.AuthenticateUserResponse authenticateUserResponse = idamApi.authenticateUser(
            BASIC + base64Authorisation,
            CODE,
            testConfig.getOauth2().getClientId(),
            testConfig.getOauth2().getRedirectUrl());

        TokenExchangeResponse tokenExchangeResponse = idamApi.exchangeCode(
            authenticateUserResponse.getCode(),
            AUTHORIZATION_CODE,
            testConfig.getOauth2().getClientId(),
            testConfig.getOauth2().getClientSecret(),
            testConfig.getOauth2().getRedirectUrl()
        );

        return BEARER + tokenExchangeResponse.getAccessToken();
    }


    private CreateUserRequest userRequest(String email, String userGroup, String[] roles) {
        return userRequestWith()
            .email(email)
            .password(testConfig.getTestUserPassword())
            .roles(Stream.of(roles)
                .map(Role::new)
                .collect(toList()))
            .userGroup(new UserGroup(userGroup))
            .build();
    }

    private String nextUserEmail() {
        return String.format(testConfig.getGeneratedUserEmailPattern(), RandomStringUtils.randomAlphanumeric(10));
    }
}
