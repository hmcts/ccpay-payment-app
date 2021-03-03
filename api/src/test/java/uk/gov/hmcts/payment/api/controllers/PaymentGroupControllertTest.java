package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.http.MethodNotSupportedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.BulkScanPaymentRequest;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.PciPalPaymentRequest;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;
import uk.gov.hmcts.payment.api.service.PciPalPaymentService;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class PaymentGroupControllertTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    private RestActions restActions;

    private static final String USER_ID = UserResolverBackdoor.CITIZEN_ID;

    @MockBean
    private SiteService<Site, String> siteServiceMock;

    @MockBean
    private PaymentGroupService<PaymentFeeLink, String> paymentGroupService;

    @MockBean
    private DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @MockBean
    private ReferenceUtil referenceUtil;

    @MockBean
    private PciPalPaymentService pciPalPaymentService;

    @MockBean
    private FeePayApportionService feePayApportionService;

    @Before
    public void setUp() throws CheckDigitException, MethodNotSupportedException {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

        List<Site> serviceReturn = Arrays.asList(Site.siteWith()
                .sopReference("sop")
                .siteId("AA99")
                .name("name")
                .service("service")
                .id(1)
                .build(),
            Site.siteWith()
                .sopReference("sop")
                .siteId("AA001")
                .name("name")
                .service("service")
                .id(1)
                .build()
        );

        when(siteServiceMock.getAllSites()).thenReturn(serviceReturn);
        List<PaymentFee> paymentFees = new ArrayList<>();
        List<PaymentFee> addFeeList = new ArrayList<>();
        PaymentFee fee = PaymentFee.feeWith()
            .feeAmount(BigDecimal.valueOf(30))
            .code("FEE-123")
            .build();
        PaymentFee fee1 = PaymentFee.feeWith()
            .feeAmount(BigDecimal.valueOf(30))
            .code("FEE-987")
            .build();
        paymentFees.add(fee);
        addFeeList.add(fee);
        addFeeList.add(fee1);

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2021-1614709196068")
            .dateCreated(Date.valueOf("2020-01-20"))
            .dateUpdated(Date.valueOf("2020-01-21"))
            .fees(paymentFees)
            .build();
        PaymentFeeLink paymentFeeLinkForAddFee = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2021-1614709196068")
            .dateCreated(Date.valueOf("2020-01-20"))
            .dateUpdated(Date.valueOf("2020-01-21"))
            .fees(addFeeList)
            .build();
        when(paymentGroupService.findByPaymentGroupReference(anyString())).thenReturn(paymentFeeLink);
        when(paymentGroupService.addNewFeeWithPaymentGroup(any(PaymentFeeLink.class))).thenReturn(paymentFeeLink);
        when(paymentGroupService.addNewFeetoExistingPaymentGroup(anyList(),anyString())).thenReturn(paymentFeeLinkForAddFee);


    }

    @Test
    public void testRetrievePayment() throws Exception {

        MvcResult result = restActions
            .get("/payment-groups/2021-1614709196068")
            .andExpect(status().isOk())
            .andReturn();
        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentGroupDto.class);
        assertEquals("2021-1614709196068",paymentGroupDto.getPaymentGroupReference());
    }

    @Test
    public void testRetrievePaymentReturnsPaymentNotFound() throws Exception {
        when(paymentGroupService.findByPaymentGroupReference(anyString())).thenThrow(PaymentNotFoundException.class);
        MvcResult result = restActions
            .get("/payment-groups/2021-1614709196068")
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testAddNewFee() throws Exception {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE-123").ccdCaseNumber("ccdCaseNumber").feeAmount(new BigDecimal(30))
            .volume(1).version("1").calculatedAmount(new BigDecimal(30)).build());

        PaymentGroupDto request = PaymentGroupDto.paymentGroupDtoWith()
            .fees(fees)
            .build();
        MvcResult result = restActions
            .post("/payment-groups", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentGroupDto.class);
        assertEquals("2021-1614709196068",paymentGroupDto.getPaymentGroupReference());
    }

    @Test
    public void testAddNewFeeWithNullCcdnumber_ThrowsException() throws Exception {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE-123").feeAmount(new BigDecimal(30))
            .volume(1).version("1").calculatedAmount(new BigDecimal(30)).build());

        PaymentGroupDto request = PaymentGroupDto.paymentGroupDtoWith()
            .fees(fees)
            .build();
        MvcResult result = restActions
            .post("/payment-groups", request)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testAddNewFeeToPaymentGroupDto() throws Exception {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE-987").ccdCaseNumber("ccdCaseNumber").feeAmount(new BigDecimal(30))
            .volume(1).version("1").calculatedAmount(new BigDecimal(30)).build());
        PaymentGroupDto request = PaymentGroupDto.paymentGroupDtoWith()
            .fees(fees)
            .build();

        MvcResult result = restActions
            .put("/payment-groups/2021-1614709196068", request)
            .andExpect(status().isOk())
            .andReturn();
        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentGroupDto.class);
        assertEquals(2,paymentGroupDto.getFees().size());
        assertEquals("FEE-987",paymentGroupDto.getFees().get(1).getCode());
    }

    @Test
    public void testAddNewFeeSendsBadRequestWhenCcdNumberisEmpty() throws Exception {
        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE-987").feeAmount(new BigDecimal(30))
            .volume(1).version("1").calculatedAmount(new BigDecimal(30)).build());
        PaymentGroupDto request = PaymentGroupDto.paymentGroupDtoWith()
            .fees(fees)
            .build();

        MvcResult result = restActions
            .put("/payment-groups/2021-1614709196068", request)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void  testCreateCardPayment() throws Exception {
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("120"))
            .ccdCaseNumber("ccdCaseNumber")
            .currency(CurrencyCode.GBP)
            .description("A test card payment")
            .service(Service.DIVORCE)
            .siteId("AA007")
            .build();
        PaymentFeeLink paymentFeeLink = getPaymentFeeLink(false);
        when(referenceUtil.getNext(anyString())).thenReturn("RC-1614-7137-1374-2371");
        when(delegatingPaymentService.update(any(PaymentServiceRequest.class))).thenReturn(paymentFeeLink);
        MvcResult result = restActions
            .post("/payment-groups/2021-1614709196068/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();
        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentGroupDto.class);
        assertEquals("2021-1614709196068",paymentGroupDto.getPaymentGroupReference());
    }

    @Test
    public void  testCreateTelephonyCardPayment() throws Exception {
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("120"))
            .ccdCaseNumber("ccdCaseNumber")
            .currency(CurrencyCode.GBP)
            .channel("telephony")
            .provider("pci pal")
            .description("A test telephony payment")
            .service(Service.DIVORCE)
            .siteId("AA007")
            .build();
        PaymentFeeLink paymentFeeLink = getPaymentFeeLink(true);
        when(referenceUtil.getNext(anyString())).thenReturn("RC-1614-7137-1374-2371");
        when(delegatingPaymentService.update(any(PaymentServiceRequest.class))).thenReturn(paymentFeeLink);
        when(pciPalPaymentService.getPciPalLink(any(PciPalPaymentRequest.class), anyString())).thenReturn("http://mock.hmcts.net");
        MvcResult result = restActions
            .post("/payment-groups/2021-1614709196068/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();
        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentGroupDto.class);
        assertEquals("2021-1614709196068",paymentGroupDto.getPaymentGroupReference());
    }


    @Test
    public void testRecordBulkScanPayments() throws Exception {
        BulkScanPaymentRequest bulkScanPaymentRequest = getBulkScanPaymentRequest();
        PaymentFeeLink paymentFeeLink = getPaymentFeeLink(false);
        when(referenceUtil.getNext(anyString())).thenReturn("RC-1614-7137-1374-2371");
        when(paymentGroupService.addNewPaymenttoExistingPaymentGroup(any(Payment.class),anyString())).thenReturn(paymentFeeLink);
        doNothing().when(feePayApportionService).processApportion(any(Payment.class));

        MvcResult result = restActions
            .post("/payment-groups/2021-1614709196068/bulk-scan-payments", bulkScanPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();
        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentGroupDto.class);
        assertEquals("2021-1614709196068",paymentGroupDto.getPaymentGroupReference());
    }

    @Test
    public void testRecordUnsolicitedBulkScanPayment() throws Exception {
        BulkScanPaymentRequest bulkScanPaymentRequest = getBulkScanPaymentRequest();
        when(referenceUtil.getNext(anyString())).thenReturn("RC-1614-7137-1374-2371");
        when(paymentGroupService.addNewBulkScanPayment(any(Payment.class), anyString())).thenReturn(getPaymentFeeLink(false));
        MvcResult result = restActions
            .post("/payment-groups/bulk-scan-payments", bulkScanPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();
        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertEquals("Success",paymentDto.getStatus());
    }


    private PaymentFeeLink getPaymentFeeLink(boolean isTelephony){
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
            .dateCreated(Date.valueOf("2020-02-01"))
            .externalReference("external-reference")
            .reference("RC-1614-7137-1374-2371")
            .build();
        if(isTelephony){
            payment.setPaymentChannel(PaymentChannel.paymentChannelWith().name("telephony").build());
            payment.setPaymentProvider(PaymentProvider.paymentProviderWith().name("pci pal").build());
        }
        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);
        return PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2021-1614709196068")
            .dateCreated(Date.valueOf("2020-01-20"))
            .dateUpdated(Date.valueOf("2020-01-21"))
            .fees(paymentFees)
            .payments(paymentList)
            .build();
    }

    private BulkScanPaymentRequest getBulkScanPaymentRequest(){
        return  BulkScanPaymentRequest.createBulkScanPaymentWith()
                .amount(BigDecimal.valueOf(100))
                .exceptionRecord("exception-rec")
                .ccdCaseNumber("ccd-case-number")
                .currency(CurrencyCode.GBP)
                .service(Service.CMC)
                .paymentMethod(PaymentMethodType.CASH)
                .paymentStatus(PaymentStatus.CREATED)
                .siteId("AA99")
                .giroSlipNo("giro-slip-no")
                .bankedDate("2020-01-02")
                .paymentChannel(PaymentChannel.paymentChannelWith().name("channel").build())
                .documentControlNumber("dcn-number")
                .payerName("payer-name")
                .build();


    }
}
