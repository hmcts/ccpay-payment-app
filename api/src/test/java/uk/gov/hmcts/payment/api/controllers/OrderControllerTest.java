package uk.gov.hmcts.payment.api.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.PaymentDbBackdoor;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.domain.model.OrderPaymentBo;
import uk.gov.hmcts.payment.api.domain.service.OrderDomainService;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.order.OrderDto;
import uk.gov.hmcts.payment.api.dto.order.OrderFeeDto;
import uk.gov.hmcts.payment.api.dto.order.OrderPaymentDto;
import uk.gov.hmcts.payment.api.model.CaseDetails;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.OrderException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.OrderExceptionForNoAmountDue;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class OrderControllerTest {

    private static final String USER_ID = UserResolverBackdoor.CITIZEN_ID;
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Mock
    private ReferenceDataService referenceDataService;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private OrderDomainService orderDomainService;

    @Autowired
    private AccountService<AccountDto, String> accountService;

    @Autowired
    PaymentDbBackdoor paymentDbBackdoor;

    private RestActions restActions;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    @Transactional
    public void setup() {

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

    }



    @Test
    public void createPBAPaymentWithOrderSuccessTest() throws Exception {

        //Creation of Order-reference
        OrderDto orderDto = OrderDto.orderDtoWith()
            .caseReference("123245677")
            .caseType("MoneyClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(getMultipleFees())
            .build();

        MvcResult result = restActions
            .post("/order", orderDto)
            .andExpect(status().isCreated())
            .andReturn();

        String orderReferenceResult = result.getResponse().getContentAsString();

        //Order Payment DTO
        OrderPaymentDto orderPaymentDto = OrderPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(300))
            .currency(CurrencyCode.valueOf("GBP"))
            .customerReference("testCustReference").
                build();


        AccountDto liberataAccountResponse = AccountDto.accountDtoWith()
            .accountNumber("PBAFUNC12345")
            .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
            .creditLimit(BigDecimal.valueOf(28879))
            .availableBalance(BigDecimal.valueOf(30000))
            .status(AccountStatus.ACTIVE)
            .build();

        when(accountService.retrieve("PBAFUNC12345")).thenReturn(liberataAccountResponse);

        //Payment amount should be matching with fees
        String idempotencyKey = UUID.randomUUID().toString();


        MvcResult successOrderPaymentResult = restActions
            .withHeader("idempotency_key", idempotencyKey)
            .post("/order/" + orderReferenceResult + "/credit-account-payment", orderPaymentDto)
            .andExpect(status().isCreated())
            .andReturn();

        OrderPaymentBo orderPaymentBo = objectMapper.readValue(successOrderPaymentResult.getResponse().getContentAsByteArray(), OrderPaymentBo.class);

        // 1. PBA Payment was successful
        assertTrue(orderPaymentBo.getPaymentReference().contains("RC-"));
        assertEquals("success", orderPaymentBo.getStatus());
        assertNotNull(orderPaymentBo.getDateCreated());
        assertNull(orderPaymentBo.getErrorCode());
        assertNull(orderPaymentBo.getErrorMessage());


        // 2.Duplicate request with same Idempotency key, same request content, same order reference
        MvcResult duplicatePBAPaymentResult = restActions
            .withHeaderIfpresent("idempotency_key", idempotencyKey)
            .post("/order/" + orderReferenceResult + "/credit-account-payment", orderPaymentDto)
            .andExpect(status().isCreated())
            .andReturn();

        OrderPaymentBo oldOrderPaymentBA = objectMapper.readValue(duplicatePBAPaymentResult.getResponse().getContentAsByteArray(), OrderPaymentBo.class);

        //Get old payment details from idempotency table
        assertEquals(orderPaymentBo.getPaymentReference(), oldOrderPaymentBA.getPaymentReference());
        assertEquals(orderPaymentBo.getStatus(), oldOrderPaymentBA.getStatus());
        assertEquals(orderPaymentBo.getDateCreated(), oldOrderPaymentBA.getDateCreated());


        //3.Duplicate request with same Idempotency key with different Payment details
        orderPaymentDto.setAmount(BigDecimal.valueOf(111)); //changed the amount
        orderPaymentDto.setCustomerReference("cust-reference-change"); //changed the customer reference

        MvcResult conflictPBAPaymentResult = restActions
            .withHeaderIfpresent("idempotency_key", idempotencyKey)
            .post("/order/" + orderReferenceResult + "/credit-account-payment", orderPaymentDto)
            .andExpect(status().isConflict())
            .andReturn();

        assertTrue(conflictPBAPaymentResult.getResponse().
            getContentAsString().
            contains("Payment already present for idempotency key with different payment details"));

        //4. Different idempotency key, same order reference, same payment details no amount due
        orderPaymentDto.setAmount(BigDecimal.valueOf(300)); //changed the amount
        orderPaymentDto.setCustomerReference("testCustReference"); //changed the customer reference

        restActions
            .withHeaderIfpresent("idempotency_key", UUID.randomUUID().toString())
            .post("/order/" + orderReferenceResult + "/credit-account-payment", orderPaymentDto)
            .andExpect(status().isPreconditionFailed())
            .andExpect(orderException -> assertTrue(orderException.getResolvedException() instanceof OrderExceptionForNoAmountDue))
            .andExpect(orderException -> assertEquals("No fee amount due for payment for this order", orderException.getResolvedException().getMessage()))
            .andReturn();
    }

    @Test
    public void createPBAPaymentWithOrderTimeOutTest() throws Exception {
        when(accountService.retrieve("PBA12346")).thenThrow(
            new HystrixRuntimeException(HystrixRuntimeException.FailureType.TIMEOUT, HystrixCommand.class, "Unable to retrieve account information", null, null));

        //Order Payment DTO
        OrderPaymentDto orderPaymentDto = OrderPaymentDto
            .paymentDtoWith().accountNumber("PBA12346")
            .amount(BigDecimal.valueOf(100))
            .currency(CurrencyCode.valueOf("GBP"))
            .customerReference("testCustReference").
                build();

        //Order
        String idempotencyKey = UUID.randomUUID().toString();

        MvcResult result = restActions
            .withHeader("idempotency_key", idempotencyKey)
            .post("/order/" + "2020-1619524583861" + "/credit-account-payment", orderPaymentDto)
            .andExpect(status().isGatewayTimeout())
            .andReturn();

        assertTrue(result.getResponse().
            getContentAsString().
            contains("Unable to retrieve account information due to timeout"));

        //Duplicate request for timeout
        MvcResult result1 = restActions
            .withHeader("idempotency_key", idempotencyKey)
            .post("/order/" + "2020-1619524583861" + "/credit-account-payment", orderPaymentDto)
            .andExpect(status().isGatewayTimeout())
            .andReturn();

        assertTrue(result1.getResponse().
            getContentAsString().
            contains("Unable to retrieve account information due to timeout"));
    }


    @Test
    public void createPBAPaymentNonMatchAmountTest() throws Exception {

        //Order Payment DTO
        OrderPaymentDto orderPaymentDto = OrderPaymentDto
            .paymentDtoWith().accountNumber("PBA12345")
            .amount(BigDecimal.valueOf(100))
            .currency(CurrencyCode.valueOf("GBP"))
            .customerReference("testCustReference").
                build();

        //Order
        PaymentFee fee1 = PaymentFee.feeWith().code("X101")
            .version("1")
            .calculatedAmount(BigDecimal.valueOf(100))
            .volume(1).build();

        PaymentFee fee2 = PaymentFee.feeWith().code("X102")
            .version("1")
            .calculatedAmount(BigDecimal.valueOf(200))
            .volume(1).build();

        CaseDetails caseDetails = CaseDetails.caseDetailsWith()
            .caseReference("testCaseReference")
            .ccdCaseNumber("1111222233334444")
            .build();

        PaymentFeeLink order = PaymentFeeLink
            .paymentFeeLinkWith().fees(Arrays.asList(fee1,fee2))
            .caseDetails(Stream.of(caseDetails).collect(Collectors.toSet()))
            .ccdCaseNumber("1111222233334444")
            .build();


        when(orderDomainService.find("2020-1619524583862")).thenReturn(order);

        //Payment amount should not be matching with fees
        MvcResult result = restActions
            .withHeader("idempotency_key", UUID.randomUUID().toString())
            .post("/order/" + "2020-1619524583862" + "/credit-account-payment", orderPaymentDto)
            .andExpect(status().isBadRequest())
            .andExpect(orderException -> assertTrue(orderException.getResolvedException() instanceof OrderException))
            .andExpect(orderException -> assertEquals("Payment amount not matching with fees", orderException.getResolvedException().getMessage()))
            .andReturn();

    }

    @Test
    public void createPBAPaymentNoAmountDueTest() throws Exception {

        //Order Payment DTO
        OrderPaymentDto orderPaymentDto = OrderPaymentDto
            .paymentDtoWith().accountNumber("PBA12345")
            .amount(BigDecimal.valueOf(100))
            .currency(CurrencyCode.valueOf("GBP"))
            .customerReference("testCustReference").
                build();

        //Amount Due 0
        PaymentFee fee1 = PaymentFee.feeWith().code("X101")
            .version("1")
            .calculatedAmount(BigDecimal.valueOf(100))
            .amountDue(BigDecimal.ZERO)
            .volume(1).build();


        CaseDetails caseDetails = CaseDetails.caseDetailsWith()
            .caseReference("testCaseReference")
            .ccdCaseNumber("1111222233334444")
            .build();

        PaymentFeeLink order = PaymentFeeLink
            .paymentFeeLinkWith().fees(Arrays.asList(fee1))
            .caseDetails(Stream.of(caseDetails).collect(Collectors.toSet()))
            .ccdCaseNumber("1111222233334444")
            .build();


        when(orderDomainService.find("2020-1619524583863")).thenReturn(order);

        //No Amount due check
        MvcResult result = restActions
            .withHeader("idempotency_key", UUID.randomUUID().toString())
            .post("/order/" + "2020-1619524583863" + "/credit-account-payment", orderPaymentDto)
            .andExpect(status().isBadRequest())
            .andExpect(orderException -> assertTrue(orderException.getResolvedException() instanceof OrderException))
            .andExpect(orderException -> assertEquals("No fee amount due for payment for this order", orderException.getResolvedException().getMessage()))
            .andReturn();

    }

    @Test
    public void createOrderWithValidRequest() throws Exception {

        OrderDto orderDto = OrderDto.orderDtoWith()
            .caseReference("123245677")
            .caseType("MoneyClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(Collections.singletonList(getFee()))
            .build();

        String orderReference = "2200-1619524583862";

        Map<String, Object> orderResponse = new HashMap<>();
        orderResponse.put("order_reference",orderReference);
        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();

        when(orderDomainService.create(any(), any())).thenReturn(orderResponse);

        MvcResult result = restActions
            .post("/order", orderDto)
            .andExpect(status().isCreated())
            .andReturn();

        Map orderReferenceResult = objectMapper.readValue(result.getResponse().getContentAsByteArray(),Map.class);

        assertThat(orderReference).isEqualTo(orderReferenceResult.get("order_reference"));

    }

    @Test
    public void createOrderWithInValidCcdCaseNumber() throws Exception {

        OrderDto orderDto = OrderDto.orderDtoWith()
            .caseReference("123245677")
            .caseType("MoneyClaimCase")
            .ccdCaseNumber("689869686968696")
            .fees(Collections.singletonList(getFee()))
            .build();


        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();

        restActions
            .post("/order", orderDto)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string("ccdCaseNumber: ccd_case_number should be 16 digit"));

    }

    @Test
    public void createOrderWithDuplicateFees() throws Exception {

        List<OrderFeeDto> orderFeeDtoList = new ArrayList<OrderFeeDto>();
        orderFeeDtoList.add(getFee());
        orderFeeDtoList.add(getFee());

        OrderDto orderDto = OrderDto.orderDtoWith()
            .caseReference("123245677")
            .caseType("MoneyClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(orderFeeDtoList)
            .build();

        restActions
            .post("/order", orderDto)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string("feeCodeUnique: Fee code cannot be duplicated"));
    }

    @Test
    public void createOrderWithInvalidCaseType() throws Exception {

        OrderDto orderDto = OrderDto.orderDtoWith()
            .caseReference("123245677")
            .caseType("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(Collections.singletonList(getFee()))
            .build();

        when(orderDomainService.create(any(),any())).thenThrow(new NoServiceFoundException("Test Error"));

        restActions
            .post("/order", orderDto)
            .andExpect(status().isNotFound())
            .andExpect(content().string("Test Error"));
    }

    @Test
    public void createOrderWithValidCaseTypeReturnsTimeOutException() throws Exception {

        OrderDto orderDto = OrderDto.orderDtoWith()
            .caseReference("123245677")
            .caseType("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(Collections.singletonList(getFee()))
            .build();

        when(orderDomainService.create(any(),any())).thenThrow(new GatewayTimeoutException("Test Error"));

        restActions
            .post("/order", orderDto)
            .andExpect(status().isGatewayTimeout())
            .andExpect(content().string("Test Error"));
    }

    private OrderFeeDto getFee() {
        return OrderFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("92.19"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }

    private List<OrderFeeDto> getMultipleFees() {
        OrderFeeDto fee1 = OrderFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("100"))
            .code("FEE100")
            .version("1")
            .volume(1)
            .build();

        OrderFeeDto fee2 = OrderFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("200"))
            .code("FEE102")
            .version("1")
            .volume(1)
            .build();

        return Arrays.asList(fee1, fee2);
    }

}
