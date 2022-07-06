package uk.gov.hmcts.payment.casepaymentorders.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.casepaymentorders.client.dto.CpoGetResponse;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;

@Component
public class ServiceRequestCpoServiceClient {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceRequestCpoServiceClient.class);

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String AUTHORIZATION = "Authorization";
    protected static final String GET_CPO = "/case-payment-orders";

    private final String url;
    private final RestTemplate restTemplateCpoClient;

    @Autowired
    public ServiceRequestCpoServiceClient(@Value("${case-payment-orders.api.url}") String url,
                                          @Qualifier("restTemplateCpoClient") RestTemplate restTemplateCpoClient) {
        this.url = url;
        this.restTemplateCpoClient = restTemplateCpoClient;
    }

    public CpoGetResponse getCasePaymentOrdersForServiceReq(String caseIds,
                                               String userAuthToken, String s2sToken) {

        LOG.info("Inside getCasePaymentOrdersForServiceReq {}", caseIds);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url + GET_CPO);

        if (StringUtils.isNotBlank(caseIds)) {
            builder.queryParam("case_ids", caseIds);
        }

        HttpHeaders headers = prepareHeaders(userAuthToken, s2sToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            HttpEntity<String> response = restTemplateCpoClient
                .exchange(builder.build().toUriString(),
                          HttpMethod.GET,
                          entity,
                          String.class);

            return cpoObjectMapper().readValue(response.getBody(), CpoGetResponse.class);

        } catch (Exception e) {
            LOG.warn("Error while retrieving Case Payment Orders: " + e.getMessage(), e);
            if (e instanceof HttpClientErrorException
                && HttpStatus.valueOf(((HttpClientErrorException) e).getRawStatusCode()).value() == 400) {
                LOG.error("CPO service Bad Request ::{}",caseIds);
            } else if (e instanceof HttpClientErrorException
                && HttpStatus.valueOf(((HttpClientErrorException) e).getRawStatusCode()).is4xxClientError()) {
                LOG.error("CPO service 4xxClientError ::{}",caseIds);
            } else {
                LOG.error("CPO service internal server error ::{}",caseIds);
            }
        }
        return null;
    }

    private HttpHeaders prepareHeaders(String authorization, String s2sToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(SERVICE_AUTHORIZATION, s2sToken);
        headers.set(AUTHORIZATION, authorization);
        return headers;
    }

    protected ObjectMapper cpoObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        return objectMapper;
    }
}
