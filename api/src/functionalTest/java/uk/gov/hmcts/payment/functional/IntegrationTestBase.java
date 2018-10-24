package uk.gov.hmcts.payment.functional;


import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Random;

import static uk.gov.hmcts.payment.api.contract.CardPaymentRequest.createCardPaymentRequestDtoWith;
import static uk.gov.hmcts.payment.api.contract.FeeDto.feeDtoWith;

@TestComponent
public class IntegrationTestBase {

    @Value("${probate.user.role}")
    protected String probateUserRole;

    @Value("${probate.user.group}")
    protected String probateUserGroup;

    @Value("${probate.service.name}")
    protected String probateServiceName;

    @Value("${probate.service.secret}")
    protected String probateSecret;

    @Value("${cmc.user.role}")
    protected String cmcUserRole;

    @Value("${cmc.user.group}")
    protected String cmcUserGroup;

    @Value("${cmc.service.name}")
    protected String cmcServiceName;

    @Value("${cmc.service.secret}")
    protected String cmcSecret;

    @Value("${payments.cmc.test.user:cmcuser@domain.com}")
    protected String paymentCmcTestUser;

    @Value("${payments.cmc.test.user.id:148906}")
    protected String paymentCmcTestUserId;

    @Value("${payments.cmc.test.user.password:dummy}")
    protected String paymentCmcTestPassword;

    @Value("payments.account.existing.account.number")
    protected String existingAccountNumber;

    @Value("${payments.account.fake.account.number}")
    protected String fakeAccountNumber;

    public CardPaymentRequest getCMCCardPaymentRequest() {
        int num = new Random().nextInt(100) + 1;

        return createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("20.99"))
            .description("A functional test for search payment " + num)
            .caseReference("REF_" + num)
            .service(Service.CMC)
            .currency(CurrencyCode.GBP)
            .siteId("AA0" + num)
            .fees(Arrays.asList(feeDtoWith()
                .calculatedAmount(new BigDecimal("20.99"))
                .code("FEE0" + num)
                .reference("REF_" + num)
                .version("1")
                .build()))
            .build();

    }

}
