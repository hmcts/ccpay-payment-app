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
import uk.gov.hmcts.payment.functional.config.ValidUser;
import uk.gov.hmcts.payment.functional.idam.IdamApi.CreateUserRequest;
import uk.gov.hmcts.payment.functional.idam.IdamApi.Role;
import uk.gov.hmcts.payment.functional.idam.IdamApi.TokenExchangeResponse;
import uk.gov.hmcts.payment.functional.idam.IdamApi.UserGroup;
import uk.gov.hmcts.payment.functional.idam.models.User;

import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.payment.functional.idam.IdamApi.CreateUserRequest.*;

@Service
public class IdamService {
    private static final Logger LOG = LoggerFactory.getLogger(IdamService.class);

    public static final String CMC_CITIZEN_GROUP = "cmc-private-beta";
    public static final String CMC_CASE_WORKER_GROUP = "caseworker";

    public static final String BEARER = "Bearer ";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String CODE = "code";
    public static final String BASIC = "Basic ";
    public static final String GRANT_TYPE = "password";
    public static final String SCOPES = "openid profile roles";
    public static final String SCOPES_SEARCH_USER = "openid profile roles search-user";
    public static final String SCOPES_CREATE_USER = "openid profile roles openid roles profile create-user manage-user";
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

    public ValidUser createUserWithSearchScope(String userGroup, String... roles) {
        String email = nextUserEmail();
        CreateUserRequest userRequest = userRequest(email, userGroup, roles);
        LOG.info("idamApi : " + idamApi.toString());
        LOG.info("userRequest : " + userRequest);
        try {
            idamApi.createUser(userRequest);
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }

        String accessToken = authenticateUserWithSearchScope(email, testConfig.getTestUserPassword());

        return new ValidUser(email, accessToken);
    }

    public ValidUser createUserWithCreateScope(String userGroup, String... roles) {
        String email = nextUserEmail();
        CreateUserRequest userRequest = userRequest(email, userGroup, roles);
        LOG.info("idamApi : " + idamApi.toString());
        LOG.info("userRequest : " + userRequest);
        try {
            idamApi.createUser(userRequest);
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }

        String accessToken = authenticateUserWithCreateScope(email, testConfig.getTestUserPassword());

        return new ValidUser(email, accessToken);
    }

    public User createUserWithRefDataEmailFormat(String userGroup, String... roles) {
        String email = nextUserEmailForRefData();
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

    public String authenticateUserWithSearchScope(String username, String password) {
        String authorisation = username + ":" + password;
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        LOG.info("username : " + username);
        LOG.info("password : " + password);
        LOG.info("base64Authorisation : " + base64Authorisation);
        LOG.info("testConfig.getOauth2().getClientId() : " + testConfig.getOauth2().getClientId());
        LOG.info("testConfig.getOauth2().getRedirectUrl() : " + testConfig.getOauth2().getRedirectUrl());

        try {
            TokenExchangeResponse tokenExchangeResponse = idamApi.exchangeCode(username,
                password,
                SCOPES_SEARCH_USER,
                GRANT_TYPE,
                testConfig.getIdamPayBubbleClientID(),
                testConfig.getIdamPayBubbleClientSecret(),
                testConfig.getOauth2().getRedirectUrl());

            return BEARER + tokenExchangeResponse.getAccessToken();
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }
        return null;
    }

    public String authenticateUserWithCreateScope(String username, String password) {
        String authorisation = username + ":" + password;
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        LOG.info("username : " + username);
        LOG.info("password : " + password);
        LOG.info("base64Authorisation : " + base64Authorisation);
        LOG.info("testConfig.getOauth2().getClientId() : " + testConfig.getOauth2().getClientId());
        LOG.info("testConfig.getOauth2().getRedirectUrl() : " + testConfig.getOauth2().getRedirectUrl());

        try {
            TokenExchangeResponse tokenExchangeResponse = idamApi.exchangeCode(username,
                password,
                SCOPES_CREATE_USER,
                GRANT_TYPE,
                testConfig.getIdamRefDataApiClientId(),
                testConfig.getIdamRefDataApiClientSecret(),
                testConfig.getOauth2().getRedirectUrl());

            return BEARER + tokenExchangeResponse.getAccessToken();
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }
        return null;
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
        return String.format(testConfig.getGeneratedUserEmailPattern(), UUID.randomUUID().toString());
    }

    private String nextUserEmailForRefData() {
        LOG.info("The value of the Ref Data Email Id "+testConfig.getGeneratedUserEmailPatternForRefData());
        LOG.info("The value of the Formatted Ref Data Email Id "+String.format(testConfig.getGeneratedUserEmailPatternForRefData(), RandomStringUtils.random(6, true, true)));
        return String.format(testConfig.getGeneratedUserEmailPatternForRefData(), RandomStringUtils.random(6, true, true));
    }
}
