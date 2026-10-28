package uk.gov.hmcts.payment.functional.idam;

import feign.Feign;
import feign.FeignException;
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
import uk.gov.hmcts.payment.functional.idam.models.User;

import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.payment.functional.idam.IdamApi.CreateUserRequest.*;

@Service
public class IdamService {
    private static final Logger LOG = LoggerFactory.getLogger(IdamService.class);
    private static final int AUTH_RETRY_COUNT = 3;
    private static final long AUTH_RETRY_DELAY_MS = 1000L;

    public static final String CMC_CITIZEN_GROUP = "cmc-private-beta";
    public static final String CMC_CASE_WORKER_GROUP = "caseworker";

    public static final String BEARER = "Bearer ";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String CODE = "code";
    public static final String BASIC = "Basic ";
    public static final String GRANT_TYPE = "password";
    public static final String SCOPES = "openid profile roles";
    public static final String SCOPES_SEARCH_USER = "openid profile authorities acr roles search-user";
    public static final String SCOPES_CREATE_USER = "openid profile roles openid roles profile create-user manage-user";
    public static final String SCOPES_CREATE_USER_AND_SEARCH_USER = "openid profile roles create-user manage-user search-user";
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


    public User createUserWith(String... roles) {
        String email = nextUserEmail();
        CreateUserRequest userRequest = userRequest(email, roles);
        idamApi.createUser(userRequest);

        String accessToken = authenticateUserWithRetry(email, testConfig.getTestUserPassword());

        return User.userWith()
            .authorisationToken(accessToken)
            .email(email)
            .build();
    }

    public ValidUser createUserWithSearchScope(String... roles) {
        String email = nextUserEmail();
        CreateUserRequest userRequest = userRequest(email, roles);
        LOG.info("idamApi : " + idamApi.toString());
        LOG.info("userRequest : " + userRequest);
        try {
            idamApi.createUser(userRequest);
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }

        String accessToken = authenticateUserWithSearchScopeWithRetry(email, testConfig.getTestUserPassword());

        return new ValidUser(email, accessToken);
    }

    public ValidUser createUserWithSearchScopeForRefData(String... roles) {
        String email = nextUserEmailForRefData();
        CreateUserRequest userRequest = userRequest(email, roles);
        LOG.info("idamApi : " + idamApi.toString());
        LOG.info("userRequest : " + userRequest);
        try {
            idamApi.createUser(userRequest);
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }

        String accessToken = authenticateUserWithSearchScopeWithRetry(email, testConfig.getTestUserPassword());

        return new ValidUser(email, accessToken);
    }

    public ValidUser createUserWithCreateScope(String... roles) {
        String email = nextUserEmail();
        CreateUserRequest userRequest = userRequest(email, roles);
        LOG.info("idamApi : " + idamApi.toString());
        LOG.info("userRequest : " + userRequest);
        try {
            idamApi.createUser(userRequest);
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }

        String accessToken = authenticateUserWithCreateAndSearchScopeWithRetry(email, testConfig.getTestUserPassword());

        return new ValidUser(email, accessToken);
    }

