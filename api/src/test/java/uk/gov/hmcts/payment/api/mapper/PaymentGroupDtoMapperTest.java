package uk.gov.hmcts.payment.api.mapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.domain.service.FeeDomainService;
import uk.gov.hmcts.payment.api.domain.service.PaymentDomainService;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.reports.FeesService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PaymentGroupDtoMapperTest {

    @Mock
    FeesService feesService;

    @Mock
    LaunchDarklyFeatureToggler featureToggler;

    @MockBean
    FeeDomainService feeDomainService;

    @MockBean
    PaymentDomainService paymentDomainService;

    @InjectMocks
    PaymentGroupDtoMapper paymentGroupDtoMapper = new PaymentGroupDtoMapper();

    PaymentFeeLink feeLink;

    @Before
    public void initiate(){

        List<Payment> payments = new ArrayList<Payment>();
        payments.add(getPayment());
        PaymentFee fee = PaymentFee.feeWith().feeAmount(new BigDecimal("100.00")).ccdCaseNumber("ccd-case-number").calculatedAmount(new BigDecimal("100.00")).build();
        Remission remission = Remission.remissionWith()
            .hwfAmount(new BigDecimal("10.00"))
            .fee(fee)
            .build();
        List<Remission> remissionList = new ArrayList<Remission>();
        remissionList.add(remission);

        List<PaymentFee> paymentFees = new ArrayList<PaymentFee>();
        paymentFees.add(fee);
        feeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("payment-reference")
            .fees(paymentFees)
            .payments(payments)
            .remissions(remissionList)
            .build();
    }

    @Test
    public void testToPaymentFee(){
        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal(("100.00")))
            .code("FEE123")
            .version("1")
            .volume(1)
            .feeAmount(new BigDecimal("50.00"))
            .netAmount(new BigDecimal("50.00"))
            .dateCreated(new Date(2020,10,1))
            .ccdCaseNumber("123456789012345")
            .reference("RC-1612-3710-5335-6484")
            .build();
        PaymentFee paymentFee = paymentGroupDtoMapper.toPaymentFee(feeDto);
        assertEquals("123456789012345",paymentFee.getCcdCaseNumber());
        assertEquals("FEE123",paymentFee.getCode());
    }

    private Payment getPayment(){
        List<PaymentAllocation> paymentAllocations1 = new ArrayList<PaymentAllocation>();
        PaymentAllocation allocation1 = PaymentAllocation.paymentAllocationWith()
            .receivingOffice("receiving-office")
            .unidentifiedReason("reason")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred").build()).build();
        paymentAllocations1.add(allocation1);
        return Payment.paymentWith()
            .siteId("siteId")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .serviceType("service-type")
            .caseReference("case-reference")
            .reference("RC-1612-3710-5335-6484")
            .ccdCaseNumber("ccd-case-number")
            .bankedDate(new Date(2021,1,1))
            .documentControlNumber("document-control-number")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("pay-method").build())
            .amount(new BigDecimal("100.00"))
            .paymentStatus(PaymentStatus.SUCCESS)
            .dateCreated(new Date(2020,10,1))
            .currency("GBP")
            .paymentAllocation(paymentAllocations1)
            .id(1).build();
    }


}
