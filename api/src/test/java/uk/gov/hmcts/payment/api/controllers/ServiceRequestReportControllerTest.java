package uk.gov.hmcts.payment.api.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.DuplicateServiceRequestDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class ServiceRequestReportControllerTest {

    private static final String USER_ID = UserResolverBackdoor.CITIZEN_ID;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private EmailService emailService;

    private RestActions restActions;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    ServiceRequestDomainService serviceRequestDomainService;

    @MockBean
    PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Before
    public void setup() {

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

        DuplicateServiceRequestDto duplicateServiceRequestDto1 = new DuplicateServiceRequestDto() {
            @Override
            public String getFee_codes() {
                return "FEE0123";
            }

            @Override
            public Integer getPayment_link_id() {
                return 123;
            }

            @Override
            public String getCcd_case_number() {
                return "123";
            }

            @Override
            public String getEnterprise_service_name() {
                return "Probate";
            }
        };

        DuplicateServiceRequestDto duplicateServiceRequestDto2 = new DuplicateServiceRequestDto() {
            @Override
            public String getFee_codes() {
                return "FEE0123";
            }

            @Override
            public Integer getPayment_link_id() {
                return 124;
            }

            @Override
            public String getCcd_case_number() {
                return "123";
            }

            @Override
            public String getEnterprise_service_name() {
                return "Probate";
            }
        };

        List<DuplicateServiceRequestDto> duplicateServiceRequestDtos = new ArrayList<>();
        duplicateServiceRequestDtos.add(duplicateServiceRequestDto1);
        duplicateServiceRequestDtos.add(duplicateServiceRequestDto2);

        when(paymentFeeLinkRepository.getDuplicates(any(LocalDate.class))).thenReturn(Optional.of(duplicateServiceRequestDtos));

    }

    @Test
    public void generateAndEmailDuplicateSRReportSuccess() throws Exception {
        MvcResult result = restActions
            .post("/jobs/email-duplicate-sr-report?date=2023-10-01")
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(result.getResponse().getContentAsString());
        verify(emailService, times(1)).sendEmail(any());
    }

    @Test
    public void generateAndEmailDuplicateSRReportInvalidDate() throws Exception {
        MvcResult result = restActions
            .post("/jobs/email-duplicate-sr-report?date=invalid-date")
            .andExpect(status().isBadRequest())
            .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Error occurred"));
    }

    @Test
    public void generateAndEmailDuplicateSRReportMissingDate() throws Exception {
        MvcResult result = restActions
            .post("/jobs/email-duplicate-sr-report")
            .andExpect(status().isBadRequest())
            .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Required request parameter 'date' for method parameter type String is not present"));
    }

}
