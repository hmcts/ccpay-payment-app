package uk.gov.justice.payment.api.unit;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.ClassRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.payment.api.configuration.GovPayConfig;
import uk.gov.justice.payment.api.controllers.PaymentController;
import uk.gov.justice.payment.api.services.PaymentService;

import java.io.FileNotFoundException;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Created by zeeshan on 29/09/2016.
 */
public class AbstractPaymentTest {

    public static final int PORT = 9190;
    public static final String URL = "http://localhost:" + PORT + "/payments";
    public static final String SERVICE_ID = "test-service-id";

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(PORT);

    PaymentController paymentController = new PaymentController(null, null, null, null, null);
    ClassLoader classLoader = getClass().getClassLoader();

    @Mock
    PaymentService paymentService;

    @Mock
    GovPayConfig govPayConfig;
    @Mock
    Map<String, String> keys;

    public void setUp() throws FileNotFoundException {
        MockitoAnnotations.initMocks(this);

        when(govPayConfig.getKey()).thenReturn(keys);
        when(keys.get(SERVICE_ID)).thenReturn(SERVICE_ID);
        when(keys.containsKey(SERVICE_ID)).thenReturn(true);

        ReflectionTestUtils.setField(paymentController, "govPayConfig", govPayConfig);
    }

    @After
    public void cleanUp() {
        //wireMockRule.stop();
    }
}
