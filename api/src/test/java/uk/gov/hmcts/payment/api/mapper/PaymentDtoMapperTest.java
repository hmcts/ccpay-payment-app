package uk.gov.hmcts.payment.api.mapper;

import org.ff4j.FF4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;
import uk.gov.hmcts.fees2.register.api.contract.Jurisdiction1Dto;
import uk.gov.hmcts.fees2.register.api.contract.Jurisdiction2Dto;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentAllocationDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PaymentDtoMapperTest {

    @Mock
    FeesService feesService;

    @Mock
    LaunchDarklyFeatureToggler featureToggler;

    @Mock
    FF4j ff4j;

    @InjectMocks
    PaymentDtoMapper paymentDtoMapper = new PaymentDtoMapper();

    PaymentFeeLink paymentFeeLink, paymentFeeLinkMultiplePayments;

    Payment payment1, multiplePayment1, multiplePayment2;

    List<PaymentFee> paymentFees;

    PaymentAllocation allocation1;

    Optional<FeeVersionDto> feeVersionDto;

    @Before
    public void initiate(){
        List<Payment> payments = new ArrayList<Payment>();
        List<Payment> multiplePayments = new ArrayList<Payment>();
        List<PaymentAllocation> paymentAllocations1 = new ArrayList<PaymentAllocation>();
        allocation1 = PaymentAllocation.paymentAllocationWith()
            .receivingOffice("receiving-office")
            .id(1)
            .unidentifiedReason("reason")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred").build()).build();
        paymentAllocations1.add(allocation1);
        StatusHistory statusHistory = StatusHistory.statusHistoryWith()
            .status("success")
            .externalStatus("external-status")
            .errorCode("")
            .message("")
            .dateCreated(new Date(2021,1,1))
            .dateUpdated(new Date(2021,1,10))
            .build();
        List<StatusHistory> statusHistories = new ArrayList<StatusHistory>();
        statusHistories.add(statusHistory);
        PaymentFee fee = PaymentFee.feeWith()
            .feeAmount(new BigDecimal("100.00"))
            .ccdCaseNumber("ccd-case-number")
            .calculatedAmount(new BigDecimal("100.00"))
            .code("FEE123")
            .version("1")
            .volume(1)
            .build();
        paymentFees = new ArrayList<PaymentFee>();
        paymentFees.add(fee);
        payment1 = Payment.paymentWith()
            .siteId("siteId")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .serviceType("service-type")
            .caseReference("case-reference")
            .reference("RC-1612-3710-5335-6484")
            .ccdCaseNumber("ccd-case-number")
            .status("success")
            .bankedDate(new Date(2021,1,1))
            .documentControlNumber("document-control-number")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("pay-method").build())
            .amount(new BigDecimal("100.00"))
            .paymentStatus(PaymentStatus.SUCCESS)
            .dateCreated(new Date(2020,10,1))
            .paymentAllocation(paymentAllocations1)
            .currency("GBP")
            .statusHistories(statusHistories)
            .id(1).build();
        multiplePayment1 = Payment.paymentWith()
            .siteId("siteId")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .serviceType("service-type")
            .caseReference("case-reference")
            .reference("RC-1612-3710-5335-6484")
            .ccdCaseNumber("ccd-case-number")
            .status("success")
            .bankedDate(new Date(2021,1,1))
            .documentControlNumber("document-control-number")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("pay-method").build())
            .amount(new BigDecimal("100.00"))
            .paymentStatus(PaymentStatus.SUCCESS)
            .dateCreated(new Date(2020,10,1))
            .paymentAllocation(paymentAllocations1)
            .currency("GBP")
            .statusHistories(statusHistories)
            .id(2).build();
        multiplePayment2 = Payment.paymentWith()
            .siteId("siteId")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .serviceType("service-type")
            .caseReference("case-reference")
            .reference("RC-1612-3710-5335-6490")
            .ccdCaseNumber("ccd-case-number")
            .status("success")
            .bankedDate(new Date(2021,1,1))
            .documentControlNumber("document-control-number")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("pay-method").build())
            .amount(new BigDecimal("100.00"))
            .paymentStatus(PaymentStatus.FAILED)
            .dateCreated(new Date(2020,10,1))
            .paymentAllocation(paymentAllocations1)
            .currency("GBP")
            .statusHistories(statusHistories)
            .id(3).build();
        payments.add(payment1);
        multiplePayments.add(multiplePayment1);
        multiplePayments.add(multiplePayment2);
        paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
                            .paymentReference("group-reference")
                            .dateCreated(new Date(2021,1,1))
                            .fees(paymentFees)
                            .payments(payments).build();
        paymentFeeLinkMultiplePayments = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("group-reference")
            .dateCreated(new Date(2021,1,1))
            .fees(paymentFees)
            .payments(multiplePayments).build();
        payment1.setPaymentLink(paymentFeeLink);
        multiplePayment1.setPaymentLink(paymentFeeLinkMultiplePayments);
        multiplePayment2.setPaymentLink(paymentFeeLinkMultiplePayments);
        feeVersionDto = Optional.of(FeeVersionDto.feeVersionDtoWith()
            .version(Integer.valueOf("1"))
            .memoLine(" memo line")
            .naturalAccountCode(" 1234567")
            .build());
    }

    @Test
    public void testToCardPaymentDto(){
        PaymentDto paymentDto = paymentDtoMapper.toCardPaymentDto(paymentFeeLink);
        assertEquals("RC-1612-3710-5335-6484",paymentDto.getReference());
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
    }

    @Test
    public void testToBulkScanPaymentDto(){
        PaymentDto paymentDto = paymentDtoMapper.toBulkScanPaymentDto(payment1,"group-reference");
        assertEquals("RC-1612-3710-5335-6484",paymentDto.getReference());
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
    }

    @Test
    public void testToBulkScanPaymentStrategicDto(){
        PaymentDto paymentDto = paymentDtoMapper.toBulkScanPaymentStrategicDto(payment1,"group-reference");
        assertEquals("RC-1612-3710-5335-6484",paymentDto.getReference());
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
    }

    @Test
    public void testToPciPalCardPaymentDto(){
        PaymentDto paymentDto = paymentDtoMapper.toPciPalCardPaymentDto(paymentFeeLink,"http://hmcts.internal");
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertEquals("http://hmcts.internal",paymentDto.getLinks().getNextUrl().getHref());
    }

    @Test
    public void testToResponseDto(){
        PaymentDto paymentDto = paymentDtoMapper.toResponseDto(paymentFeeLink,payment1);
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertEquals("service-type",paymentDto.getServiceName());
    }

    @Test
    public void testToRetrieveCardPaymentResponseDto(){
        PaymentDto paymentDto = paymentDtoMapper.toRetrieveCardPaymentResponseDto(paymentFeeLink, payment1.getReference());
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertTrue(PaymentStatus.SUCCESS.getName().equalsIgnoreCase(paymentDto.getStatus()));
        assertEquals("RC-1612-3710-5335-6484",paymentDto.getReference());
        assertEquals("service-type",paymentDto.getServiceName());
    }

    @Test
    public void testToRetrieveCardPaymentResponseDtoMultipleReferences1(){
        PaymentDto paymentDto = paymentDtoMapper.toRetrieveCardPaymentResponseDto(paymentFeeLinkMultiplePayments, multiplePayment1.getReference());
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertTrue(PaymentStatus.SUCCESS.getName().equalsIgnoreCase(paymentDto.getStatus()));
        assertEquals("RC-1612-3710-5335-6484",paymentDto.getReference());
        assertEquals("service-type",paymentDto.getServiceName());
    }

    @Test
    public void testToRetrieveCardPaymentResponseDtoMultipleReferences2(){
        PaymentDto paymentDto = paymentDtoMapper.toRetrieveCardPaymentResponseDto(paymentFeeLinkMultiplePayments, multiplePayment2.getReference());
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertTrue(PaymentStatus.FAILED.getName().equalsIgnoreCase(paymentDto.getStatus()));
        assertEquals("RC-1612-3710-5335-6490",paymentDto.getReference());
        assertEquals("service-type",paymentDto.getServiceName());
    }

    @Test(expected = PaymentNotFoundException.class)
    public void testToRetrieveCardPaymentResponseDtoMissingReference(){
        PaymentDto paymentDto = paymentDtoMapper.toRetrieveCardPaymentResponseDto(paymentFeeLink, "RC-0000-0000-0000-0000");
    }

    @Test
    public void testToRetrievePaymentStatusesDto(){
        PaymentDto paymentDto = paymentDtoMapper.toRetrievePaymentStatusesDto(paymentFeeLink, payment1.getReference());
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertTrue(PaymentStatus.SUCCESS.getName().equalsIgnoreCase(paymentDto.getStatus()));
        assertEquals("RC-1612-3710-5335-6484",paymentDto.getReference());
        assertEquals("100.00",paymentDto.getAmount().toString());
    }

    @Test
    public void testToRetrievePaymentStatusesDtoMultipleReferences1(){
        PaymentDto paymentDto = paymentDtoMapper.toRetrievePaymentStatusesDto(paymentFeeLinkMultiplePayments, multiplePayment1.getReference());
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertTrue(PaymentStatus.SUCCESS.getName().equalsIgnoreCase(paymentDto.getStatus()));
        assertEquals("RC-1612-3710-5335-6484",paymentDto.getReference());
        assertEquals("100.00",paymentDto.getAmount().toString());
    }

    @Test
    public void testToRetrievePaymentStatusesDtoMultipleReferences2(){
        PaymentDto paymentDto = paymentDtoMapper.toRetrievePaymentStatusesDto(paymentFeeLinkMultiplePayments, multiplePayment2.getReference());
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertTrue(PaymentStatus.FAILED.getName().equalsIgnoreCase(paymentDto.getStatus()));
        assertEquals("RC-1612-3710-5335-6490",paymentDto.getReference());
        assertEquals("100.00",paymentDto.getAmount().toString());
    }

    @Test(expected = PaymentNotFoundException.class)
    public void testToRetrievePaymentStatusesDtoMissingReference(){
        PaymentDto paymentDto = paymentDtoMapper.toRetrievePaymentStatusesDto(paymentFeeLink, "RC-0000-0000-0000-0000");
    }

    @Test
    public void testToPaymentStatusesDto(){
        PaymentDto paymentDto = paymentDtoMapper.toPaymentStatusesDto(payment1);
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertEquals("100.00",paymentDto.getAmount().toString());

    }

    @Test
    public void testToGetPaymentResponseDtos(){
        PaymentDto paymentDto = paymentDtoMapper.toGetPaymentResponseDtos(payment1);
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertEquals("service-type",paymentDto.getServiceName());
    }

    @Test
    public void testToReconciliationResponseDto(){
        PaymentDto paymentDto = paymentDtoMapper.toReconciliationResponseDto(paymentFeeLink);
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
    }

    @Test
    public void testToCreateRecordPaymentResponse(){
        PaymentDto paymentDto = paymentDtoMapper.toCreateRecordPaymentResponse(paymentFeeLink);
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
        assertEquals("group-reference",paymentDto.getPaymentGroupReference());
    }

    @Test
    public void testToPaymentAllocationDto(){
        PaymentAllocationDto paymentAllocationDto = paymentDtoMapper.toPaymentAllocationDto(allocation1);
        assertEquals("1",paymentAllocationDto.getId());
        assertEquals("receiving-office",paymentAllocationDto.getReceivingOffice());
    }

    @Test
    public void testToPaymentAllocationDtoForLiberata(){
        PaymentAllocationDto paymentAllocationDto = paymentDtoMapper.toPaymentAllocationDtoForLibereta(allocation1);
        assertEquals("transferred",paymentAllocationDto.getAllocationStatus());
        assertEquals("receiving-office",paymentAllocationDto.getReceivingOffice());
    }

    @Test
    public void testToPaymentAllocationDtos(){
        PaymentAllocationDto paymentAllocationDto = paymentDtoMapper.toPaymentAllocationDtos(allocation1);
        assertEquals("Transferred",paymentAllocationDto.getAllocationStatus());
    }

    @Test
    public void testToRetrieveCardPaymentResponseDtoWithoutExtReference() {
        PaymentMethod paymentMethod = PaymentMethod.paymentMethodWith().name("online").build();

        Payment payment = Payment.paymentWith().internalReference("abc")
            .id(1)
            .reference("RC-1632-3254-9172-5888")
            .caseReference("123789")
            .paymentMethod(paymentMethod )
            .ccdCaseNumber("1234")
            .amount(new BigDecimal(300))
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .build();

        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().ccdCaseNumber("1234")
            .enterpriseServiceName("divorce")
            .payments(paymentList)
            .paymentReference("123456")
            .build();

        PaymentDto paymentDto = paymentDtoMapper.toRetrieveCardPaymentResponseDtoWithoutExtReference(paymentFeeLink, "abc");
        assertEquals(paymentDto.getCaseReference(), payment.getCaseReference());
    }

    @Test
    public void testMemoLineAndNaturalAccountCodeValidation() {
        when(feesService.getFeeVersion(eq("FEE123"), eq("1"))).thenReturn(feeVersionDto);
        when(ff4j.check(any(String.class))).thenReturn(true);

        // Mock the fee map so enrichment works
        Map<String, uk.gov.hmcts.fees2.register.api.contract.Fee2Dto> feeMap = new HashMap<>();
        Fee2Dto fee2Dto = Fee2Dto.fee2DtoWith()
            .jurisdiction1Dto(Jurisdiction1Dto.jurisdiction1TypeDtoWith().name("J1").build())
            .jurisdiction2Dto(Jurisdiction2Dto.jurisdiction2TypeDtoWith().name("J1").build())
            .build();
        feeMap.put("FEE123", fee2Dto);
        when(feesService.getFeesDtoMap()).thenReturn(feeMap);

        PaymentDto paymentDto = paymentDtoMapper.toReconciliationResponseDtoForLibereta(payment1, "group-reference", paymentFees, ff4j, true);
        FeeDto feeDto = paymentDto.getFees().get(0);
        // Assert: values are trimmed
        assertEquals("memo line", feeDto.getMemoLine());
        assertEquals("1234567", feeDto.getNaturalAccountCode());
    }
}
