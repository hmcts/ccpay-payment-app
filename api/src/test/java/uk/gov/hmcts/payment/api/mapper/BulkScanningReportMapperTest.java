package uk.gov.hmcts.payment.api.mapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.contract.BulkScanningReportDto;
import uk.gov.hmcts.payment.api.contract.BulkScanningUnderOverPaymentDto;
import uk.gov.hmcts.payment.api.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BulkScanningReportMapperTest {
    List<Payment> payments = new ArrayList<Payment>();


    BulkScanningReportMapper bulkScanningReportMapper = new BulkScanningReportMapper();

    Payment payment2;
    Payment payment1;
    PaymentFeeLink feeLink;

    @Before
    public void initiate(){
        List<PaymentAllocation> paymentAllocations1 = new ArrayList<PaymentAllocation>();
        List<PaymentAllocation> paymentAllocations2 = new ArrayList<PaymentAllocation>();
        PaymentAllocation allocation1 = PaymentAllocation.paymentAllocationWith()
                                            .receivingOffice("receiving-office")
                                            .unidentifiedReason("reason")
                                            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred").build()).build();
        PaymentAllocation allocation2 = PaymentAllocation.paymentAllocationWith()
            .receivingOffice("receiving-office")
            .unidentifiedReason("reason")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Allocated").build()).build();
        paymentAllocations1.add(allocation1);
        paymentAllocations2.add(allocation2);
        Payment payment1 = Payment.paymentWith()
                                .siteId("siteId")
                                .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
                                .serviceType("service-type")
                                .caseReference("case-reference")
                                .ccdCaseNumber("ccd-case-number")
                                .bankedDate(new Date(2021,1,1))
                                .documentControlNumber("document-control-number")
                                .paymentMethod(PaymentMethod.paymentMethodWith().name("pay-method").build())
                                .amount(new BigDecimal("100.00"))
                                .paymentStatus(PaymentStatus.SUCCESS)
                                .dateCreated(new Date(2020,10,1))
                                .id(1).paymentAllocation(paymentAllocations1).build();
        Payment payment2 = Payment.paymentWith()
            .siteId("siteId")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("non-exela").build())
            .serviceType("service-type")
            .caseReference("case-reference")
            .ccdCaseNumber("ccd-case-number")
            .bankedDate(new Date(2021,1,1))
            .documentControlNumber("document-control-number")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("pay-method").build())
            .amount(new BigDecimal("100.00"))
            .paymentStatus(PaymentStatus.SUCCESS)
            .dateCreated(new Date(2020,10,1))
            .id(2).paymentAllocation(paymentAllocations1).build();
        Payment payment3 = Payment.paymentWith()
            .siteId("siteId")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .serviceType("service-type")
            .caseReference("case-reference")
            .ccdCaseNumber("ccd-case-number")
            .bankedDate(new Date(2021,1,1))
            .documentControlNumber("document-control-number")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("pay-method").build())
            .amount(new BigDecimal("100.00"))
            .paymentStatus(PaymentStatus.SUCCESS)
            .dateCreated(new Date(2020,10,1))
            .id(1).paymentAllocation(paymentAllocations2).build();
        payments.add(payment1);
        payments.add(payment2);
        payments.add(payment3);
        PaymentFee fee = PaymentFee.feeWith().feeAmount(new BigDecimal("100.00")).ccdCaseNumber("ccd-case-number").calculatedAmount(new BigDecimal("100.00")).build();
        List<PaymentFee> paymentFees = new ArrayList<PaymentFee>();
        paymentFees.add(fee);
        Remission remission = Remission.remissionWith()
                                .hwfAmount(new BigDecimal("10.00"))
                                .build();
        List<Remission> remissionList = new ArrayList<Remission>();
        remissionList.add(remission);
        feeLink = PaymentFeeLink.paymentFeeLinkWith()
                                    .fees(paymentFees)
                                    .payments(payments)
                                    .remissions(remissionList)
                                    .build();
        payment1.setPaymentLink(feeLink);
        payment2.setPaymentLink(feeLink);
        payment3.setPaymentLink(feeLink);

    }

    @Test
    public void testToBulkScanningUnallocatedReportDto(){
        List<BulkScanningReportDto> bulkScanningReportDtos = new ArrayList<>();
        BulkScanningReportDto bulkScanningReportDto = BulkScanningReportDto.report2DtoWith()
                                                        .respServiceId("siteId")
                                                        .respServiceName("service-type")
                                                        .ccdExceptionReference("case-reference")
                                                        .build();
        bulkScanningReportDtos.add(bulkScanningReportDto);
        List<BulkScanningReportDto> responseDtos = bulkScanningReportMapper.toBulkScanningUnallocatedReportDto(payments);
        assertEquals(responseDtos.get(0).getRespServiceName(),bulkScanningReportDtos.get(0).getRespServiceName());
    }

    @Test
    public void testToBulkScanningUnallocatedReportDtoFilteringExelaPayments() {
        List<BulkScanningReportDto> responseDtos = bulkScanningReportMapper.toBulkScanningUnallocatedReportDto(payments);
        assertEquals(1,responseDtos.size());
    }

    @Test
    public void testToSurplusAndShortfallReportdto(){
        List<BulkScanningUnderOverPaymentDto> underOverPaymentDtos = new ArrayList<>();
        BulkScanningUnderOverPaymentDto bulkScanningUnderOverPaymentDto = BulkScanningUnderOverPaymentDto.report2DtoWith()
                                                                            .respServiceId("siteId")
                                                                            .respServiceName("service-type")
                                                                            .ccdExceptionReference("case-reference")
                                                                            .build();
        underOverPaymentDtos.add(bulkScanningUnderOverPaymentDto);
        List<BulkScanningUnderOverPaymentDto> responseOverPaymentDtos = bulkScanningReportMapper.toSurplusAndShortfallReportdto(payments);
        assertEquals(responseOverPaymentDtos.get(0).getRespServiceName(),underOverPaymentDtos.get(0).getRespServiceName());
    }

    @Test
    public void testToSurplusAndShortfallReportdtoFilteringExelaPayments(){
        List<BulkScanningUnderOverPaymentDto> responseDtos = bulkScanningReportMapper.toSurplusAndShortfallReportdto(payments);
        assertEquals(1,responseDtos.size());
    }

}
