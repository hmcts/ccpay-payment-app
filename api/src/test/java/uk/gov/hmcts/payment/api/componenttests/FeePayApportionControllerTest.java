package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.controllers.FeePayApportionController;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.servicebus.CallbackServiceImpl;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.model.PaymentFee.feeWith;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest", "mockcallbackservice"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class FeePayApportionControllerTest extends PaymentsDataUtil {

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @InjectMocks
    private FeePayApportionController feePayApportionController;

    @Autowired
    protected PaymentDbBackdoor db;

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService<PaymentFeeLink, String> paymentService;

    @MockBean
    private LaunchDarklyFeatureToggler featureToggler;


    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");
    }

    @Test
    @Transactional
    public void retrieveApportionDetailsWithReferenceWhenDateCreatedIsAfterApportionDate() throws Exception {
        Payment payment = populateCardPaymentToDb("1");
        List<FeePayApportion> feePayApportionList = new ArrayList<>();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .id(1)
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionType("AUTO")
            .feeId(1)
            .feeAmount(BigDecimal.valueOf(100))
            .isFullyApportioned("Y")
            .build();
        feePayApportionList.add(feePayApportion);
        when(paymentService.retrieve(payment.getReference())).thenReturn(payment.getPaymentLink());
        when(paymentService.findByPaymentId(payment.getId())).thenReturn(feePayApportionList);
        MvcResult result = restActions
            .get("/payment-groups/fee-pay-apportion/" + payment.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentGroupDto.class);
        assertNotNull(paymentGroupDto);
        assertThat(paymentGroupDto.getPayments().get(0).getReference()).isEqualTo(payment.getReference());
    }

    @Test
    @Transactional
    public void retrieveApportionDetailsWithReferenceWhenDateCreatedIsAfterApportionDateWithoutFees() throws Exception {
        String paymentReference = "RC-1519-9028-1909-1435";
        Payment payment =populateTelephonyPaymentToDbWithoutFees(paymentReference,false);
        List<FeePayApportion> feePayApportionList = new ArrayList<>();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .id(1)
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionType("AUTO")
            .feeId(1)
            .feeAmount(BigDecimal.valueOf(100))
            .isFullyApportioned("Y")
            .build();
        feePayApportionList.add(feePayApportion);
        when(paymentService.retrieve(payment.getReference())).thenReturn(payment.getPaymentLink());
        when(paymentService.findByPaymentId(payment.getId())).thenReturn(feePayApportionList);
        MvcResult result = restActions
            .get("/payment-groups/fee-pay-apportion/" + payment.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentGroupDto.class);
        assertNotNull(paymentGroupDto);
        assertThat(paymentGroupDto.getPayments().get(0).getReference()).isEqualTo(payment.getReference());
    }

    @Test
    @Transactional
    public void retrieveApportionDetailsWithReferenceWhenDateCreatedIsEqualToApportionDate() throws Exception {
        Payment payment = populateCardPaymentToDb("1");
        List<FeePayApportion> feePayApportionList = new ArrayList<>();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .id(1)
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionType("AUTO")
            .feeId(1)
            .feeAmount(BigDecimal.valueOf(100))
            .isFullyApportioned("Y")
            .build();
        feePayApportionList.add(feePayApportion);
        when(paymentService.retrieve(payment.getReference())).thenReturn(payment.getPaymentLink());
        when(paymentService.findByPaymentId(payment.getId())).thenReturn(feePayApportionList);
        payment.setDateCreated(parseDate("01.06.2020"));
        MvcResult result = restActions
            .get("/payment-groups/fee-pay-apportion/" + payment.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentGroupDto.class);
        assertNotNull(paymentGroupDto);
        assertThat(paymentGroupDto.getPayments().get(0).getReference()).isEqualTo(payment.getReference());
    }

    @Test
    @Transactional
    public void retrieveApportionDetailsWithReferenceWhenDateCreatedIsBeforeApportionDate() throws Exception {
        Payment payment = populateCardPaymentToDb("1");
        List<FeePayApportion> feePayApportionList = new ArrayList<>();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .id(1)
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionType("AUTO")
            .feeId(1)
            .feeAmount(BigDecimal.valueOf(100))
            .isFullyApportioned("Y")
            .build();
        feePayApportionList.add(feePayApportion);
        when(paymentService.retrieve(payment.getReference())).thenReturn(payment.getPaymentLink());
        when(paymentService.findByPaymentId(payment.getId())).thenReturn(feePayApportionList);
        payment.setDateCreated(parseDate("01.05.2020"));
        MvcResult result = restActions
            .get("/payment-groups/fee-pay-apportion/" + payment.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentGroupDto.class);
        assertNotNull(paymentGroupDto);
        assertThat(paymentGroupDto.getPayments().get(0).getReference()).isEqualTo(payment.getReference());
    }

    @Test
    @Transactional
    public void retrieveApportionDetailsWithReferenceWhenFeeIdIsDifferent() throws Exception {
        Payment payment = populateCardPaymentToDb("1");
        List<FeePayApportion> feePayApportionList = new ArrayList<>();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .id(1)
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionType("AUTO")
            .feeId(4)
            .feeAmount(BigDecimal.valueOf(100))
            .isFullyApportioned("Y")
            .build();
        feePayApportionList.add(feePayApportion);
        when(paymentService.retrieve(payment.getReference())).thenReturn(payment.getPaymentLink());
        when(paymentService.findByPaymentId(payment.getId())).thenReturn(feePayApportionList);
        MvcResult result = restActions
            .get("/payment-groups/fee-pay-apportion/" + payment.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentGroupDto.class);
        assertNotNull(paymentGroupDto);
        assertThat(paymentGroupDto.getPayments().get(0).getReference()).isEqualTo(payment.getReference());
    }

    @Test
    @Transactional
    public void retrieveApportionDetailsWithReferenceWhenFeeIdIsSame() throws Exception {
        Payment payment = populateCardPaymentToDb("1");
        List<FeePayApportion> feePayApportionList = new ArrayList<>();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .id(1)
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionType("AUTO")
            .feeId(1)
            .feeAmount(BigDecimal.valueOf(100))
            .isFullyApportioned("Y")
            .build();
        feePayApportionList.add(feePayApportion);
        when(paymentService.retrieve(payment.getReference())).thenReturn(payment.getPaymentLink());
        when(paymentService.findByPaymentId(payment.getId())).thenReturn(feePayApportionList);
        MvcResult result = restActions
            .get("/payment-groups/fee-pay-apportion/" + payment.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentGroupDto.class);
        assertNotNull(paymentGroupDto);
        assertThat(paymentGroupDto.getPayments().get(0).getReference()).isEqualTo(payment.getReference());
    }

    @Test
    @Transactional
    public void retrunEmptyListWhenPaymentIsNotPresent() throws Exception {
        Payment payment = populateCardPaymentToDb("1");
        List<FeePayApportion> feePayApportionList = new ArrayList<>();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .id(1)
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionType("AUTO")
            .feeId(1)
            .feeAmount(BigDecimal.valueOf(100))
            .isFullyApportioned("Y")
            .build();
        feePayApportionList.add(feePayApportion);
        when(paymentService.retrieve(anyString())).thenReturn(payment.getPaymentLink());
        when(paymentService.findByPaymentId(payment.getId())).thenReturn(feePayApportionList);
        MvcResult result = restActions
            .get("/payment-groups/fee-pay-apportion/" + "123")
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentGroupDto.class);
        assertNotNull(paymentGroupDto);
        assertThat(paymentGroupDto.getPayments().get(0).getReference()).isEqualTo(payment.getReference());
    }

    @Test
    public void getting404PaymentNotFoundException() throws Exception {
        String errorMessage = "errorMessage";
        PaymentNotFoundException ex = new PaymentNotFoundException(errorMessage);
        assertEquals(errorMessage, feePayApportionController.notFound(ex));
    }


    private Date parseDate(String date) {
        try {
            return new SimpleDateFormat("dd.MM.yyyy").parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
