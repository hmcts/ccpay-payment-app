package uk.gov.hmcts.payment.api.controllers;

import org.ff4j.FF4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.CardDetailsService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.math.BigDecimal;
import java.util.Arrays;


import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class CardPaymentControllerNewTest extends PaymentsDataUtil {

    @Mock
    DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentServiceMock;

    @Mock
    FeePayApportionService feePayApportionService;

    @Mock
    PaymentDtoMapper paymentDtoMapper;

    @Mock
    LaunchDarklyFeatureToggler featureToggler;

    @Mock
    CardDetailsService<CardDetails, String> cardDetailsService;

    @Mock
    DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @Mock
    FF4j ff4j;

    @InjectMocks
    CardPaymentController cardPaymentController;

    private static final String USER_ID = UserResolverBackdoor.CITIZEN_ID;

    @Autowired
    private ObjectMapper objectMapper;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @Test
    public void testCreateCardPaymentWithValidRequestShouldReturnValidResponse() throws Exception {
        CardPaymentRequest cardPaymentRequest = getValidCardPaymentRequest();
        String returnURL = "http://localhost";
        String serviceCallbackUrl = "http://payments.com";
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();
        PaymentFeeLink paymentLink = paymentFeeLinkWith().paymentReference("2018-15186162001").payments(Arrays.asList(getPaymentWithInitiatedStatus(cardPaymentRequest))).fees(Arrays.asList(fee)).build();
        when(delegatingPaymentServiceMock.create(any(PaymentServiceRequest.class))).thenReturn(paymentLink);
        when(paymentDtoMapper.toFee(any(FeeDto.class))).thenCallRealMethod();
        when(paymentDtoMapper.toCardPaymentDto(any(PaymentFeeLink.class))).thenCallRealMethod();
        when(featureToggler.getBooleanValue(eq("apportion-feature"),any(Boolean.class))).thenReturn(false);
        ResponseEntity<PaymentDto> responseEntity = cardPaymentController.createCardPayment(returnURL,serviceCallbackUrl,cardPaymentRequest);
        assertEquals(201, responseEntity.getStatusCode().value());
    }

//    @Test
//    public void testRetrievePaymentWithReference(){
//        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();
//        PaymentFeeLink paymentLink = paymentFeeLinkWith().paymentReference("2018-15186162001").payments(Arrays.asList(getPayment())).fees(Arrays.asList(fee)).build();
//        when(delegatingPaymentServiceMock.retrieve(any(String.class))).thenReturn(paymentLink);
//        when(paymentDtoMapper.toRetrieveCardPaymentResponseDto(any(PaymentFeeLink.class))).thenCallRealMethod();
//        PaymentDto payment = cardPaymentController.retrieve("RC-1519-9028-1909-3475");
//        assertEquals("Success",payment.getStatus());
//    }

    @Test
    public void testRetrieveWithCardDetails(){
        CardDetails cardDetails = CardDetails.cardDetailsWith()
                                    .cardBrand("card-brand")
                                    .cardholderName("card-holder-name")
                                    .email("email")
                                    .lastDigitsCardNumber("1234")
                                    .build();
        when(cardDetailsService.retrieve(any(String.class))).thenReturn(cardDetails);
        CardDetails result = cardPaymentController.retrieveWithCardDetails("reference");
        assertEquals("1234",result.getLastDigitsCardNumber());
    }

//    @Test
//    public void testRetrievePaymentStatus(){
//        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();
//        PaymentFeeLink paymentLink = paymentFeeLinkWith().paymentReference("2018-15186162001").payments(Arrays.asList(getPayment())).fees(Arrays.asList(fee)).build();
//        when(delegatingPaymentService.retrieve(any(String.class))).thenReturn(paymentLink);
//        Payment payment = getPayment();
//        when(paymentDtoMapper.toPaymentStatusesDto(payment)).thenReturn(getPaymentDto());
//        PaymentDto paymentDto = cardPaymentController.retrievePaymentStatus("RC-1519-9028-1909-3475");
//        assertEquals("Initiated",paymentDto.getStatus());
//    }

    @Test(expected = PaymentException.class)
    public void testCancelPaymentThrowsPaymentException(){
        when(ff4j.check(any(String.class))).thenReturn(false);
        cardPaymentController.cancelPayment("payment-reference");
    }

    @Test
    public void testCancelPayment(){
        when(ff4j.check(any(String.class))).thenReturn(true);
        ResponseEntity responseEntity = cardPaymentController.cancelPayment("payment-reference");
        assertEquals(204,responseEntity.getStatusCode().value());
    }


    private CardPaymentRequest getValidCardPaymentRequest() throws Exception {
        return objectMapper.readValue(requestJson().getBytes(), CardPaymentRequest.class);
    }

    private PaymentDto getPaymentDto(){
        return PaymentDto.payment2DtoWith()
            .reference("reference")
            .amount(new BigDecimal("100.00"))
            .paymentGroupReference("group-reference")
            .status(PayStatusToPayHubStatus.valueOf("created").getMappedStatus())
            .build();
    }

    private Payment getPayment(){
        return Payment.paymentWith()
            .amount(new BigDecimal("200.00"))
            .caseReference("case-reference")
            .ccdCaseNumber("ccd-number")
            .description("description")
            .serviceType("service-type")
            .currency("GBP")
            .status("created")
            .siteId("site-id")
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .externalReference("ia2mv22nl5o880rct0vqfa7k76")
            .reference("RC-1519-9028-1909-3475")
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .status("Initiated")
                .externalStatus("created")
                .build()))
            .build();
    }

    private Payment getPaymentWithInitiatedStatus(CardPaymentRequest cardPaymentRequest){
        return Payment.paymentWith()
            .amount(cardPaymentRequest.getAmount())
            .caseReference(cardPaymentRequest.getCaseReference())
            .ccdCaseNumber(cardPaymentRequest.getCcdCaseNumber())
            .description(cardPaymentRequest.getDescription())
            .serviceType(cardPaymentRequest.getService().getName())
            .currency(cardPaymentRequest.getCurrency().getCode())
            .status("created")
            .siteId(cardPaymentRequest.getSiteId())
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name(cardPaymentRequest.getChannel()).build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("ia2mv22nl5o880rct0vqfa7k76")
            .reference("RC-1519-9028-1909-3475")
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .status("Initiated")
                .externalStatus("created")
                .build()))
            .build();
    }

    private String cardPaymentInvalidRequestJson() {
        return "{\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"invalid\": \"CCD101\",\n" +
            "  \"invalid_reference\": \"12345\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"return_url\": \"https://www.moneyclaims.service.gov.uk\",\n" +
            "  \"site_id\": \"AA101\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

}
