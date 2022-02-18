package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.dto.IdamFullNameRetrivalResponse;
import uk.gov.hmcts.payment.api.dto.idam.IdamUserIdResponse;
import uk.gov.hmcts.payment.api.dto.IdamUserIdDetailsResponse;
import uk.gov.hmcts.payment.api.dto.UserIdentityDataDto;
import uk.gov.hmcts.payment.api.exception.UserNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;


import java.util.Collections;
import java.util.List;

@Service
@SuppressWarnings("PMD.PreserveStackTrace")
public class IdamServiceImpl implements IdamService {

    private static final Logger LOG = LoggerFactory.getLogger(IdamServiceImpl.class);

    public static final String USERDETAILS_ENDPOINT = "/details";
    public static final String USERID_ENDPOINT = "/o/userinfo";

    public static final String USER_FULL_NAME_ENDPOINT = "/api/v1/users";
    private static final String INTERNAL_SERVER_ERROR_MSG = "Internal Server error. Please, try again later";
    private static final String USER_DETAILS_NOT_FOUND_ERROR_MSG = "User details not found for these roles in IDAM";

    @Value("${auth.idam.client.baseUrl}")
    private String idamBaseURL;

    @Autowired()
    @Qualifier("restTemplateIdam")
    private RestTemplate restTemplateIdam;

    @Override
    public IdamUserIdResponse getUserId(MultiValueMap<String, String> headers) {
        try {
            ResponseEntity<IdamUserIdResponse> responseEntity = getResponseEntity(headers);
            if (responseEntity != null) {
                IdamUserIdResponse idamUserIdDetailsResponse = responseEntity.getBody();
                if (idamUserIdDetailsResponse != null) {
                    return idamUserIdDetailsResponse;
                }
            }
            LOG.error("Parse error user not found");
            throw new UserNotFoundException(USER_DETAILS_NOT_FOUND_ERROR_MSG);
        } catch (HttpClientErrorException e) {
            LOG.error("client err ", e);
            throw new UserNotFoundException(INTERNAL_SERVER_ERROR_MSG);
        } catch (HttpServerErrorException e) {
            LOG.error("server err ", e);
            throw new GatewayTimeoutException("Unable to retrieve User information. Please try again later");
        }
    }

    @Override
    public String getUserDetails(MultiValueMap<String, String> headers) {

//         to test locally
//         return "asdfghjk-kjhgfds-dfghj-sdfghjk";
        try {
            ResponseEntity<IdamUserIdDetailsResponse> responseEntity = getUserDetailsResponseEntity(headers);
            if (responseEntity != null) {
                IdamUserIdDetailsResponse idamUserIdDetailsResponse = responseEntity.getBody();
                if (idamUserIdDetailsResponse != null) {
                    LOG.info("User id from IDAM service {} " ,idamUserIdDetailsResponse.getEmail());
                    return idamUserIdDetailsResponse.getEmail();
                }
            }
            LOG.error("Parse error user not found");
            throw new UserNotFoundException("Internal Server error. Please, try again later");
        } catch (HttpClientErrorException e) {
            LOG.error("client err ", e);
            throw new UserNotFoundException("Internal Server error. Please, try again later");
        } catch (HttpServerErrorException e) {
            LOG.error("server err ", e);
            throw new GatewayTimeoutException("Unable to retrieve User information. Please try again later");
        }
    }

    private ResponseEntity<IdamUserIdResponse> getResponseEntity(MultiValueMap<String, String> headers) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(idamBaseURL + USERID_ENDPOINT);
        LOG.info("builder.toUriString() : {}", builder.toUriString());
        return restTemplateIdam
            .exchange(
                builder.toUriString(),
                HttpMethod.GET,
                getEntity(headers), IdamUserIdResponse.class
            );
    }

    private ResponseEntity<IdamUserIdDetailsResponse> getUserDetailsResponseEntity(MultiValueMap<String, String> headers) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(idamBaseURL + USERDETAILS_ENDPOINT);
        LOG.info("builder.toUriString() : {}", builder.toUriString());
        return restTemplateIdam
            .exchange(
                builder.toUriString(),
                HttpMethod.GET,
                getEntity(headers), IdamUserIdDetailsResponse.class
            );
    }

    private HttpEntity<String> getEntity(MultiValueMap<String, String> headers) {
        MultiValueMap<String, String> headerMultiValueMap = new LinkedMultiValueMap<>();
        headerMultiValueMap.put(
            "Content-Type", List.of("application/json")
        );
        String userAuthorization = headers.get("authorization") == null ? headers.get("Authorization").get(0) : headers.get(
            "authorization").get(0);
        headerMultiValueMap.put(
            "Authorization", Collections.singletonList(userAuthorization.startsWith("Bearer ")
                ? userAuthorization : "Bearer ".concat(userAuthorization))
        );
        HttpHeaders httpHeaders = new HttpHeaders(headerMultiValueMap);
        LOG.error("headers : {}", httpHeaders);
        return new HttpEntity<>(httpHeaders);
    }


    @Override
    public UserIdentityDataDto getUserIdentityData(MultiValueMap<String, String> headers, String uid) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(idamBaseURL + USER_FULL_NAME_ENDPOINT)
            .queryParam("query", "id:" + uid);
        LOG.error("builder.toUriString() : {}", builder.toUriString());

        ResponseEntity<IdamFullNameRetrivalResponse[]> idamFullNameResEntity = restTemplateIdam
            .exchange(
                builder.toUriString(),
                HttpMethod.GET,
                getEntity(headers), IdamFullNameRetrivalResponse[].class
            );


        if (idamFullNameResEntity != null && idamFullNameResEntity.getBody() != null) {
            LOG.error("idamFullNameResEntity body available");
            IdamFullNameRetrivalResponse[] idamArrayFullNameRetrievalResponse = idamFullNameResEntity.getBody();

            if (idamArrayFullNameRetrievalResponse != null && idamArrayFullNameRetrievalResponse.length > 0) {
                IdamFullNameRetrivalResponse idamFullNameRetrivalResponse = idamArrayFullNameRetrievalResponse[0];
                return UserIdentityDataDto.userIdentityDataWith()
                    .emailId(idamFullNameRetrivalResponse.getEmail())
                    .fullName(idamFullNameRetrivalResponse.getForename() + " " + idamFullNameRetrivalResponse.getSurname())
                    .build();
            }
        }

        LOG.error("User details not found for given user id : {}", uid);
        throw new UserNotFoundException("Internal Server error. Please, try again later");
    }
}
