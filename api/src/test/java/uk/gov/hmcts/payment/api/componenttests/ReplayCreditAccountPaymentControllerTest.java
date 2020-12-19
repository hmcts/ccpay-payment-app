package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.math.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.CSVUtil;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@TestPropertySource(properties = "duplicate.payment.check.interval.in.minutes=0")
public class ReplayCreditAccountPaymentControllerTest extends PaymentsDataUtil {

    private final static String PAYMENT_REFERENCE_REFEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    protected PaymentDbBackdoor db;

    @Autowired
    protected Payment2Repository paymentRepository;

    @Autowired
    protected AccountService<AccountDto, String> accountService;

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

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
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

        Mockito.reset(accountService);
    }

    @Test
    public void replayCreditAccountPayment_AutomatedTest() throws Exception {
        Map<String, CreditAccountPaymentRequest> csvParseMap = new HashMap<>();

        //Create 10 PBA payments
        createCreditAccountPayments(csvParseMap);

        //Create CSV
        createCSV(csvParseMap);

        //Invoke replay-credit-account-payment
        MockMultipartFile csvFile = new MockMultipartFile("csvFile", "test.csv", "text/csv",
            new FileInputStream(new File("src/test/resources/test.csv")));

        MvcResult result = restActions
            .postWithMultiPartFileData("/replay-credit-account-payments",
                csvFile, "isReplayPBAPayments", "true")
            .andExpect(status().isOk())
            .andReturn();

        /*List<PaymentFee> savedfees = db.findByReference(paymentDto.getPaymentGroupReference()).getFees();
        assertEquals(new BigDecimal(0), savedfees.get(0).getAmountDue());*/

    }

    private void createCSV(Map<String, CreditAccountPaymentRequest> csvParseMap) throws IOException {
        String csvFile = "src/test/resources/test.csv";
        FileWriter writer = new FileWriter(csvFile);

        //for header
        CSVUtil.writeLine
            (writer, Arrays.asList("index_col", "impacted.payment.reference", "payment.amount", "payment.ccd_case_number",
                "payment.pba_number", "payment.description", "payment.case_reference", "payment.service",
                "payment.currency", "payment.customer_reference", "payment.organisation_name", "payment.site_id",
                "fee.code", "fee.calculated_amount", "fee.version"));

        csvParseMap.entrySet().stream().forEach(paymentRequestEntry ->
            {
                CreditAccountPaymentRequest request = paymentRequestEntry.getValue();
                List<String> list = new ArrayList<>();
                list.add("");
                list.add(paymentRequestEntry.getKey());
                list.add(request.getAmount().toString());
                list.add(request.getCcdCaseNumber());
                list.add(request.getAccountNumber());
                list.add(request.getDescription());
                list.add(request.getCaseReference());
                list.add("CMC");
                list.add("GBP");
                list.add(request.getCustomerReference());
                list.add(request.getOrganisationName());
                list.add(request.getSiteId());
                list.add(request.getFees().get(0).getCode());
                list.add(request.getFees().get(0).getCalculatedAmount().toString());
                list.add(request.getFees().get(0).getVersion());

                try {
                    CSVUtil.writeLine(writer, list);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        );

        writer.flush();
        writer.close();
    }

    private void createCreditAccountPayments(Map<String, CreditAccountPaymentRequest> csvParseMap) throws Exception {
        for (int i = 0; i < 10; i++) {
            //create PBA payment
            setCreditAccountPaymentLiberataCheckFeature(true);

            when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);

            Double calculatedAmount = Double.parseDouble(Integer.toString(RandomUtils.nextInt(99)));

            List<FeeDto> fees = getFees(calculatedAmount);

            CreditAccountPaymentRequest request = getPBAPayment(calculatedAmount, fees);

            AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
                new BigDecimal(calculatedAmount), new BigDecimal(calculatedAmount), AccountStatus.ACTIVE, new Date());
            Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

            MvcResult result = restActions
                .post("/credit-account-payments", request)
                .andExpect(status().isCreated())
                .andReturn();

            PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
            csvParseMap.put(paymentDto.getReference(), request);
        }
    }

    private CreditAccountPaymentRequest getPBAPayment(Double calculatedAmount, List<FeeDto> fees) {
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
                .amount(new BigDecimal(calculatedAmount))
                .ccdCaseNumber("1607065536649" + RandomUtils.nextInt(999))
                .accountNumber("\"PBA0073" + RandomUtils.nextInt(999) + "\"")
                .description("Money Claim issue fee")
                .caseReference("\"9eb95270-7fee-48cf-afa2-e6c58ee" + RandomUtils.nextInt(999) + "ba\"")
                .service(Service.CMC)
                .currency(CurrencyCode.GBP)
                .customerReference("DEA2682/1/SWG" + RandomUtils.nextInt(999))
                .organisationName("\"Slater & Gordon" + RandomUtils.nextInt(999) + "\"")
                .siteId("Y689")
                .fees(fees)
                .build();
    }

    @NotNull
    private List<FeeDto> getFees(Double calculatedAmount) {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith()
            .code("FEE020" + RandomUtils.nextInt(9))
            .version(Integer.toString(RandomUtils.nextInt(9)))
            .calculatedAmount(new BigDecimal(calculatedAmount)).build());
        return fees;
    }

    private void setCreditAccountPaymentLiberataCheckFeature(boolean enabled) throws Exception {
        String url = "/api/ff4j/store/features/credit-account-payment-liberata-check/";
        if (enabled) {
            url += "enable";
        } else {
            url += "disable";
        }

        restActions
            .post(url)
            .andExpect(status().isAccepted());
    }
}