    public ValidUser createUserWithRefDataEmailFormat(String... roles) {
        String email = nextUserEmailForRefData();
        CreateUserRequest userRequest = userRequest(email, roles);
        LOG.info("idamApi : " + idamApi.toString());
        LOG.info("userRequest : " + userRequest);
        try {
            idamApi.createUser(userRequest);
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }

        String accessToken = authenticateUserWithSearchScopeWithRetry(email, testConfig.getTestUserPassword());

        return new ValidUser(email, accessToken);
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
        LOG.info("testConfig.getIdamPayBubbleClientID() : " + testConfig.getIdamPayBubbleClientID());
        LOG.info("testConfig.getIdamPayBubbleClientSecret() : " + testConfig.getIdamPayBubbleClientSecret());

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

    public String authenticateUserWithCreateAndSearchScope(String username, String password) {
        String authorisation = username + ":" + password;
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        LOG.info("username : " + username);
        LOG.info("password : " + password);
        LOG.info("base64Authorisation : " + base64Authorisation);
        LOG.info(" testConfig.getIdamRefDataApiClientId() : " +  testConfig.getIdamRefDataApiClientId());
        LOG.info(" testConfig.getIdamRefDataApiClientSecret() : " +  testConfig.getIdamRefDataApiClientSecret());
        LOG.info("testConfig.getOauth2().getRedirectUrl() : " + testConfig.getOauth2().getRedirectUrl());

        try {
            TokenExchangeResponse tokenExchangeResponse = idamApi.exchangeCode(username,
                password,
                SCOPES_CREATE_USER_AND_SEARCH_USER,
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

    private CreateUserRequest userRequest(String email, String[] roles) {
        return userRequestWith()
            .email(email)
            .password(testConfig.getTestUserPassword())
            .roles(Stream.of(roles)
                .map(Role::new)
                .collect(toList()))
            .build();
    }

    private String nextUserEmail() {
        return String.format(testConfig.getGeneratedUserEmailPattern(), UUID.randomUUID().toString());
    }

    private String nextUserEmailForRefData() {
        LOG.info("The value of the Ref Data Email Id "+testConfig.getGeneratedUserEmailPatternForRefData());
        return String.format(testConfig.getGeneratedUserEmailPatternForRefData(), RandomStringUtils.random(6, true, true));
    }

    public static void deleteUser(String emailAddress)
    {
        idamApi.deleteUser(emailAddress);
    }

    private String authenticateUserWithRetry(String username, String password) {
        for (int attempt = 1; attempt <= AUTH_RETRY_COUNT; attempt++) {
            try {
                return authenticateUser(username, password);
            } catch (FeignException ex) {
                boolean isRetryable = isInvalidGrant(ex) || ex.status() == 429 || ex.status() >= 500;
                if (!isRetryable || attempt == AUTH_RETRY_COUNT) {
                    throw ex;
                }
                LOG.warn("Retrying IDAM authentication for {} after transient error (attempt {}/{}): status={} body={}",
                    username, attempt, AUTH_RETRY_COUNT, ex.status(), ex.contentUTF8());
                sleepBeforeRetry();
            }
        }
        throw new IllegalStateException("Unexpected IDAM authentication retry flow fallthrough");
    }

    private boolean isInvalidGrant(FeignException ex) {
        String body = ex.contentUTF8();
        return body != null && body.contains("invalid_grant");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(AUTH_RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying IDAM authentication", ie);
        }
    }

    private String authenticateUserWithSearchScopeWithRetry(String username, String password) {
        for (int attempt = 1; attempt <= AUTH_RETRY_COUNT; attempt++) {
            String accessToken = authenticateUserWithSearchScope(username, password);
            if (accessToken != null) {
                return accessToken;
            }
            if (attempt < AUTH_RETRY_COUNT) {
                LOG.warn("Retrying IDAM search-scope authentication for {} (attempt {}/{})",
                    username, attempt, AUTH_RETRY_COUNT);
                sleepBeforeRetry();
            }
        }
        throw new IllegalStateException("Unable to authenticate IDAM user with search scope after retries for " + username);
    }

    private String authenticateUserWithCreateAndSearchScopeWithRetry(String username, String password) {
        for (int attempt = 1; attempt <= AUTH_RETRY_COUNT; attempt++) {
            String accessToken = authenticateUserWithCreateAndSearchScope(username, password);
            if (accessToken != null) {
                return accessToken;
            }
            if (attempt < AUTH_RETRY_COUNT) {
                LOG.warn("Retrying IDAM create/search-scope authentication for {} (attempt {}/{})",
                    username, attempt, AUTH_RETRY_COUNT);
                sleepBeforeRetry();
            }
        }
        throw new IllegalStateException("Unable to authenticate IDAM user with create/search scope after retries for " + username);
    }
}
