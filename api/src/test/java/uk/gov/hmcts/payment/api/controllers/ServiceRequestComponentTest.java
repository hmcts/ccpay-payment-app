package uk.gov.hmcts.payment.api.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestCancelDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
public class ServiceRequestComponentTest {

    private static final String USER_ID = UserResolverBackdoor.CITIZEN_ID;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @MockitoBean
    // Reference-data is mocked because cancellation does not use it. The domain service, controller,
    // repository, Liquibase schema and database are real in this component test.
    private ReferenceDataService referenceDataService;

    private RestActions restActions;

    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper)
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID);

        when(referenceDataService.getOrganisationalDetail(any(), any(), any())).thenReturn(null);
    }

    @Test
    public void cancelServiceRequestUpdatesTheDatabase() throws Exception {
        // Unlike ServiceRequestControllerTest, this test does not mock ServiceRequestDomainService.
        String serviceRequestReference = "2026-" + UUID.randomUUID().toString().replace("-", "").substring(0, 11);
        String reason = "The service request was created in error";

        paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(serviceRequestReference)
            .build());

        ServiceRequestCancelDto cancellation = ServiceRequestCancelDto.cancelDtoWith()
            .reason(reason)
            .build();

        restActions
            .post("/service-request/" + serviceRequestReference + "/cancel", cancellation)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());

        PaymentFeeLink persisted = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow();

        assertTrue(persisted.isCancelled());
        assertEquals(reason, persisted.getCancellationReason());
        assertEquals("TODO", persisted.getCancelledBy());
        assertNotNull(persisted.getDateCancelled());
    }
}
