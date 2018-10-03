package uk.gov.hmcts.payment.functional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;

import java.math.BigDecimal;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class CMCCardPaymentFunctionalTest {

    @Autowired
    private IntegrationTestBase integrationTestBase;

    @Autowired(required = true)
    private PaymentsTestDsl dsl;

    @Test
    public void createCMCCardPaymentTestShouldReturn201Success() {
        dsl.given().userId(integrationTestBase.paymentCmcTestUser, integrationTestBase.paymentCmcTestUserId, integrationTestBase.paymentCmcTestPassword, integrationTestBase.cmcUserGroup)
            .serviceId(integrationTestBase.cmcServiceName, integrationTestBase.cmcSecret)
            .returnUrl("https://www.google.com")
            .when().createCardPayment(getCardPaymentRequest())
            .then().created(paymentDto -> {
                assertNotNull(paymentDto.getReference());
                assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
        });

    }

    @Test
    public void retrieveCMCCardPaymentTestShouldReturn200Success() {
        final String[] reference = new String[1];

        // create card payment
        dsl.given().userId(integrationTestBase.paymentCmcTestUser, integrationTestBase.paymentCmcTestUserId, integrationTestBase.paymentCmcTestPassword, integrationTestBase.cmcUserGroup)
            .serviceId(integrationTestBase.cmcServiceName, integrationTestBase.cmcSecret)
            .returnUrl("https://www.google.com")
            .when().createCardPayment(getCardPaymentRequest())
            .then().created(savedPayment -> {
                reference[0] = savedPayment.getReference();

                assertNotNull(savedPayment.getReference());
                assertEquals("payment status is properly set", "Initiated", savedPayment.getStatus());
        });


        // retrieve card payment
        PaymentDto paymentDto = dsl.given().userId(integrationTestBase.paymentCmcTestUser, integrationTestBase.paymentCmcTestUserId, integrationTestBase.paymentCmcTestPassword, integrationTestBase.cmcUserGroup)
            .serviceId(integrationTestBase.cmcServiceName, integrationTestBase.cmcSecret)
            .when().getCardPayment(reference[0])
            .then().get();

        assertNotNull(paymentDto);
        assertEquals(paymentDto.getAmount(), new BigDecimal("20.99"));
        assertEquals(paymentDto.getReference(), reference[0]);
        assertEquals(paymentDto.getExternalProvider(), "gov pay");
        assertEquals(paymentDto.getServiceName(), "Civil Money Claims");
        assertEquals(paymentDto.getStatus(), "Initiated");
        paymentDto.getFees().stream().forEach(f -> {
            assertEquals(f.getVersion(), "1");
            assertEquals(f.getCalculatedAmount(), new BigDecimal("20.99"));
        });

    }

    private CardPaymentRequest getCardPaymentRequest() {
        return integrationTestBase.getCMCCardPaymentRequest();
    }

}
