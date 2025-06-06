package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.dto.PaymentFailureReportResponse;
import uk.gov.hmcts.payment.api.dto.PaymentFailureResponseDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusChargebackDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusUpdateSecond;
import uk.gov.hmcts.payment.api.dto.RefundDto;
import uk.gov.hmcts.payment.api.dto.RefundPaymentFailureReportDtoResponse;
import uk.gov.hmcts.payment.api.dto.RepresentmentStatus;
import uk.gov.hmcts.payment.api.dto.SearchResponse;
import uk.gov.hmcts.payment.api.dto.TelephonyPaymentsReportDto;
import uk.gov.hmcts.payment.api.dto.TelephonyPaymentsReportResponse;
import uk.gov.hmcts.payment.api.dto.UnprocessedPayment;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentFailureReportMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFailureRepository;
import uk.gov.hmcts.payment.api.model.PaymentFailures;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.CustomTupleForTelephonyPaymentsReport;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.service.PaymentStatusUpdateService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import jakarta.persistence.Tuple;
import java.math.BigDecimal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    private static final String USER_ID = UserResolverBackdoor.CASEWORKER_ID;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

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

    @Mock
    private Logger logger;

    @MockBean
    @Qualifier("restTemplateRefundCancel")
    private RestTemplate restTemplateRefundCancel;

    @MockBean
    @Qualifier("restTemplatePaymentGroup")
    private RestTemplate restTemplatePaymentGroup;

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
    private LaunchDarklyFeatureToggler featureToggler;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("MM/dd/yyyy");

    @MockBean
    @Qualifier("restTemplateGetRefund")
    private RestTemplate restTemplateGetRefund;

    @Spy
    private PaymentFailureReportMapper paymentFailureReportMapper;

    @InjectMocks
    PaymentStatusController paymentStatusController;

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);

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
    public void returnsPaymentNotFoundExceptionWhenNoPaymentFoundForPaymentReferenceForBounceCheque() throws Exception {

        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any(), any())).thenReturn(paymentFailures);
        when(paymentRepository.findByReference(any())).thenReturn(Optional.empty());
        MvcResult result = restActions
            .post("/payment-failures/bounced-cheque", paymentStatusBouncedChequeDto)
            .andExpect(status().isNotFound())
            .andReturn();

        assertEquals("No Payments available for the given Payment reference",result.getResolvedException().getMessage());

    }

    @Test
    public void returnsFailureReferenceNotFoundExceptionWhenFailureReferenceAlreadyAvailableForBounceCheque() throws Exception {

        Payment payment = getPayment();
        PaymentFailures paymentFailures = getPaymentFailures();
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any(), any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.of(paymentFailures));
        when(paymentFailureRepository.save(any())).thenThrow(DataIntegrityViolationException.class);
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        MvcResult result = restActions
            .post("/payment-failures/bounced-cheque", paymentStatusBouncedChequeDto)
            .andExpect(status().isTooManyRequests())
            .andReturn();

        assertEquals("Request already received for this failure reference", result.getResolvedException().getMessage());

    }

    @Test
    public void returnSuccessWhenPaymentFailureIsSucessfullOpertionForBounceCheque() throws Exception {

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

        PaymentFailures paymentFailures = getPaymentFailures();
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(paymentStatusDtoMapper.bounceChequeRequestMapper(any(), any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.empty());
        when(paymentFailureRepository.save(any())).thenReturn(paymentFailures);
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        when(paymentService.findSavedPayment(any())).thenReturn(payment1);
        when(paymentService.findByPaymentId(anyInt())).thenReturn(Arrays.asList(FeePayApportion.feePayApportionWith()
            .feeId(1)
            .build()));
        when(paymentStatusUpdateService.cancelFailurePaymentRefund(any())).thenReturn(true);
        when(authTokenGenerator.generate()).thenReturn("service auth token");
        when(this.restTemplateRefundCancel.exchange(anyString(),
            eq(HttpMethod.PATCH),
            any(HttpEntity.class),
            eq(String.class), any(Map.class)))
            .thenReturn(ResponseEntity.ok().build());
        MvcResult result1 = restActions
            .post("/payment-failures/bounced-cheque", paymentStatusBouncedChequeDto)
            .andExpect(status().isOk())
            .andReturn();

       assertEquals(200, result1.getResponse().getStatus());

    }

    @Test
    public void returnsPaymentNotFoundExceptionWhenNoPaymentFoundForPaymentReferenceForChargeback() throws Exception {

        PaymentStatusChargebackDto paymentStatusChargebackDto =getPaymentStatusChargebackDto();
        when(paymentStatusDtoMapper.ChargebackRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentRepository.findByReference(any())).thenReturn(Optional.empty());
        MvcResult result = restActions
            .post("/payment-failures/chargeback", paymentStatusChargebackDto)
            .andExpect(status().isNotFound())
            .andReturn();

        assertEquals("No Payments available for the given Payment reference",result.getResolvedException().getMessage());

    }

    @Test
    public void returnsFailureReferenceNotFoundExceptionWhenFailureReferenceAlreadyAvailableForChargeback() throws Exception {

        Payment payment = getPayment();
        PaymentFailures paymentFailures = getPaymentFailures();
        PaymentStatusChargebackDto paymentStatusChargebackDto =getPaymentStatusChargebackDto();
        when(paymentStatusDtoMapper.ChargebackRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.of(paymentFailures));
        when(paymentFailureRepository.save(any())).thenThrow(DataIntegrityViolationException.class);
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        MvcResult result = restActions
            .post("/payment-failures/chargeback", paymentStatusChargebackDto)
            .andExpect(status().isTooManyRequests())
            .andReturn();

        assertEquals("Request already received for this failure reference", result.getResolvedException().getMessage());

    }

    @Test
    public void returnSuccessWhenPaymentFailureIsSucessfullOpertionForChargeback() throws Exception {

        Payment payment = getPayment();
      PaymentMethod paymentMethod = PaymentMethod.paymentMethodWith().name("online").build();
        PaymentFee fee = PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build();
        Payment payment2 = Payment.paymentWith().internalReference("abc")
            .id(1)
            .reference("RC-1632-3254-9172-5888")
            .caseReference("123789")
            .paymentMethod(paymentMethod )
            .ccdCaseNumber("1234")
            .amount(new BigDecimal(300))
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .build();
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .paymentReference("2018-15202505035")
            .fees(Arrays.asList(fee))
            .payments(Arrays.asList(payment2))
            .callBackUrl("http//:test")
            .build();

        PaymentFailures paymentFailures = getPaymentFailures();
        PaymentStatusChargebackDto paymentStatusChargebackDto =getPaymentStatusChargebackDto();
        when(paymentStatusDtoMapper.ChargebackRequestMapper(any())).thenReturn(paymentFailures);
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.empty());
        when(paymentFailureRepository.save(any())).thenReturn(paymentFailures);
        when(paymentRepository.findByReference(any())).thenReturn(Optional.of(payment));
        when(delegatingPaymentService.retrieve(any(PaymentFeeLink.class) ,anyString())).thenReturn(paymentFeeLink);
        when(paymentStatusUpdateService.cancelFailurePaymentRefund(any())).thenReturn(true);
        when(authTokenGenerator.generate()).thenReturn("service auth token");
        when(this.restTemplateRefundCancel.exchange(anyString(),
            eq(HttpMethod.PATCH),
            any(HttpEntity.class),
            eq(String.class), any(Map.class)))
            .thenReturn(ResponseEntity.ok().build());
        MvcResult result = restActions
            .post("/payment-failures/chargeback", paymentStatusChargebackDto)
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(200, result.getResponse().getStatus());

    }

    @Test
    public void retrievePaymentFailureByPaymentReference() throws Exception {

        when(paymentFailureRepository.findByPaymentReferenceOrderByFailureEventDateTimeDesc(any())).thenReturn(Optional.of(getPaymentFailuresList()));
        MvcResult result = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/payment-failures/RC-1637-5072-9888-4233")
            .andExpect(status().isOk())
            .andReturn();

        PaymentFailureResponseDto response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentFailureResponseDto.class);
        assertNotNull(response);
    }

    @Test
    public void return204WhenPaymentFailureNotFoundByPaymentReference() throws Exception {

        when(paymentFailureRepository.findByPaymentReferenceOrderByFailureEventDateTimeDesc(any())).thenReturn(Optional.empty());
        MvcResult result = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/payment-failures/RC-1637-5072-9888-4233")
            .andExpect(status().isNoContent())
            .andReturn();
        assertEquals(204,result.getResponse().getStatus());
    }

    @Test
    public void testDeletePaymentByReference() {
        String failureReference = "FR12345";
        doNothing().when(paymentStatusUpdateService).deleteByFailureReference(failureReference);
        paymentStatusController.deleteByFailureReference(failureReference);
        verify(paymentStatusUpdateService).deleteByFailureReference(failureReference);
    }

    @Test
    public void testDeletePaymentReferenceNotFound() throws Exception {
        restActions.delete("/payment-status-delete/test")
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void lockedChargeBackShouldThrowServiceUnavailable() throws Exception {
        PaymentStatusChargebackDto paymentStatusChargebackDto =getPaymentStatusChargebackDto();
        when(featureToggler.getBooleanValue(eq("payment-status-update-flag"),anyBoolean())).thenReturn(true);
        MvcResult result = restActions
            .post("/payment-failures/chargeback", paymentStatusChargebackDto)
            .andExpect(status().isServiceUnavailable())
            .andReturn();
    }

    @Test
    public void lockedBounceChequeShouldThrowServiceUnavailable() throws Exception {
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto =getPaymentStatusBouncedChequeDto();
        when(featureToggler.getBooleanValue(eq("payment-status-update-flag"),anyBoolean())).thenReturn(true);
        MvcResult result = restActions
            .post("/payment-failures/bounced-cheque", paymentStatusBouncedChequeDto)
            .andExpect(status().isServiceUnavailable())
            .andReturn();
    }

    @Test
    public void lockedGetFailuresShouldThrowServiceUnavailable() throws Exception {

        when(featureToggler.getBooleanValue(eq("payment-status-update-flag"),anyBoolean())).thenReturn(true);
        MvcResult result = restActions
                .withAuthorizedUser(USER_ID)
                .withUserId(USER_ID)
                .get("/payment-failures/RC-1637-5072-9888-4233")
                .andExpect(status().isServiceUnavailable())
                .andReturn();
        assertEquals(503,result.getResponse().getStatus());

    }

    @Test
    public void lockedPaymentStatusSecondShouldThrowServiceUnavailable() throws Exception {
        when(featureToggler.getBooleanValue(eq("payment-status-update-flag"), anyBoolean())).thenReturn(true);
        restActions
                .patch("/payment-failures/failureReference", PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                        .representmentStatus(RepresentmentStatus.No)
                        .representmentDate("2022-10-10T10:10:10")
                        .build())
                .andExpect(status().isServiceUnavailable())
                .andReturn();
    }

    @Test
    public void lockedUnprocessedPaymentShouldThrowServiceUnavailable() throws Exception {
        when(featureToggler.getBooleanValue(eq("payment-status-update-flag"), anyBoolean())).thenReturn(true);
        restActions
                .post("/payment-failures/unprocessed-payment", UnprocessedPayment.unprocessedPayment()
                        .amount(BigDecimal.valueOf(999))
                        .failureReference("FR9999")
                        .eventDateTime("2022-10-10T10:10:10")
                        .reason("RR001")
                        .dcn("9999")
                        .build())
                .andExpect(status().isServiceUnavailable())
                .andReturn();
    }

    @Test
    public void givenNoRepresentmentStatusWhenPaymentStatusSecondThenBadRequestException() throws Exception {
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentDate("2022-10-10T10:10:10")
                .build();

        MvcResult result = restActions
                .patch("/payment-failures/failureReference", paymentStatusUpdateSecond)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals("Bad request", result.getResolvedException().getMessage());
    }

    @Test
    public void givenEmptyRequestWhenPaymentStatusSecondThenBadRequestException() throws Exception {
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .build();

        MvcResult result = restActions
                .patch("/payment-failures/failureReference", paymentStatusUpdateSecond)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals("Bad request", result.getResolvedException().getMessage());
    }

    @Test
    public void givenNoPaymentFailureWhenPaymentStatusSecondThenPaymentNotFoundException() throws Exception {
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.Yes)
                .representmentDate("2022-10-10T10:10:10")
                .build();
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.empty());

        MvcResult result = restActions
                .patch("/payment-failures/failureReference", paymentStatusUpdateSecond)
                .andExpect(status().isNotFound())
                .andReturn();

        assertEquals("No Payment Failure available for the given Failure reference",
                result.getResolvedException().getMessage());
    }

    @Test
    public void givenPaymentFailureWhenPaymentStatusSecondThenSuccess() throws Exception {
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.Yes)
                .representmentDate("2022-10-10T10:10:10")
                .build();
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.of(getPaymentFailures()));
        MvcResult result = restActions
                .patch("/payment-failures/failureReference", paymentStatusUpdateSecond)
                .andExpect(status().isOk())
                .andReturn();

        String message = result.getResponse().getContentAsString();
        assertEquals("successful operation", message);
    }

    @Test
    public void givenPaymentFailureWhenUnprocessedPaymentThenSuccess() throws Exception {
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(888))
                .failureReference("FR8888")
                .eventDateTime("2022-10-10T10:10:10")
                .reason("RR001")
                .dcn("88")
                .poBoxNumber("8")
                .build();
        ResponseEntity responseEntity = ResponseEntity.ok().build();
        when(this.restTemplatePaymentGroup.exchange(any(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(SearchResponse.class), any(HashMap.class)))
                .thenReturn(responseEntity);
        MvcResult result = restActions
                .post("/payment-failures/unprocessed-payment", unprocessedPayment)
                .andExpect(status().isOk())
                .andReturn();

        String message = result.getResponse().getContentAsString();
        assertEquals("successful operation", message);
    }

    @Test
    public void givenNoPaymentWhenUnprocessedPaymentThen404() throws Exception {
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(888))
                .failureReference("FR8888")
                .eventDateTime("2022-10-10T10:10:10")
                .reason("RR001")
                .dcn("88")
                .poBoxNumber("8")
                .build();
        when(this.restTemplatePaymentGroup.exchange(any(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(SearchResponse.class), any(HashMap.class)))
                .thenReturn(ResponseEntity.notFound().build());
        MvcResult result = restActions
                .post("/payment-failures/unprocessed-payment", unprocessedPayment)
                .andExpect(status().isNotFound())
                .andReturn();

        String message = result.getResponse().getContentAsString();
        assertEquals("No Payments available for the given document reference number", message);
    }

    @Test
    public void givenDuplicateRequestWhenUnprocessedPaymentThen429() throws Exception {
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(888))
                .failureReference("FR8888")
                .eventDateTime("2022-10-10T10:10:10")
                .reason("RR001")
                .dcn("88")
                .poBoxNumber("8")
                .build();
        when(this.restTemplatePaymentGroup.exchange(any(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(SearchResponse.class), any(HashMap.class)))
                .thenReturn(ResponseEntity.ok().build());
        when(paymentFailureRepository.save(any())).thenThrow(DataIntegrityViolationException.class);
        MvcResult result = restActions
                .post("/payment-failures/unprocessed-payment", unprocessedPayment)
                .andExpect(status().isTooManyRequests())
                .andReturn();

        assertEquals("Request already received for this failure reference", result.getResolvedException().getMessage());
    }

    @Test
    public void testSuccessFullyUnprocessedPaymentUpdate() throws Exception{
        when(featureToggler.getBooleanValue(eq("payment-status-update-flag"),anyBoolean())).thenReturn(false);
        when(paymentFailureRepository.findDcn()).thenReturn(getPaymentFailuresDcnList());
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.of(getPaymentFailures()));
        when(paymentRepository.findByDocumentControlNumberInAndPaymentMethod(any(),any())).thenReturn(Arrays.asList(getPayment()));

        MvcResult result = restActions
            .patch("/jobs/unprocessed-payment-update")
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void testFullyUnprocessedPaymentUpdate() throws Exception{
        when(featureToggler.getBooleanValue(eq("payment-status-update-flag"),anyBoolean())).thenReturn(false);
        when(paymentFailureRepository.findDcn()).thenReturn(getPaymentFailuresDcnList());
        when(paymentFailureRepository.findByFailureReference(any())).thenReturn(Optional.of(getPaymentFailures()));
        when(paymentRepository.findByDocumentControlNumberInAndPaymentMethod(any(),any())).thenReturn(Arrays.asList(getPayment()));

        MvcResult result = restActions
            .patch("/jobs/unprocessed-payment-update")
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        verify(paymentFailureRepository, atLeastOnce()).findByFailureReference(any());
        verify(paymentRepository, atLeastOnce()).findByDocumentControlNumberInAndPaymentMethod(any(),any());
        verify(paymentFailureRepository, atLeastOnce()).findDcn();
    }

    @Test
    public void testFullyUnprocessedPaymentUpdateWhenFeatureFlagIsEnabled() throws Exception{
        when(featureToggler.getBooleanValue(eq("payment-status-update-flag"),anyBoolean())).thenReturn(true);

        MvcResult result = restActions
            .patch("/jobs/unprocessed-payment-update")
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        verify(paymentFailureRepository, never()).findByFailureReference(any());
        verify(paymentRepository, never()).findByDocumentControlNumberInAndPaymentMethod(any(),any());
        verify(paymentFailureRepository, never()).findDcn();
    }

    @Test
    public void returnSuccessPaymentFailureReport() throws Exception{

        when(paymentFailureRepository.findByDatesBetween(any(),any())).thenReturn(getPaymentFailuresList());
        when(paymentRepository.findByReferenceIn(any())).thenReturn(getPaymentList());
        ResponseEntity<RefundPaymentFailureReportDtoResponse> responseEntity = new ResponseEntity<>(getFailureRefund(), HttpStatus.OK);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        headers.add("Content-Type", "application/json");
        when(authTokenGenerator.generate()).thenReturn("test-token");

        when(this.restTemplateGetRefund.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<RefundPaymentFailureReportDtoResponse>() {
            }))).thenReturn(responseEntity);
        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/payment-failures/failure-report?date_from=" + startDate + "&date_to=" + endDate)
            .andExpect(status().isOk())
            .andReturn();
        PaymentFailureReportResponse paymentFailureReportResponse = objectMapper.readValue(result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                });

        assertThat(paymentFailureReportResponse.getPaymentFailureReportList().size()).isEqualTo(1);
        assertThat(paymentFailureReportResponse.getPaymentFailureReportList().get(0).getPaymentReference()).isEqualTo("RC-1520-2505-0381-8145");

    }

    @Test
    public void returnSuccessTelephonyPaymentsReport() throws Exception{

        when(paymentStatusUpdateService.telephonyPaymentsReport(any(),any(),any())).thenReturn(getTelephonyPaymentsDtoList());
        when(paymentRepository.findAllByDateCreatedBetweenAndPaymentChannel(any(),any(),any())).thenReturn(getTelephonyPaymentsObjectList());
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        headers.add("Content-Type", "application/json");
        when(authTokenGenerator.generate()).thenReturn("test-token");

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/telephony-payments/telephony-payments-report?date_from=" + startDate + "&date_to=" + endDate)
            .andExpect(status().isOk())
            .andReturn();
        TelephonyPaymentsReportResponse telephonyPaymentsReportResponse = objectMapper.readValue(result.getResponse().getContentAsByteArray(),
            new TypeReference<>() {
            });

        assertThat(telephonyPaymentsReportResponse.getTelephonyPaymentsReportList().size()).isEqualTo(2);
        assertThat(telephonyPaymentsReportResponse.getTelephonyPaymentsReportList().get(0).getPaymentReference()).isEqualTo("PAY123");

    }

    @Test
    public void lockedReportShouldThrowServiceUnavailable() throws Exception {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "auth");
        headers.add("ServiceAuthorization", "service-auth");
        headers.add("Content-Type", "application/json");
        when(authTokenGenerator.generate()).thenReturn("test-token");
        when(paymentFailureRepository.findByPaymentReferenceOrderByFailureEventDateTimeDesc(any())).thenReturn(Optional.empty());
        when(featureToggler.getBooleanValue(eq("payment-status-update-flag"),anyBoolean())).thenReturn(true);

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/payment-failures/failure-report?date_from=" + startDate + "&date_to=" + endDate)
            .andExpect(status().isServiceUnavailable())
            .andReturn();
        assertEquals(503,result.getResponse().getStatus());

    }
    private PaymentStatusBouncedChequeDto getPaymentStatusBouncedChequeDto() {

        return PaymentStatusBouncedChequeDto.paymentStatusBouncedChequeRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(555))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .build();
    }

    private Payment getPayment() throws ParseException {

        String dateInString = "2021-10-09T10:10:10";
        Date date = formatter.parse(dateInString);
        return Payment.paymentWith()
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
            .documentControlNumber("12345")
            .dateUpdated(date)
            .bankedDate(date)
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("cheque").build())
            .paymentLink(PaymentFeeLink.paymentFeeLinkWith()
                .id(1)
                .paymentReference("2018-15202505035")
                .fees(Arrays.asList(PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build()))
                .payments(Arrays.asList(Payment.paymentWith().internalReference("abc")
                    .id(1)
                    .reference("RC-1632-3254-9172-5888")
                    .caseReference("123789")
                    .ccdCaseNumber("1234")
                    .amount(new BigDecimal(300))
                    .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
                    .build()))
                .callBackUrl("http//:test")
                .build())
            .build();
    }

    private PaymentFailures getPaymentFailures() throws ParseException {

        String dateInString = "2021-10-09T10:10:10";
        Date date = formatter.parse(dateInString);
        return PaymentFailures.paymentFailuresWith()
            .id(1)
            .reason("RR001")
            .failureReference("Bounce Cheque")
            .paymentReference("RC12345")
            .ccdCaseNumber("123456")
            .amount(BigDecimal.valueOf(555))
            .failureEventDateTime(date)
            .dcn("12345")
            .failureType("Bounced Cheque")
            .build();

    }

    private PaymentStatusChargebackDto getPaymentStatusChargebackDto() {

        return PaymentStatusChargebackDto.paymentStatusChargebackRequestWith()
            .additionalReference("AR1234")
            .amount(BigDecimal.valueOf(555))
            .failureReference("FR12345")
            .eventDateTime("2021-10-10T10:10:10")
            .ccdCaseNumber("123456")
            .reason("RR001")
            .paymentReference("RC1234")
            .hasAmountDebited("yes")
            .build();
    }

    private List<PaymentFailures> getPaymentFailuresList(){

        List<PaymentFailures> paymentFailuresList = new ArrayList<>();
        PaymentFailures paymentFailures = PaymentFailures.paymentFailuresWith()
            .id(1)
            .reason("test")
            .failureReference("Bounce Cheque")
            .paymentReference("RC-1520-2505-0381-8145")
            .ccdCaseNumber("123456")
            .amount(BigDecimal.valueOf(555))
            .representmentSuccess("yes")
            .failureType("Chargeback")
            .additionalReference("AR12345")
            .build();

        paymentFailuresList.add(paymentFailures);
        return paymentFailuresList;

    }

    private List<PaymentFailures> getPaymentFailuresDcnList(){

        List<PaymentFailures> paymentFailuresList = new ArrayList<>();
        PaymentFailures paymentFailures = PaymentFailures.paymentFailuresWith()
            .id(1)
            .reason("test")
            .failureReference("FR12345")
            .amount(BigDecimal.valueOf(555))
            .representmentSuccess("yes")
            .poBoxNumber("12345")
            .dcn("12345")
            .build();

        paymentFailuresList.add(paymentFailures);
        return paymentFailuresList;

    }

    private RefundDto getRefund(){
        DateTime currentDateTime = new DateTime();
        return RefundDto.buildRefundListDtoWith()
            .refundReference("RF-123=345=897")
            .amount(BigDecimal.valueOf(5))
            .paymentReference("RC-1520-2505-0381-8145")
            .refundDate(currentDateTime.toDate().toString())
            .build();
    }

    private List<Payment> getPaymentList() {

        List<Payment> paymentList =new ArrayList<>();
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
            .paymentLink(PaymentFeeLink.paymentFeeLinkWith()
                .id(1)
                .paymentReference("2018-15202505035")
                .fees(Arrays.asList(PaymentFee.feeWith().id(1).calculatedAmount(new BigDecimal("11.99")).code("X0001").version("1").build()))
                .payments(Arrays.asList(Payment.paymentWith().internalReference("abc")
                    .id(1)
                    .reference("RC-1632-3254-9172-5888")
                    .caseReference("123789")
                    .ccdCaseNumber("1234")
                    .amount(new BigDecimal(300))
                    .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
                    .build()))
                .callBackUrl("http//:test")
                .build())
            .build();

        paymentList.add(payment);

        return paymentList;
    }


    private RefundPaymentFailureReportDtoResponse getFailureRefund(){
        return   RefundPaymentFailureReportDtoResponse.buildPaymentFailureListWith().paymentFailureDto(Arrays.asList(getRefund())).build();

    }


    private List<Tuple> getTelephonyPaymentsObjectList(){
        List<Tuple> telephonyPaymentsList = Arrays.asList(
            new CustomTupleForTelephonyPaymentsReport("Service1", "CCD123", "PAY123", "FEE001", new Date(), new BigDecimal("100.00"), "Success"),
            new CustomTupleForTelephonyPaymentsReport("Service2", "CCD456", "PAY456", "FEE002", new Date(), new BigDecimal("200.00"), "Failed")
        );

       return telephonyPaymentsList;
    }

    private List<TelephonyPaymentsReportDto> getTelephonyPaymentsDtoList(){
        List<TelephonyPaymentsReportDto> mockReportList = Arrays.asList(
            TelephonyPaymentsReportDto.telephonyPaymentsReportDtoWith()
                .serviceName("Service1")
                .ccdReference("CCD123")
                .paymentReference("PAY123")
                .feeCode("FEE001")
                .paymentDate(new Date())
                .amount(new BigDecimal("100.00"))
                .paymentStatus("Success")
                .build(),
            TelephonyPaymentsReportDto.telephonyPaymentsReportDtoWith()
                .serviceName("Service2")
                .ccdReference("CCD456")
                .paymentReference("PAY456")
                .feeCode("FEE002")
                .paymentDate(new Date())
                .amount(new BigDecimal("200.00"))
                .paymentStatus("Failed")
                .build()
        );
        return mockReportList;
    }
}
