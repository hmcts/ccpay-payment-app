package uk.gov.hmcts.payment.functional;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ResourceUtils;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class IntegrationTestBase {
//    @Value("#{'${base-urls.gov-pay-stubs}'.split(',')}")
//    private List<String> govPayStubUrls;
//
//    private static boolean stubsLoaded = false;
//
//
//    @Before
//    public void loadGovPayStubs() throws FileNotFoundException, MalformedURLException {
//        if (!stubsLoaded) {
//            for (String govPayStubUrl : govPayStubUrls) {
//                URL url = new URL(govPayStubUrl);
//                WireMock wireMock = new WireMock(url.getHost(), url.getPort());
//                wireMock.resetMappings();
//                wireMock.loadMappingsFrom(ResourceUtils.getFile("classpath:gov-pay-stub"));
//            }
//        }
//    }
}
