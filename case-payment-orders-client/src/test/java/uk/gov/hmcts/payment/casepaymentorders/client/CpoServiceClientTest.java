package uk.gov.hmcts.payment.casepaymentorders.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.casepaymentorders.client.dto.CasePaymentOrder;
import uk.gov.hmcts.payment.casepaymentorders.client.dto.CpoGetResponse;
import uk.gov.hmcts.payment.casepaymentorders.client.exceptions.CpoBadRequestException;
import uk.gov.hmcts.payment.casepaymentorders.client.exceptions.CpoClientException;
import uk.gov.hmcts.payment.casepaymentorders.client.exceptions.CpoInternalServerErrorException;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.gov.hmcts.payment.casepaymentorders.client.CpoServiceClient.GET_CPO;

@RestClientTest(CpoServiceClientTest.class)
@AutoConfigureWebClient(registerRestTemplate = true)
@ContextConfiguration(classes = TestContextConfiguration.class)
class CpoServiceClientTest {

    public static final String HOST_URL = "http://localhost:4457";
    public static final UUID CPO_ID = UUID.randomUUID();
    public static final long CASE_ID = 12345L;

    @Autowired
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    private CpoServiceClient client;

    @BeforeEach
    public void setUp() {
        this.mockRestServiceServer.reset();

        client = new CpoServiceClient(HOST_URL, restTemplate);
        objectMapper = client.cpoObjectMapper();
    }

    @Test
    public void getCasePaymentOrders() throws Exception {
        String json = this.objectMapper
            .writeValueAsString(createCpoGetResponse());

        this.mockRestServiceServer
            .expect(requestTo(HOST_URL + GET_CPO + "?ids=id1&pageNumber=1&pageSize=2"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        CpoGetResponse result = client.getCasePaymentOrders("id1", "", "1", "2", "authToken", "s2sToken");
        assertEquals(3L, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(CASE_ID, result.getContent().get(0).getCaseId());
        assertEquals(CPO_ID, result.getContent().get(0).getId());
    }

    @Test
    public void getCasePaymentOrdersWithCaseIdsAndNoOtherParameters() throws Exception {
        String json = this.objectMapper
            .writeValueAsString(createCpoGetResponse());

        this.mockRestServiceServer
            .expect(requestTo(HOST_URL + GET_CPO + "?case-ids=caseId1"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        CpoGetResponse result = client.getCasePaymentOrders(null, "caseId1", null, null, "authToken", "s2sToken");
        assertEquals(3L, result.getTotalElements());
    }

    @Test
    public void getCasePaymentOrdersThrowsCpoBadRequestExceptionWhen400Received() {
        this.mockRestServiceServer
            .expect(requestTo(HOST_URL + GET_CPO + "?ids=id1&pageNumber=1&pageSize=2"))
            .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(CpoBadRequestException.class, () -> client
            .getCasePaymentOrders("id1", "", "1", "2", "authToken", "s2sToken"));
    }

    @Test
    public void getCasePaymentOrdersThrowsCpoClientExceptionWhen403Received() {
        this.mockRestServiceServer
            .expect(requestTo(HOST_URL + GET_CPO + "?ids=id1&pageNumber=1&pageSize=2"))
            .andRespond(MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN));

        assertThrows(CpoClientException.class, () -> client
            .getCasePaymentOrders("id1", "", "1", "2", "authToken", "s2sToken"));
    }

    @Test
    public void getCasePaymentOrdersThrowsCpoInternalServerErrorExceptionWhen502Received() {
        this.mockRestServiceServer
            .expect(requestTo(HOST_URL + GET_CPO + "?ids=id1&pageNumber=1&pageSize=2"))
            .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_GATEWAY));

        assertThrows(CpoInternalServerErrorException.class, () -> client
            .getCasePaymentOrders("id1", "", "1", "2", "authToken", "s2sToken"));
    }

    private static CpoGetResponse createCpoGetResponse() {
        CpoGetResponse response = new CpoGetResponse();
        response.setContent(Collections.singletonList(createCasePaymentOrder()));
        response.setTotalElements(3L);
        response.setSize(2);
        response.setNumber(1);
        return response;
    }

    private static CasePaymentOrder createCasePaymentOrder() {
        CasePaymentOrder cpo = new CasePaymentOrder();
        cpo.setCaseId(CASE_ID);
        cpo.setId(CPO_ID);
        return cpo;
    }

    @AfterEach
    public void tearDown() {
        this.mockRestServiceServer.verify();
    }
}
