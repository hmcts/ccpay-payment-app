package uk.gov.hmcts.payment.api.service.govpay;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayKeyRepository;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GovPayDelegatingPaymentServiceTest {

    @Mock
    private GovPayKeyRepository govPayKeyRepository;

    @Mock
    private GovPayClient govPayClient;

    @Mock
    private ServiceIdSupplier serviceIdSupplier;

    @Mock
    private GovPayAuthUtil govPayAuthUtil;

    @InjectMocks
    private GovPayDelegatingPaymentService govPayCardPaymentService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(govPayKeyRepository.getKey("divorce")).thenReturn("divorce-gov-pay-key");
        when(govPayKeyRepository.getKey("ccd")).thenReturn("ccd-gov-pay-key");
    }

    @Test
    public void createPaymentTest() throws Exception {
        when(serviceIdSupplier.get()).thenReturn("divorce");
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
            Arrays.asList(PaymentFee.feeWith().calculatedAmount(new BigDecimal("10000")).code("feeCode").version("1")
                .build()));
        assertNotNull(govPayPayment);
        assertEquals(govPayPayment.getAmount(), new Integer(10000));
        assertEquals(govPayPayment.getState().getStatus(), "created");
    }

    @Test
    public void retrieveGovPaymentTest() throws Exception {
        when(serviceIdSupplier.get()).thenReturn("divorce");
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

    @Test
    public void retrieveWithTargetServiceShouldRetrievePaymentFromAnyServiceTypeWhenOperationalServiceIsTheCaller() {
        String govPayReference = "RC-1518-9479-8089-4416";
        String serviceCaller = "ccd";
        String paymentTargetService = "divorce";
        String paymentId = "ia2mv22nl5o880rct0vqfa7k76";

        when(serviceIdSupplier.get()).thenReturn(serviceCaller);

        String targetServiceKey = govPayKeyRepository.getKey(paymentTargetService);

        when(govPayAuthUtil.getServiceName(serviceCaller, paymentTargetService)).thenReturn(paymentTargetService);
        when(govPayAuthUtil.getServiceToken(paymentTargetService)).thenReturn(targetServiceKey);
        when(govPayClient.retrievePayment(targetServiceKey, govPayReference)).thenReturn(GovPayPayment.govPaymentWith()
            .amount(112233)
            .state(new State("Success", true, "payment expired", "P0020"))
            .description("description")
            .reference(govPayReference)
            .paymentId(paymentId)
            .returnUrl("https://www.google.com")
            .build());

        GovPayPayment govPayPayment = govPayCardPaymentService.retrieve(govPayReference, paymentTargetService);
        assertNotNull(govPayPayment);
        assertEquals(govPayPayment.getPaymentId(), paymentId);
        assertEquals(govPayPayment.getReference(), govPayReference);
    }

}
