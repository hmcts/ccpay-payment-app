package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentReference;
import uk.gov.hmcts.payment.api.dto.PaymentStatusDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.dto.servicerequest.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeesService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.service.PaymentStatusUpdateService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
public class PaymentStatusControllerTest {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @InjectMocks
    PaymentStatusController paymentStatusController;


    MockMvc mvc;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;
    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;

    private RestActions restActions;
    @Autowired
    private ObjectMapper objectMapper;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @MockBean
    private PaymentFailureRepository paymentFailureRepository;

    @Mock
    private PaymentStatusDtoMapper paymentStatusDtoMapper;

    @Mock
    private PaymentStatusUpdateService paymentStatusUpdateService;

    @MockBean
    @Qualifier("restTemplateRefundCancel")
    private RestTemplate restTemplateRefundCancel;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private PaymentFailures paymentFailures;
    @MockBean
    private Payment2Repository paymentRepository;

   @MockBean
   private PaymentService<PaymentFeeLink, String> paymentService;

    @MockBean
    private DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @MockBean
    private PaymentFeeRepository paymentFeeRepository;
    @MockBean
    PaymentGroupDtoMapper paymentGroupDtoMapper;
    @Before
    public void setup() {
        mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

       restActions
            .withAuthorizedService("cmc")
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

    }

    @After
    public void tearDown() {
       this.restActions=null;
        mvc=null;
    }

    @Test
    public void returnsPaymentNotFoundExceptionWhenNoPaymentFoundForPaymentReference() throws Exception {

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentRepository.findByReference(any())).thenReturn(Optional.empty());
        MvcResult result = restActions
            .post("/payment-failures/bounced-cheque", paymentStatusBouncedChequeDto)
            .andExpect(status().isNotFound())
            .andReturn();

