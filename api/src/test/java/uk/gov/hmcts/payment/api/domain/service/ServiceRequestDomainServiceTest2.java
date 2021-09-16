package uk.gov.hmcts.payment.api.domain.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.service.ReferenceDataServiceImpl;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentGroupNotFoundException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class ServiceRequestDomainServiceTest2 {

    @InjectMocks
    private ServiceRequestDomainServiceImpl orderDomainService;

    @Mock
    private ReferenceDataServiceImpl referenceDataService;

    @Spy
    private ServiceRequestDtoDomainMapper orderDtoDomainMapper;

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Mock
    private ServiceRequestBo orderBo;

    @Before
    public void setup() {

    }

    @Test
    public void createOrderWithValidRequest() throws Exception {

        ServiceRequestDto orderDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(Collections.singletonList(getOrderFee()))
            .build();

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceCode("AA001")
            .serviceDescription("DIVORCE")
            .build();

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();

        when(referenceDataService.getOrganisationalDetail(any(),any(), any())).thenReturn(organisationalServiceDto);

        String orderReference = "2200-1619524583862";
        ServiceRequestResponseDto orderResponse = ServiceRequestResponseDto.serviceRequestResponseDtoWith()
                                            .serviceRequestReference(orderReference)
                                            .build();
        doReturn(orderResponse).when(orderBo).createServiceRequest(any());

        ServiceRequestResponseDto orderReferenceResult = orderDomainService.create(orderDto, header);

        assertThat(orderReference).isEqualTo(orderReferenceResult.getServiceRequestReference());

    }

    @Test
    public void findOrdersByCcdCaseNumber() throws Exception {
        when(paymentFeeLinkRepository.findByCcdCaseNumber(anyString())).thenReturn(Optional.of(Collections.singletonList(getPaymentFeeLink())));

        List<PaymentFeeLink> paymentFeeLinkList = orderDomainService.findByCcdCaseNumber("1607065108455502");

        assertThat(paymentFeeLinkList.get(0).getCcdCaseNumber()).isEqualTo("1607065108455502");
    }

    @Test
    public void findNoOrdersByCcdCaseNumber() throws Exception {
        when(paymentFeeLinkRepository.findByCcdCaseNumber(anyString())).thenReturn(Optional.of(Collections.emptyList()));

        try {
            List<PaymentFeeLink> paymentFeeLinkList = orderDomainService.findByCcdCaseNumber("1607065108455502");
        } catch (PaymentGroupNotFoundException e) {
            assertThat(e.getMessage()).isEqualTo("Order detail not found for given ccdcasenumber 1607065108455502");
        }
    }

    private ServiceRequestFeeDto getOrderFee() {
        return ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("92.19"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }

    private PaymentFeeLink getPaymentFeeLink() {
        return PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .orgId("org-id")
            .enterpriseServiceName("enterprise-service-name")
            .paymentReference("payment-ref")
            .ccdCaseNumber("1607065108455502")
            .fees(Arrays.asList(PaymentFee.feeWith().calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0001").volume(1).build()))
            .build();
    }

}
