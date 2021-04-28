package uk.gov.hmcts.payment.api.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
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
import uk.gov.hmcts.payment.api.domain.service.OrderDomainService;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.order.OrderDto;
import uk.gov.hmcts.payment.api.dto.order.OrderFeeDto;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    @MockBean
    private ReferenceDataService referenceDataService;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @InjectMocks
    private OrderController orderController;
    @MockBean
    private OrderDomainService orderDomainService;
    private RestActions restActions;
    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;
    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setup() {

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceDescription("Specified Money Claims")
            .serviceCode("AAD1")
            .build();

        when(referenceDataService.getOrganisationalDetail(any(), any())).thenReturn(organisationalServiceDto);
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
        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();

        when(orderDomainService.create(any(), any())).thenReturn(orderReference);

        MvcResult result = restActions
            .post("/order", orderDto)
            .andExpect(status().isCreated())
            .andReturn();

        String orderReferenceResult = result.getResponse().getContentAsString();

        assertThat(orderReferenceResult).isEqualTo(orderReference);

    }

    @Test
    public void createOrderWithInValidCcdCaseNmber() throws Exception {

        OrderDto orderDto = OrderDto.orderDtoWith()
            .caseReference("123245677")
            .caseType("MoneyClaimCase")
            .ccdCaseNumber("689869686968696")
            .fees(Collections.singletonList(getFee()))
            .build();


        String orderReference = "2200-1619524583862";
        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();

        when(orderDomainService.create(any(), any())).thenReturn(orderReference);

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
        ;

    }


    private OrderFeeDto getFee() {
        return OrderFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("92.19"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }
}