        assertEquals("No Payments available for the given Payment reference",result.getResolvedException().getMessage());


    }

    @Test
    public void returnsFailureReferenceNotFoundExceptionWhenFailureReferenceAlreadyAvailable() throws Exception {

        Payment payment = getPayment();
        PaymentFailures paymentFailures = getPaymentFailures();
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.of(paymentFailures));
        when(paymentStatusUpdateService.searchFailureReference(any())).thenReturn(Optional.of(paymentFailures));
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        MvcResult result = restActions
            .post("/payment-failures/bounced-cheque", paymentStatusBouncedChequeDto)
            .andExpect(status().isTooManyRequests())
            .andReturn();

        assertEquals("Request already received for this failure reference", result.getResolvedException().getMessage());

    }

    @Test
    public void returnSuccessWhenPaymentFailureIsSucessfullOpertion() throws Exception {

        Payment payment = getPayment();
        PaymentMethod paymentMethod = PaymentMethod.paymentMethodWith().name("online").build();
        Payment payment1 = Payment.paymentWith().internalReference("abc")
            .id(1)
            .reference("RC-1632-3254-9172-5888")
            .caseReference("123789")
            .paymentMethod(paymentMethod )
            .ccdCaseNumber("1234")
            .amount(new BigDecimal(300))
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .build();

        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment1);

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().ccdCaseNumber("1234")
            .enterpriseServiceName("divorce")
            .payments(paymentList)
            .paymentReference("123456")
            .build();

        uk.gov.hmcts.payment.api.dto.PaymentReference paymentReference = PaymentReference.paymentReference()
            .paymentAmount(new BigDecimal(300))
            .paymentReference("123")
            .paymentMethod("online")
            .caseReference("123")
            .accountNumber("123")
            .build();

        PaymentStatusDto paymentStatusDto = PaymentStatusDto.paymentStatusDto()
            .serviceRequestReference("123")
            .ccdCaseNumber("123456")
            .serviceRequestAmount(new BigDecimal(300))
            .serviceRequestStatus("Success")
            .payment(paymentReference)
            .build();

        PaymentFailures paymentFailures = getPaymentFailures();
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.empty());
        when(paymentStatusUpdateService.searchFailureReference(any())).thenReturn(Optional.empty());
        when(paymentFailureRepository.save(any())).thenReturn(paymentFailures);
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        when(paymentService.findSavedPayment(any())).thenReturn(payment1);
        when(paymentService.findByPaymentId(anyInt())).thenReturn(Arrays.asList(FeePayApportion.feePayApportionWith()
            .feeId(1)
            .build()));
        when(paymentFeeRepository.findById(anyInt())).thenReturn(Optional.of(PaymentFee.feeWith().paymentLink(paymentFeeLink).build()));
        when(delegatingPaymentService.retrieve(any(PaymentFeeLink.class) ,anyString())).thenReturn(paymentFeeLink);

        PaymentGroupDto paymentGroupDto = new PaymentGroupDto();
        paymentGroupDto.setServiceRequestStatus("Paid");
        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(paymentGroupDto);
        when(paymentStatusUpdateService.cancelFailurePaymentRefund(any())).thenReturn(true);
        when(authTokenGenerator.generate()).thenReturn("service auth token");
        when(this.restTemplateRefundCancel.exchange(anyString(),
            eq(HttpMethod.PATCH),
            any(HttpEntity.class),
            eq(String.class), any(Map.class)))
            .thenReturn(new ResponseEntity(HttpStatus.OK));
        MvcResult result1 = restActions
            .post("/payment-failures/bounced-cheque", paymentStatusBouncedChequeDto)
            .andExpect(status().isOk())
            .andReturn();

       assertEquals(200, result1.getResponse().getStatus());

    }

    @Test
    public void retrun500WhenRefundServerNotAvilable() throws Exception {

        Payment payment = getPayment();

        PaymentFailures paymentFailures = getPaymentFailures();
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.empty());
        when(paymentStatusUpdateService.searchFailureReference(any())).thenReturn(Optional.empty());
        when(paymentFailureRepository.save(any())).thenReturn(paymentFailures);
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        when(paymentStatusUpdateService.cancelFailurePaymentRefund(any())).thenReturn(false);
        when(authTokenGenerator.generate()).thenReturn("service auth token");
        when(this.restTemplateRefundCancel.exchange(anyString(),
            eq(HttpMethod.PATCH),
            any(HttpEntity.class),
            eq(String.class), any(Map.class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.NOT_FOUND));
        MvcResult result = restActions
            .post("/payment-failures/bounced-cheque", paymentStatusBouncedChequeDto)
            .andExpect(status().is5xxServerError())
            .andReturn();

        assertEquals(500, result.getResponse().getStatus());

    }

    private PaymentStatusBouncedChequeDto getPaymentStatusBouncedChequeDto() {

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto = PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(555))
            .failureReference("FR12345")
            .failure_event_date_time(Timestamp.valueOf("2021-10-10 10:10:10"))
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .build();

        return paymentStatusBouncedChequeDto;
    }

    private Payment getPayment() {

        Payment payment = Payment.paymentWith()
            .id(1)
            .amount(BigDecimal.valueOf(555))
            .caseReference("caseReference")
            .description("retrieve payment mock test")
            .serviceType("Civil Money Claims")
            .siteId("siteID")
            .currency("GBP")
            .organisationName("organisationName")
            .customerReference("customerReference")
            .pbaNumber("pbaNumer")
            .reference("RC-1520-2505-0381-8145")
            .ccdCaseNumber("1234123412341234")
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .build();

        return payment;
    }

    private PaymentFailures getPaymentFailures(){

        PaymentFailures paymentFailures = PaymentFailures.paymentFailuresWith()
            .id(1)
            .reason("RR001")
            .failureReference("Bounce Cheque")
            .paymentReference("RC12345")
            .ccdCaseNumber("123456")
            .amount(BigDecimal.valueOf(555))
            .build();
        return paymentFailures;

    }

}
