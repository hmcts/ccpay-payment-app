package uk.gov.hmcts.payment.casepaymentorders.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.casepaymentorders.client.dto.CasePaymentOrder;
import uk.gov.hmcts.payment.casepaymentorders.client.dto.CpoGetResponse;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(CpoServiceClientTest.class)
@AutoConfigureWebClient(registerRestTemplate = true)
@ContextConfiguration(classes = TestContextConfiguration.class)
@RunWith(value = BlockJUnit4ClassRunner.class)
public class ServiceRequestCpoServiceClientTest {

    private static final String HOST_URL = "http://localhost:4457";
    private static final UUID CPO_ID = UUID.randomUUID();
    private static final LocalDateTime CREATED_TIMESTAMP = LocalDateTime.of(2020, 3, 13, 10, 0);
    private static final long CASE_ID = 12345L;
    private static final String ACTION = "action1";
    private static final String RESPONDENT = "respondent";
    private static final String ORDER_REFERENCE = "orderRef123";
    private static final String GET_CPO = "/case-payment-orders";
    @Autowired
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    private ServiceRequestCpoServiceClient client;

    @BeforeEach
    public void setUp() {
        this.mockRestServiceServer.reset();

        client = new ServiceRequestCpoServiceClient(HOST_URL, restTemplate);
        objectMapper = client.cpoObjectMapper();
    }

    @Test
    public void getCasePaymentOrders() throws Exception {
        String json = this.objectMapper
            .writeValueAsString(createCpoGetResponse());

        this.mockRestServiceServer
            .expect(requestTo(HOST_URL + GET_CPO + "?case_ids=caseId1"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        CpoGetResponse result = client.getCasePaymentOrdersForServiceReq("caseId1",   "authToken", "s2sToken");
        assertEquals(3L, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(2, result.getSize());
        CasePaymentOrder firstCasePaymentOrder = result.getContent().get(0);
        assertEquals(CPO_ID, firstCasePaymentOrder.getId());
        assertEquals(CREATED_TIMESTAMP, firstCasePaymentOrder.getCreatedTimestamp());
        assertEquals(CASE_ID, firstCasePaymentOrder.getCaseId());
        assertEquals(ACTION, firstCasePaymentOrder.getAction());
        assertEquals(RESPONDENT, firstCasePaymentOrder.getResponsibleParty());
        assertEquals(ORDER_REFERENCE, firstCasePaymentOrder.getOrderReference());
    }

    @Test
    public void getCasePaymentOrdersWithCaseIdsAndNoOtherParameters() throws Exception {
        String json = this.objectMapper
            .writeValueAsString(createCpoGetResponse());

        this.mockRestServiceServer
            .expect(requestTo(HOST_URL + GET_CPO + "?case_ids=caseId1"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        CpoGetResponse result = client.getCasePaymentOrdersForServiceReq("caseId1",   "authToken", "s2sToken");
        assertEquals(3L, result.getTotalElements());
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
        cpo.setId(CPO_ID);
        cpo.setCreatedTimestamp(CREATED_TIMESTAMP);
        cpo.setCaseId(CASE_ID);
        cpo.setAction(ACTION);
        cpo.setResponsibleParty(RESPONDENT);
        cpo.setOrderReference(ORDER_REFERENCE);
        return cpo;
    }

    @AfterEach
    public void tearDown() {
        this.mockRestServiceServer.verify();
    }
}


