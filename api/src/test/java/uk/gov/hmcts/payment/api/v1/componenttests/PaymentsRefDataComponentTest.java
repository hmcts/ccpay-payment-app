package uk.gov.hmcts.payment.api.v1.componenttests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.payment.api.controllers.PaymentReferenceDataController;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
//@DataJpaTest
public class PaymentsRefDataComponentTest extends PaymentsComponentTest {

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    @Autowired
    private ObjectMapper objectMapper;

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);

    @InjectMocks
    private PaymentReferenceDataController paymentReferenceDataController;

    @Mock
    private LegacySiteRepository legacySiteRepository;

    @Mock
    private PaymentStatusRepository paymentStatusRepository;

    @Test
    public void testFindAllPaymentChannels() throws Exception {

        restActions
            .get("/refdata/channels")
            .andExpect(status().isOk())
            .andExpect(body().asListOf(PaymentChannel.class, paymentChannels -> {
                assertThat(paymentChannels).anySatisfy(paymentChannel -> {
                    assertThat(paymentChannel.getName()).isEqualTo("Online");
                });
            }));
    }

    @Test
    public void testFindAllPaymentMethods() throws Exception {

        restActions
            .get("/refdata/methods")
            .andExpect(status().isOk())
            .andExpect(body().asListOf(PaymentMethod.class, paymentMethods -> {
                assertThat(paymentMethods).anySatisfy(paymentMethod -> {
                    assertThat(paymentMethod.getName()).isEqualTo("Card");
                });
            }));
    }

    @Test
    public void testFindAllPaymentProviders() throws Exception {

        restActions
            .get("/refdata/providers")
            .andExpect(status().isOk())
            .andExpect(body().asListOf(PaymentProvider.class, paymentProviders -> {
                assertThat(paymentProviders).anySatisfy(paymentProvider -> {
                    assertThat(paymentProvider.getName()).isEqualTo("GovPay");
                });
            }));
    }

    @Test
    public void testFindAllPaymentStatuses() throws Exception {

        MvcResult mvcResult = restActions
            .get("/refdata/status")
            .andExpect(status().isOk())
            .andReturn();
        List<PaymentStatus> paymentStatuses = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<PaymentStatus>>() {
        });
        PaymentStatus paymentStatus = paymentStatuses.get(0);
        assertEquals(paymentStatus.getName(), "created");
        //Controller Mock Test
        List<PaymentStatus> paymentStatusMock =  Collections.singletonList(new PaymentStatus().CREATED);
        when(paymentStatusRepository.findAll()).thenReturn(paymentStatusMock);
        List<PaymentStatus> paymentStatusMockResponse = paymentReferenceDataController.findAllPaymentStatuses();
        assertEquals(paymentStatusMock, paymentStatusMockResponse);
    }


    @Test
    public void testFindAllLegacySites() throws Exception {
        MvcResult mvcResult = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withAuthorizedService("divorce")
            .get("/refdata/legacy-sites")
            .andExpect(status().isOk())
            .andReturn();
        List<LegacySite> legacySites = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<LegacySite>>() {
        });
        LegacySite legacySite = legacySites.get(0);
        assertEquals(legacySite.getSiteId(), "Y402");
        assertEquals(legacySite.getSiteName(), "Aberdare County Court");
        //Controller Mock Test
        List<LegacySite> legacySiteMock =  Collections.singletonList(new LegacySite("site 2","Site name 1"));
            when(legacySiteRepository.findAll()).thenReturn(legacySiteMock);
            List<LegacySite> legacySiteMockResponse = paymentReferenceDataController.findAllLegacySites();
            assertEquals(legacySiteMock, legacySiteMockResponse);
    }


}
