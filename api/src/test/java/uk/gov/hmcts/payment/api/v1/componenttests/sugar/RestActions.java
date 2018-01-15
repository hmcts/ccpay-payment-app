package uk.gov.hmcts.payment.api.v1.componenttests.sugar;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.auth.checker.service.ServiceRequestAuthorizer;
import uk.gov.hmcts.auth.checker.user.UserRequestAuthorizer;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public class RestActions {
    private final HttpHeaders httpHeaders = new HttpHeaders();
    private final MockMvc mvc;
    private final ServiceResolverBackdoor serviceRequestAuthorizer;
    private final UserResolverBackdoor userRequestAuthorizer;
    private final ObjectMapper objectMapper;

    public RestActions(MockMvc mvc, ServiceResolverBackdoor serviceRequestAuthorizer, UserResolverBackdoor userRequestAuthorizer, ObjectMapper objectMapper) {
        this.mvc = mvc;
        this.serviceRequestAuthorizer = serviceRequestAuthorizer;
        this.userRequestAuthorizer = userRequestAuthorizer;
        this.objectMapper = objectMapper;
    }

    public RestActions withAuthorizedService(String serviceId) {
        String token = UUID.randomUUID().toString();
        serviceRequestAuthorizer.registerToken(token, serviceId);
        httpHeaders.add(ServiceRequestAuthorizer.AUTHORISATION, token);
        return this;
    }

    public RestActions withAuthorizedUser(String userId) {
        String token = UUID.randomUUID().toString();
        userRequestAuthorizer.registerToken(token, userId);
        httpHeaders.add(UserRequestAuthorizer.AUTHORISATION, token);
        return this;
    }

    public ResultActions get(String urlTemplate) {
        return translateException(() -> mvc.perform(MockMvcRequestBuilders.get(urlTemplate)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON)
            .headers(httpHeaders))
        );
    }

    public ResultActions post(String urlTemplate) {
        return post(urlTemplate, null);
    }

    public ResultActions post(String urlTemplate, Object requestBody) {
        return translateException(() -> mvc.perform(MockMvcRequestBuilders.post(urlTemplate)
            .headers(httpHeaders)
            .content(toJson(requestBody))
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON)));
    }

    private String toJson(Object o) {
        return translateException(() -> objectMapper.writeValueAsString(o));
    }

    private <T> T translateException(CallableWithException<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    interface CallableWithException<T> {
        T call() throws Exception;
    }
}
