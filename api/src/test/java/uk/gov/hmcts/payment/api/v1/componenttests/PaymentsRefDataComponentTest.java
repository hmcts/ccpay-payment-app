package uk.gov.hmcts.payment.api.v1.componenttests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.payment.api.controllers.PaymentReferenceDataController;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PaymentsRefDataComponentTest extends PaymentsComponentTest {

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    @MockBean
    private LegacySiteRepository legacySiteRepository;

    @InjectMocks
    private PaymentReferenceDataController paymentReferenceDataController;

    @Autowired
    private ObjectMapper objectMapper;

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

        restActions
            .get("/refdata/status")
            .andExpect(status().isOk())
            .andExpect(body().asListOf(PaymentStatus.class, paymentStatuses -> {
                assertThat(paymentStatuses).anySatisfy(paymentStatus -> {
                    assertThat(paymentStatus.getName()).isEqualTo("Pending");
                });
            }));
    }

    @Test
    public void testFindAllLegacySites() throws Exception {
        LegacySite legacySiteExpected = new LegacySite("site1", "site name 2");
        LegacySite legacySiteActual = new LegacySite("site1", "site name 3");
        when(legacySiteRepository.findAll()).thenReturn(Lists.newArrayList(legacySiteActual));
        MvcResult mvcResult = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withAuthorizedService("divorce")
            .get("/refdata/legacy-sites")
            .andExpect(status().isOk())
            .andReturn();
        List<LegacySite> legacySites = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<LegacySite>>() {});
        LegacySite mockLegacySite = legacySites.get(0);
        assertEquals(mockLegacySite.getSiteId(),legacySiteExpected.getSiteId());
        assertNotEquals(mockLegacySite.getSiteName(),legacySiteExpected.getSiteName());
    }


}
