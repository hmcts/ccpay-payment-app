package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the generated OpenAPI specs, as served at {@code /v3/api-docs}.
 */
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@AutoConfigureMockMvc
class SwaggerSpecTest {

    private static final List<String> GROUPS = List.of("payment", "payment2", "payment-external-api", "reference-data");
    private static final Endpoint CANCEL = post("/service-request/{service-request-reference}/cancel");

    // Endpoints that refuse to change a cancelled service request - we will check they doc 409 response
    private static final List<Endpoint> blockedWhenCancelledEndpoints = List.of(
        post("/service-request/{service-request-reference}/pba-payments"),
        post("/service-request/{service-request-reference}/card-payments"),
        post("/payment-groups/{payment-group-reference}/telephony-card-payments"),
        post("/payment-groups/{payment-group-reference}/bulk-scan-payments"),
        post("/payment-groups/{payment-group-reference}/bulk-scan-payments-strategic"),
        put("/payment-groups/{payment-group-reference}"),
        post("/payment-groups/{payment-group-reference}/fees/{unique_fee_id}/remissions"),
        post("/payment-groups/{payment-group-reference}/fees/{unique_fee_id}/retro-remission"),
        delete("/credit-account-payments/{paymentReference}"),
        delete("/fees/{feeId}"),
        patch("/payments/{reference}"),
        patch("/payments/{reference}/status/{status}"),
        patch("/refund/resubmit/{payment-reference}")
    );

    private record Endpoint(HttpMethod method, String path) {
        @Override
        public String toString() {
            return "%s %s".formatted(method, path);
        }
    }

    private final Map<String, JsonNode> specs = new HashMap<>();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void noOperationDeclaresTheSameHeaderTwice() throws Exception {
        SoftAssertions softly = new SoftAssertions();
        for (String group : GROUPS) {
            spec(group).path("paths").properties().forEach(path -> path.getValue().properties().forEach(operation -> {
                Set<String> seen = new HashSet<>();
                List<String> duplicates = new ArrayList<>();
                for (JsonNode parameter : operation.getValue().path("parameters")) {
                    String name = parameter.path("name").asText().toLowerCase();
                    if ("header".equals(parameter.path("in").asText()) && !seen.add(name)) {
                        duplicates.add(name);
                    }
                }
                softly.assertThat(duplicates)
                    .as("%s: %s %s", group, operation.getKey().toUpperCase(), path.getKey())
                    .isEmpty();
            }));
        }
        softly.assertAll();
    }

    @Test
    void operationsThatDoNotDeclareTheAuthHeadersGetBothAsRequired() throws Exception {
        JsonNode operation = operation(get("/cases/{ccdcasenumber}/paymentgroups"));

        assertThat(header(operation, "Authorization").path("required").asBoolean()).isTrue();
        assertThat(header(operation, "ServiceAuthorization").path("required").asBoolean()).isTrue();
    }

    @Test
    void cancelServiceRequestAuthorizationIsOptionalForAnAutomatedCancellation() throws Exception {
        JsonNode authorization = header(operation(CANCEL), "Authorization");

        assertThat(authorization.path("required").asBoolean()).isFalse();
        assertThat(authorization.path("description").asText()).contains("automated cancellation");
    }

    @Test
    void cancelServiceRequestRequiresAServiceToken() throws Exception {
        assertThat(header(operation(CANCEL), "ServiceAuthorization").path("required").asBoolean()).isTrue();
    }

    @Test
    void cancelServiceRequestBodyIsOptionalAndOnlyHasAReason() throws Exception {
        JsonNode requestBody = operation(CANCEL).path("requestBody");

        assertThat(requestBody.path("required").asBoolean()).isFalse();
        assertThat(requestBody.at("/content/application~1json/schema/$ref").asText())
            .isEqualTo("#/components/schemas/ServiceRequestCancelDto");

        JsonNode properties = spec("payment2").at("/components/schemas/ServiceRequestCancelDto/properties");
        assertThat(fieldNames(properties)).containsExactly("reason");
        assertThat(properties.at("/reason/type").asText()).isEqualTo("string");
    }

    @Test
    void cancelServiceRequestDocumentsItsResponses() throws Exception {
        JsonNode responses = operation(CANCEL).path("responses");

        assertThat(fieldNames(responses)).contains("204", "400", "401", "403", "404", "409", "504");
        assertThat(responses.at("/409/description").asText()).isEqualTo("Service request cannot be cancelled");
        assertThat(responses.at("/504/description").asText()).contains("bulk scanning");
    }

    @Test
    void endpointsBlockedWhenTheServiceRequestIsCancelledDocumentTheirConflict() throws Exception {
        // Every operation lists a generic 409 "Conflict" from ControllerExceptionHandler, so check the description.
        SoftAssertions softly = new SoftAssertions();
        for (Endpoint endpoint : blockedWhenCancelledEndpoints) {
            softly.assertThat(operation(endpoint).at("/responses/409/description").asText())
                .as(endpoint.toString())
                .containsIgnoringCase("service request has been cancelled");
        }
        softly.assertAll();
    }

    private static Endpoint get(String path) {
        return new Endpoint(HttpMethod.GET, path);
    }

    private static Endpoint post(String path) {
        return new Endpoint(HttpMethod.POST, path);
    }

    private static Endpoint put(String path) {
        return new Endpoint(HttpMethod.PUT, path);
    }

    private static Endpoint patch(String path) {
        return new Endpoint(HttpMethod.PATCH, path);
    }

    private static Endpoint delete(String path) {
        return new Endpoint(HttpMethod.DELETE, path);
    }

    private JsonNode operation(Endpoint endpoint) throws Exception {
        JsonNode operation = spec("payment2").path("paths").path(endpoint.path())
            .path(endpoint.method().name().toLowerCase());
        assertThat(operation.isMissingNode()).as("%s in the spec", endpoint).isFalse();
        return operation;
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static JsonNode header(JsonNode operation, String name) {
        List<JsonNode> headers = new ArrayList<>();
        for (JsonNode parameter : operation.path("parameters")) {
            if ("header".equals(parameter.path("in").asText()) && name.equalsIgnoreCase(parameter.path("name").asText())) {
                headers.add(parameter);
            }
        }
        assertThat(headers).as("%s headers on %s", name, operation.path("operationId").asText()).hasSize(1);
        return headers.getFirst();
    }

    private JsonNode spec(String group) throws Exception {
        if (!specs.containsKey(group)) {
            String json = mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs?group=" + group))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            specs.put(group, objectMapper.readTree(json));
        }
        return specs.get(group);
    }
}
