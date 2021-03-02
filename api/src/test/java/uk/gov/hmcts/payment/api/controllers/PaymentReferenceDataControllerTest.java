package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class PaymentReferenceDataControllerTest {

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @MockBean
    private LegacySiteRepository legacySiteRepository;

    @MockBean
    private PaymentStatusRepository paymentStatusRepository;

    @MockBean
    private PaymentProviderRepository paymentProviderRepository;

    @MockBean
    private PaymentMethodRepository paymentMethodRepository;

    @MockBean
    private PaymentChannelRepository paymentChannelRepository;

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;


    @Before
    public void setup(){
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

    }

    @Test
    public void testFindAllPaymentChannels() throws Exception {
        PaymentChannel paymentChannel = PaymentChannel.paymentChannelWith()
                                            .name("online")
                                            .description("online payments").build();
        List<PaymentChannel> paymentChannels = new ArrayList<>();
        paymentChannels.add(paymentChannel);
        when(paymentChannelRepository.findAll()).thenReturn(paymentChannels);
        MvcResult result = restActions
                            .get("/refdata/channels")
                            .andExpect(status().isOk())
                            .andReturn();
        List<LinkedHashMap> actualResponse = objectMapper.readValue(result.getResponse().getContentAsString(),List.class);
        assertEquals("online",actualResponse.get(0).get("name"));
    }

    @Test
    public void testFindAllPaymentMethods() throws Exception {
        PaymentMethod paymentMethod = PaymentMethod.paymentMethodWith()
                                            .name("payment-method-1")
                                            .description("payment-description")
                                            .build();
        List<PaymentMethod> paymentMethodList = new ArrayList<>();
        paymentMethodList.add(paymentMethod);
        when(paymentMethodRepository.findAll()).thenReturn(paymentMethodList);
        MvcResult result = restActions
                            .get("/refdata/methods")
                            .andExpect(status().isOk())
                            .andReturn();
        List<LinkedHashMap> actualResponse = objectMapper.readValue(result.getResponse().getContentAsString(),List.class);
        assertEquals("payment-method-1",actualResponse.get(0).get("name"));
    }

    @Test
    public void testFindAllPaymentProviders() throws Exception {
        PaymentProvider paymentProvider = PaymentProvider.paymentProviderWith()
                                                .name("payment-provider-name")
                                                .description("description")
                                                .build();
        List<PaymentProvider> paymentProviders = new ArrayList<>();
        paymentProviders.add(paymentProvider);
        when(paymentProviderRepository.findAll()).thenReturn(paymentProviders);
        MvcResult result = restActions
            .get("/refdata/providers")
            .andExpect(status().isOk())
            .andReturn();
        List<LinkedHashMap> actualResponse = objectMapper.readValue(result.getResponse().getContentAsString(),List.class);
        assertEquals("payment-provider-name",actualResponse.get(0).get("name"));
    }

    @Test
    public void testFindAllPaymentStatuses() throws Exception {
        PaymentStatus paymentStatus = PaymentStatus.paymentStatusWith()
                                                .name("success")
                                                .description("successful payment")
                                                .build();
        List<PaymentStatus> paymentStatusList = new ArrayList<>();
        paymentStatusList.add(paymentStatus);
        when(paymentStatusRepository.findAll()).thenReturn(paymentStatusList);
        MvcResult result = restActions
            .get("/refdata/status")
            .andExpect(status().isOk())
            .andReturn();
        List<LinkedHashMap> actualResponse = objectMapper.readValue(result.getResponse().getContentAsString(),List.class);
        assertEquals("success",actualResponse.get(0).get("name"));
    }

    @Test
    public void testFindAllLegacySites() throws Exception {
        LegacySite legacySite = LegacySite.legacySiteWith()
                                    .siteId("legacy-site-id")
                                    .siteName("legacy-site-name")
                                    .build();
        List<LegacySite> legacySites = new ArrayList<>();
        legacySites.add(legacySite);
        when(legacySiteRepository.findAll()).thenReturn(legacySites);
        MvcResult result = restActions
            .get("/refdata/legacy-sites")
            .andExpect(status().isOk())
            .andReturn();
        List<LinkedHashMap> actualResponse = objectMapper.readValue(result.getResponse().getContentAsString(),List.class);
        assertEquals("legacy-site-id",actualResponse.get(0).get("siteId"));


    }

}
