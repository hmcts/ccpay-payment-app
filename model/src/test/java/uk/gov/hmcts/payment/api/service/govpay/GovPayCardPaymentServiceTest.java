package uk.gov.hmcts.payment.api.service.govpay;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayKeyRepository;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GovPayCardPaymentServiceTest {

    @Mock
    private GovPayKeyRepository govPayKeyRepository;

    @Mock
    private GovPayClient govPayClient;

    @Mock
    private ServiceIdSupplier serviceIdSupplier;

    @InjectMocks
    private GovPayCardPaymentService govPayCardPaymentService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(serviceIdSupplier.get()).thenReturn("divorce");
        String serviceId = serviceIdSupplier.get();

        when(govPayKeyRepository.getKey(serviceId)).thenReturn("divorce-gov-pay-key");
    }

    @Test
    public void createPaymentTest() throws Exception {
        String key = govPayKeyRepository.getKey("divorce");

        CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest(10000, "reference", "description", "https://www.google.com");
        when(govPayClient.createPayment("divorce-gov-pay-key", createPaymentRequest)).thenReturn(GovPayPayment.govPaymentWith()
            .amount(10000)
            .state(new State("created", false, null, null))
            .description("description")
            .reference("reference")
            .paymentId("paymentId")
            .paymentProvider("sandbox")
            .returnUrl("https://www.google.com")
            .build());

        GovPayPayment govPayPayment = govPayCardPaymentService.create(10000, "reference", "description", "https://www.google.com",
            "ccdCaseNumer", "caseReference", "GBP", "siteId", "divorce",
            Arrays.asList(Fee.feeWith().calculatedAmount(new BigDecimal("10000")).code("feeCode").version("1").volume(1)
                .build()));
        assertNotNull(govPayPayment);
        assertEquals(govPayPayment.getAmount(), new Integer(10000));
        assertEquals(govPayPayment.getState().getStatus(), "created");
    }

    @Test
    public void retrieveGovPaymentTest() throws Exception {
        String key = govPayKeyRepository.getKey("divorce");

        when(govPayClient.retrievePayment(key, "RC-1518-9479-8089-4415")).thenReturn(GovPayPayment.govPaymentWith()
            .amount(11199)
            .state(new State("Success", true, "payment expired", "P0020"))
            .description("description")
            .reference("RC-1518-9479-8089-4415")
            .paymentId("ia2mv22nl5o880rct0vqfa7k76")
            .returnUrl("https://www.google.com")
            .build());

        GovPayPayment govPayPayment = govPayCardPaymentService.retrieve("RC-1518-9479-8089-4415");
        assertNotNull(govPayPayment);
        assertEquals(govPayPayment.getPaymentId(), "ia2mv22nl5o880rct0vqfa7k76");
        assertEquals(govPayPayment.getReference(), "RC-1518-9479-8089-4415");
    }

}
