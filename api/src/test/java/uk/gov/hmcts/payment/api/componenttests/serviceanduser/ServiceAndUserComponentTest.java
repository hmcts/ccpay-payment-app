package uk.gov.hmcts.payment.api.componenttests.serviceanduser;

import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.reform.auth.checker.core.service.ServiceRequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.user.UserRequestAuthorizer;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ServiceAndUserComponentTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserResolverBackdoor userResolverBackdoor;

    @Autowired
    private ServiceResolverBackdoor serviceResolverBackdoor;

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    private static final String SERVICE_ID_DIVORCE = "divorce";

    @Test
    public void happyPathResultsIn200() {
        userResolverBackdoor.registerToken("userToken", "1");
        serviceResolverBackdoor.registerToken("serviceToken", SERVICE_ID_DIVORCE);
        HttpEntity<Object> entity = withServiceAndUserHeaders("serviceToken", "userToken");
        ResponseEntity<String> response = restTemplate.exchange("/test", GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo("1@divorce");
    }

    @Test
    public void noAuthorizationHeadersShouldResultIn403() {
        ResponseEntity<String> response = restTemplate.getForEntity("/test", String.class);
        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    public void noUserHeaderAndWrongServiceTokenShouldResultIn403() {
        serviceResolverBackdoor.registerToken("serviceToken", SERVICE_ID_DIVORCE);
        HttpEntity<Object> entity = withServiceAndUserHeaders("unknownServiceToken", null);
        ResponseEntity<String> response = restTemplate.exchange("/test", GET, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    public void noUserHeaderAndCorrectServiceTokenShouldResultIn200() {
        serviceResolverBackdoor.registerToken("serviceToken", SERVICE_ID_DIVORCE);
        HttpEntity<Object> entity = withServiceAndUserHeaders("serviceToken", null);
        ResponseEntity<String> response = restTemplate.exchange("/test", GET, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo("anonymous@divorce");
    }

    @Test
    public void noServiceAuthorizationHeaderShouldResultIn403() {
        serviceResolverBackdoor.registerToken("serviceToken", SERVICE_ID_DIVORCE);
        HttpEntity<Object> entity = withServiceAndUserHeaders("serviceToken", "unknownUserToken");
        ResponseEntity<String> response = restTemplate.exchange("/test", GET, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }



    private HttpEntity<Object> withServiceAndUserHeaders(String serviceToken, String userToken) {
        HttpHeaders headers = new HttpHeaders();

        if (serviceToken != null) {
            headers.add(ServiceRequestAuthorizer.AUTHORISATION, serviceToken);
        }

        if (userToken != null) {
            headers.add(UserRequestAuthorizer.AUTHORISATION, userToken);
        }

        return new HttpEntity<>(headers);
    }
    @SneakyThrows
    String contentsOf(String fileName) {
        String content = new String(Files.readAllBytes(Paths.get(ResourceUtils.getURL("classpath:" + fileName).toURI())));
        return resolvePlaceholders(content);
    }

    String resolvePlaceholders(String content) {
        return configurableListableBeanFactory.resolveEmbeddedValue(content);
    }


}
