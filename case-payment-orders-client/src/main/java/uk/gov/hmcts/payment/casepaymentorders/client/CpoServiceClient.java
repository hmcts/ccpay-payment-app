package uk.gov.hmcts.payment.casepaymentorders.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.casepaymentorders.client.dto.CpoGetResponse;
import uk.gov.hmcts.payment.casepaymentorders.client.exceptions.CpoBadRequestException;
import uk.gov.hmcts.payment.casepaymentorders.client.exceptions.CpoClientException;
import uk.gov.hmcts.payment.casepaymentorders.client.exceptions.CpoInternalServerErrorException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;

@Component
public class CpoServiceClient {
    private static final Logger LOG = LoggerFactory.getLogger(CpoServiceClient.class);

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization"; // TODO: add this header
    public static final String AUTHORIZATION = "Authorization";
    public static final String GET_CPO = "/api/case-payment-orders";

    private final String url;
    private final RestTemplate restTemplateCpoClient;
    private final AuthTokenGenerator authTokenGenerator;

    @Autowired
    public CpoServiceClient(@Value("${case-payment-orders.api.url}") String url,
                            @Qualifier("restTemplateCpoClient") RestTemplate restTemplateCpoClient,
                            AuthTokenGenerator authTokenGenerator) {
        this.url = url;
        this.restTemplateCpoClient = restTemplateCpoClient;
        this.authTokenGenerator = authTokenGenerator;
    }

    public CpoGetResponse getCasePaymentOrders(String ids, String caseIds, String page, String size,
                                               String authorization) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url + GET_CPO)
            .queryParam("ids", ids)
            .queryParam("caseIds", caseIds)
            .queryParam("page", page)
            .queryParam("size", size);

        HttpHeaders headers = prepareHeaders(authorization);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            HttpEntity<String> response = restTemplateCpoClient
                .exchange(builder.build().toUriString(),
                          HttpMethod.GET,
                          entity,
                          String.class);

            return objectMapper().readValue(response.getBody(), CpoGetResponse.class);

        } catch (Exception e) {
            LOG.warn("Error while retrieving Case Payment Orders: " + e.getMessage(), e);
            if (e instanceof HttpClientErrorException
                && HttpStatus.valueOf(((HttpClientErrorException) e).getRawStatusCode()).value() == 400) {
                throw new CpoBadRequestException(e.getMessage(), e);
            } else if (e instanceof HttpClientErrorException
                && HttpStatus.valueOf(((HttpClientErrorException) e).getRawStatusCode()).is4xxClientError()) {
                throw new CpoClientException(e.getMessage(),
                                             HttpStatus.valueOf(((HttpClientErrorException) e).getRawStatusCode()).value(),
                                             e);
            } else {
                throw new CpoInternalServerErrorException(e.getMessage(), e);
            }
        }
    }

    private HttpHeaders prepareHeaders(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(SERVICE_AUTHORIZATION, authTokenGenerator.generate());
        headers.set(AUTHORIZATION, authorization);
        return headers;
    }

    private ObjectMapper objectMapper() {

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        return objectMapper;
    }
}
