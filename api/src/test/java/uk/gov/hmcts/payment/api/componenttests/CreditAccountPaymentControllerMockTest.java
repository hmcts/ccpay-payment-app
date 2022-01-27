package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.payment.api.controllers.CreditAccountPaymentController;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @After
    public void cleanup() {
        this.mockMvc = null;
    }

    @Test
    public void retrieveCreditAccountPayment_byReferenceTest() throws Exception {
        BigDecimal amount = new BigDecimal("11.99");
        Payment payment = Payment.paymentWith()
            .id(1)
            .amount(amount)
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
        PaymentFee fee = PaymentFee.feeWith().id(1).calculatedAmount(amount).code("X0001").version("1").build();
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
