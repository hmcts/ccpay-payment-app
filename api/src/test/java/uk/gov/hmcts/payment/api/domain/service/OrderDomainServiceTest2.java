package uk.gov.hmcts.payment.api.domain.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.domain.mapper.OrderDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.OrderDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.model.OrderBo;
import uk.gov.hmcts.payment.api.domain.model.OrderFeeBo;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.order.OrderDto;
import uk.gov.hmcts.payment.api.dto.order.OrderFeeDto;
import uk.gov.hmcts.payment.api.model.CaseDetails;
import uk.gov.hmcts.payment.api.model.CaseDetailsRepository;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.service.ReferenceDataServiceImpl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class OrderDomainServiceTest2 {

    @InjectMocks
    private OrderDomainServiceImpl orderDomainService;

    @Mock
    private ReferenceDataServiceImpl referenceDataService;

    @Spy
    private OrderDtoDomainMapper orderDtoDomainMapper;

    @Mock
    private OrderBo orderBo;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Before
    public void setup() {

    }

    @Test
    public void createOrderWithValidRequest() throws Exception {

        OrderDto orderDto = OrderDto.orderDtoWith()
            .caseReference("123245677")
            .caseType("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(Collections.singletonList(getOrderFee()))
            .build();

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceCode("AA001")
            .serviceDescription("DIVORCE")
            .build();

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();

        when(referenceDataService.getOrganisationalDetail(any(), any())).thenReturn(organisationalServiceDto);

        when(caseDetailsRepository.existsByCcdCaseNumber(anyString())).thenReturn(true);

        CaseDetails caseDetails = CaseDetails.caseDetailsWith()
            .caseReference("rertyuilkjhcxdfgh")
            .ccdCaseNumber("8696869686968696")
            .build();

        when(caseDetailsRepository.findByCcdCaseNumber(anyString())).thenReturn(caseDetails);

        String orderReference = "2200-1619524583862";

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .orgId("AA001")
            .enterpriseServiceName("DIVORCE")
            .paymentReference("12345678")
            .caseDetails(new HashSet<>())
            .ccdCaseNumber("8696869686968696")
            .fees(Collections.singletonList(getFee()))
            .build();

        when(paymentFeeLinkRepository.save(any())).thenReturn(paymentFeeLink);

       doReturn(orderReference).when(orderBo).createOrder(any());

        String orderReferenceResult = orderDomainService.create(orderDto, header);

        assertThat(orderReference).isEqualTo(orderReference);

    }

    private PaymentFee getFee() {
        return PaymentFee.feeWith()
            .calculatedAmount(new BigDecimal("92.19"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }


    private OrderFeeDto getOrderFee() {
        return OrderFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("92.19"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }

}
