package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.domain.service.CaseDetailsDomainService;
import uk.gov.hmcts.payment.api.domain.service.FeeDomainService;
import uk.gov.hmcts.payment.api.domain.service.PaymentDomainService;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
public class FeePayApportionControllerTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @MockBean
    private CaseDetailsDomainService caseDetailsDomainService;

    @MockBean
    private PaymentDomainService paymentDomainService;

    @MockBean
    private FeeDomainService feeDomainService;


    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    @MockBean
    private PaymentService<PaymentFeeLink, String> paymentService;

    @MockBean
    private LaunchDarklyFeatureToggler featureToggler;

    @MockBean
    private PaymentFeeRepository paymentFeeRepository;

    @Before
    public void setup(){
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");
    }

    @Test
    public void testRetrieveApportionDetails() throws Exception {
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
                                            .feeId(123)
                                            .feeAmount(BigDecimal.valueOf(100))
                                            .build();
        List<FeePayApportion> feePayApportionList= new ArrayList<>();
        feePayApportionList.add(feePayApportion);
        PaymentFee paymentFee = PaymentFee.feeWith()
                                    .code("FEE123")
                                    .feeAmount(BigDecimal.valueOf(100))
                                    .build();
        when(paymentService.retrieve(anyString())).thenReturn(getPaymentFeeLink());
        when(featureToggler.getBooleanValue("apportion-feature",false)).thenReturn(true);
        when(paymentService.findByPaymentId(anyInt())).thenReturn(feePayApportionList);
        when(paymentFeeRepository.findById(anyInt())).thenReturn(java.util.Optional.ofNullable(paymentFee));
        MvcResult result = restActions
            .get("/payment-groups/fee-pay-apportion/RC-1234-1234-1234-1234")
            .andExpect(status().isOk())
            .andReturn();
//        PaymentGroupDto paymentGroupDto  = objectMapper.readValue(result.getResponse().getContentAsString(),PaymentGroupDto.class);
//        assertEquals("RC-1234-1234-1234-1234",paymentGroupDto.getPaymentGroupReference());
    }

    @Test
    public void testRetrieveApportionDetailsThrowsException_WhenPaymentNotFound() throws Exception {
        when(paymentService.retrieve(anyString())).thenThrow(PaymentNotFoundException.class);
        MvcResult result = restActions
            .get("/payment-groups/fee-pay-apportion/RC-1519-9028-2432-0001")
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void testRetrieveApportionDetailsForOrders() throws Exception {
        Mockito.when(paymentDomainService.getPaymentByReference(anyString())).thenReturn(getPayment());
        Mockito.when(paymentDomainService.getFeePayApportionByPaymentId(anyInt())).thenReturn(Arrays.asList(getFeePayApportion()));
        when(feeDomainService.getPaymentFeeById(anyInt())).thenReturn(getPaymentFee());
        MvcResult result = restActions
            .get("/orderpoc/payment-groups/fee-pay-apportion/RC-1603-1374-9345-6197")
            .andExpect(status().isOk())
            .andReturn();
        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsString(),PaymentGroupDto.class);
        assertEquals(paymentGroupDto.getPayments().get(0).getCcdCaseNumber(),"ccdCaseNumber");
        assertEquals(paymentGroupDto.getPayments().get(0).getAmount(),new BigDecimal("99.99"));
    }

    @Test
    public void testRetrieveApportionDetailsForOrdersWithEmptyApportions() throws Exception {
        Mockito.when(paymentDomainService.getPaymentByReference(anyString())).thenReturn(getPayment());
        Mockito.when(paymentDomainService.getFeePayApportionByPaymentId(anyInt())).thenReturn(Collections.emptyList());
        MvcResult result = restActions
            .get("/orderpoc/payment-groups/fee-pay-apportion/RC-1603-1374-9345-6197")
            .andExpect(status().isOk())
            .andReturn();
        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsString(),PaymentGroupDto.class);
        assertNull(paymentGroupDto.getFees());
    }

    private PaymentFeeLink getPaymentFeeLink(){
        List<PaymentFee> paymentFees = new ArrayList<>();
        PaymentFee fee = PaymentFee.feeWith()
            .feeAmount(BigDecimal.valueOf(30))
            .calculatedAmount(BigDecimal.valueOf(10))
            .code("FEE-123")
            .build();
        paymentFees.add(fee);
        Payment payment = Payment.paymentWith()
            .paymentStatus(PaymentStatus.SUCCESS)
            .status("success")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("card").build())
            .currency("GBP")
            .caseReference("case-reference")
            .ccdCaseNumber("ccd-number")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("cash").build())
            .dateCreated(Date.valueOf("2020-02-01"))
            .externalReference("external-reference")
            .reference("RC-1234-1234-1234-1234")
            .build();
        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);
        return PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("RC-1234-1234-1234-1234")
            .dateCreated(Date.valueOf("2020-01-20"))
            .dateUpdated(Date.valueOf("2020-01-21"))
            .fees(paymentFees)
            .payments(paymentList)
            .build();
    }

    private Payment getPayment(){
        return Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA09")
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1603-1374-9345-6197")
            .build();
    }

    private FeePayApportion getFeePayApportion(){
        return FeePayApportion.feePayApportionWith()
            .apportionAmount(new BigDecimal("99.99"))
            .apportionType("AUTO")
            .feeId(1)
            .paymentId(1)
            .feeAmount(new BigDecimal("99.99"))
            .paymentId(1)
            .build();
    }

    private PaymentFee getPaymentFee(){
        return PaymentFee.feeWith()
            .calculatedAmount(new BigDecimal("99.99"))
            .version("1").code("FEE0001").volume(1).build();
    }
}
