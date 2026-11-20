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
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestCancellationErrors;

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
 * Checks the generated OpenAPI specs, as served for each group at {@code /v3/api-docs/{group}}.
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

    /**
     * 401 and 403 responses come from Spring Security, before any controller runs. Their body is empty, or the plain
     * text {@code User does not have a valid role}, so the spec must not give them a JSON schema.
     *
     * <p>An {@code @ApiResponse} without {@code content} gets one anyway: springdoc documents the method's return type
     * for it, so Swagger shows the success response as the example. Declare {@code content = @Content} instead.</p>
     *
     * <p>{@link #KNOWN_AUTH_RESPONSES_WITH_A_BODY} lists the existing cases. A new case fails this test, and so does
     * fixing an existing one, until it is removed from the list.</p>
     */
    @Test
    void authFailuresAreNotDocumentedWithAJsonBody() throws Exception {
        List<String> withABody = new ArrayList<>();
        for (String group : GROUPS) {
            spec(group).path("paths").properties().forEach(path -> path.getValue().properties().forEach(operation -> {
                for (String code : List.of("401", "403")) {
                    JsonNode content = operation.getValue().at("/responses/%s/content".formatted(code));
                    content.forEach(mediaType -> {
                        JsonNode schema = mediaType.path("schema");
                        if (schema.has("$ref") || Set.of("object", "array").contains(schema.path("type").asText())) {
                            withABody.add("%s: %s %s %s".formatted(group, operation.getKey().toUpperCase(),
                                path.getKey(), code));
                        }
                    });
                }
            }));
        }

        assertThat(withABody).containsExactlyInAnyOrderElementsOf(KNOWN_AUTH_RESPONSES_WITH_A_BODY);
    }

    // TODO: These ones are incorrect and need content = @Content (see above).  Fix and remove from this list.
    private static final List<String> KNOWN_AUTH_RESPONSES_WITH_A_BODY = List.of(
        "payment2: GET /accounts/{accountNumber} 401",
        "payment2: GET /accounts/{accountNumber} 403",
        "payment2: GET /card-payments/{reference} 403",
        "payment2: GET /case-payment-orders 401",
        "payment2: GET /case-payment-orders 403",
        "payment2: GET /cases/{ccdcasenumber}/paymentgroups 403",
        "payment2: POST /credit-account-payments 403",
        "payment2: GET /payment-groups/fee-pay-apportion/{paymentreference} 403",
        "payment2: GET /payment-groups/{payment-group-reference} 403",
        "payment2: GET /payments/{reference} 403",
        "payment2: GET /pba-accounts 401",
        "payment2: GET /pba-accounts 403",
        "payment2: GET /refdata/legacy-sites 401",
        "payment2: GET /refunds/payments 403",
        "payment2: POST /service-request 401",
        "payment2: POST /service-request 403",
        "payment2: POST /service-request/{service-request-reference}/card-payments 403",
        "payment2: POST /service-request/{service-request-reference}/pba-payments 403",
        "payment-external-api: GET /pba-accounts 401",
        "payment-external-api: GET /pba-accounts 403",
        "payment-external-api: GET /refunds/payments 403"
    );

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
        assertThat(properties.at("/reason/maxLength").asInt()).isEqualTo(255);
    }

    @Test
    void cancelServiceRequestDocumentsTheReferenceFormat() throws Exception {
        JsonNode reference = pathParameter(operation(CANCEL), "service-request-reference");

        assertThat(reference.at("/schema/pattern").asText()).isEqualTo("^\\d{4}-\\d+$");
        assertThat(reference.path("example").asText()).matches("^\\d{4}-\\d+$");
    }

    @Test
    void cancelServiceRequestDocumentsItsResponses() throws Exception {
        JsonNode responses = operation(CANCEL).path("responses");

        assertThat(fieldNames(responses)).contains("204", "400", "403", "404", "409", "503", "504")
            // Authentication failures are refused with 403, never 401.
            .doesNotContain("401");
        assertThat(responses.at("/403/description").asText()).contains("payments role");
        assertThat(responses.at("/409/description").asText()).startsWith("Service request cannot be cancelled.");
        assertThat(responses.at("/504/description").asText()).contains("bulk scanning");
    }

    /**
     * The cancel endpoint's error messages are a contract with its clients, listed in Appendix A of the Service Request
     * Cancellation LLD. Swagger documents each one as an example. The messages are written out here, rather than taken
     * from ServiceRequestCancellationErrors, so that changing one fails this test and prompts an update to the LLD.
     */
    @Test
    void cancelServiceRequestDocumentsEveryErrorMessage() throws Exception {
        JsonNode responses = operation(CANCEL).path("responses");

        assertThat(exampleValues(responses.path("400"))).containsExactlyInAnyOrder(
            "Invalid service request reference format",
            "Unknown field(s) in request body: comment",
            "Cancellation reason needs to be supplied for manual service request cancellation",
            "Cancellation reason must have a value when a body is supplied",
            "Cancellation reason must not exceed 255 characters");

        assertThat(exampleValues(responses.path("409")))
            .containsExactlyInAnyOrder(
                "Service request has a created payment %s",
                "Service request has a started payment %s",
                "Service request has a successful payment %s",
                "Service request has a pending payment %s",
                "Service request has a submitted payment %s",
                "Service request has an unknown status for payment %s",
                "Service request has a remission",
                "Service request has an allocated payment",
                "Case has an unallocated bulk scan payment")
            // Every reason the eligibility service can give, which ServiceRequestCancellationEligibilityServiceTest
            // checks against the service itself.
            .containsExactlyInAnyOrderElementsOf(ServiceRequestCancellationErrors.INELIGIBLE_REASONS);

        assertThat(exampleValues(responses.path("503")))
            .containsExactly("Unable to check bulk scanning for unallocated payments as no system user is configured");

        assertThat(exampleValues(responses.path("504")))
            .containsExactly("Unable to check bulk scanning for unallocated payments, try again later");
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

    private static List<String> exampleValues(JsonNode response) {
        List<String> values = new ArrayList<>();
        response.at("/content/application~1json/examples").forEach(example -> values.add(example.path("value").asText()));
        return values;
    }

    private static JsonNode pathParameter(JsonNode operation, String name) {
        for (JsonNode parameter : operation.path("parameters")) {
            if ("path".equals(parameter.path("in").asText()) && name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        throw new AssertionError("no path parameter %s on %s".formatted(name, operation.path("operationId").asText()));
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
            // springdoc serves each group at its own path
            String json = mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs/{group}", group))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            specs.put(group, objectMapper.readTree(json));
        }
        return specs.get(group);
    }
}
