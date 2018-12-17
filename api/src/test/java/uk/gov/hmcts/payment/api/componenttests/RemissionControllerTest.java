package uk.gov.hmcts.payment.api.componenttests;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class RemissionControllerTest {
    // TODO: create good remission

    // TODO: try creating duplicate remission

    // TODO: cases for not filling out mandatory fields / validation failures
}
