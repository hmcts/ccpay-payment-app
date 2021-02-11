package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.payment.api.controllers.CreditAccountPaymentController;
import uk.gov.hmcts.payment.api.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CreditAccountPaymentControllerMockTest {

    private MockMvc mockMvc;

    @Mock
    private CreditAccountPaymentService creditAccountPaymentService;

    @Spy
    private CreditAccountDtoMapper creditAccountDtoMapper;

    @InjectMocks
    private CreditAccountPaymentController creditAccountPaymentController;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(creditAccountPaymentController).build();
    }


    @Test
    public void retrieveCreditAccountPayment_byReferenceTest() throws Exception {
        Payment payment = Payment.paymentWith()
            .id(1)
            .amount(new BigDecimal("11.99"))
            .caseReference("caseReference")
            .description("retrieve payment mock test")
            .serviceType("Civil Money Claims")
            .siteId("siteID")
            .currency("GBP")
            .organisationName("organisationName")
            .customerReference("customerReference")
            .pbaNumber("pbaNumer")
            .reference("RC-1520-2505-0381-8145")
            .paymentStatus(PaymentStatus.paymentStatusWith().name("pending").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .build();
        PaymentFee fee = PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build();
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .paymentReference("2018-15202505035")
            .payments(Arrays.asList(payment))
            .fees(Arrays.asList(fee))
            .build();

        when(creditAccountPaymentService.retrieveByPaymentReference("RC-1520-2505-0381-8145")).thenReturn(paymentFeeLink);
        payment.setPaymentLink(paymentFeeLink);

        this.mockMvc.perform(get("/credit-account-payments/RC-1520-2505-0381-8145"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount", is(11.99)))
            .andExpect(jsonPath("$.case_reference", is("caseReference")))
            .andExpect(jsonPath("$.reference", is("RC-1520-2505-0381-8145")));

    }


}
