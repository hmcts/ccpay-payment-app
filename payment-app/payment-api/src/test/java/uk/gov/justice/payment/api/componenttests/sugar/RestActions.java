package uk.gov.justice.payment.api.componenttests.sugar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public class RestActions {
    public static final String SERVICE_ID = "divorce";

    private final MockMvc mvc;
    private final ObjectMapper objectMapper;

    public RestActions(MockMvc mvc, ObjectMapper objectMapper) {
        this.mvc = mvc;
        this.objectMapper = objectMapper;
    }

    public ResultActions get(String urlTemplate) {
        return get(urlTemplate, new HttpHeaders());
    }

    public ResultActions get(String urlTemplate, HttpHeaders httpHeaders) {
        if (!httpHeaders.containsKey("service_id")) {
            httpHeaders.add("service_id", SERVICE_ID);
        }

        return translateException(() -> mvc.perform(MockMvcRequestBuilders.get(urlTemplate)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .headers(httpHeaders))
        );
    }

    public ResultActions post(String urlTemplate) {
        return post(urlTemplate, null);
    }

    private ResultActions post(String urlTemplate, Object requestBody) {
        return post(urlTemplate, requestBody, new HttpHeaders());
    }

    private ResultActions post(String urlTemplate, Object requestBody, HttpHeaders httpHeaders) {
        return translateException(() -> mvc.perform(MockMvcRequestBuilders.post(urlTemplate)
                .headers(httpHeaders)
                .content(toJson(requestBody))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("service_id", "divorce")));
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
