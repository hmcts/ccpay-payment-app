package uk.gov.hmcts.payment.api.componenttests.jobs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.payment.api.controllers.MaintenanceJobsController;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CardPaymentService;
import uk.gov.hmcts.payment.api.service.PaymentService;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class MaintenanceJobsControllerMockTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService<PaymentFeeLink, String> paymentService;

    @Mock
    private CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    @InjectMocks
    private MaintenanceJobsController controller;

    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testThatNoPaymentsReturn200() throws Exception{

        // when & then
        this.mockMvc.perform(patch("/jobs/card-payments-status-update"))
            .andExpect(status().isOk());

        verify(paymentService).listCreatedStatusPaymentsReferences();

        verify(cardPaymentService, times(0)).retrieve(any());

    }

    @Test
    public void testThatReturns200WhenPaymentsExist() throws Exception{

        doReturn(Arrays.asList(reference, reference)).when(paymentService).listCreatedStatusPaymentsReferences();

        // when & then
        this.mockMvc.perform(patch("/jobs/card-payments-status-update"))
            .andExpect(status().isOk());

        verify(paymentService).listCreatedStatusPaymentsReferences();

        verify(cardPaymentService, times(2)).retrieve("xxx");

    }

    private Reference reference = new Reference("xxx");

}
