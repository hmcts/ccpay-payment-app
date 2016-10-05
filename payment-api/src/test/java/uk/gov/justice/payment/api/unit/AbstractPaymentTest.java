package uk.gov.justice.payment.api.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.PaymentController;
import uk.gov.justice.payment.api.services.PaymentService;

/**
 * Created by zeeshan on 29/09/2016.
 */
public class AbstractPaymentTest {

    public static final int PORT = 9190;
    public static final String URL = "http://localhost:"+PORT+"/payments";
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    PaymentController paymentController = new PaymentController();
    ObjectMapper mapper = new ObjectMapper();
    RestTemplate restTemplate = new RestTemplate();
    ClassLoader classLoader = getClass().getClassLoader();

    @Mock
    PaymentService paymentService;
}
