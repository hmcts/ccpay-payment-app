package uk.gov.hmcts.payment.api.dto.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.BulkScanningReportDto;
import uk.gov.hmcts.payment.api.contract.BulkScanningUnderOverPaymentDto;
import uk.gov.hmcts.payment.api.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class BulkScanningReportMapper {

    private static final Logger LOG = LoggerFactory.getLogger(BulkScanningReportMapper.class);

    public List<BulkScanningReportDto> toBulkScanningUnallocatedReportDto(Payment payment,List<BulkScanningReportDto> bulkScanningReportDtoList) {
        LOG.info("Inside BulkScanningReportMapper");
        BulkScanningReportDto bulkScanningReportDto = new BulkScanningReportDto();

                if(payment.getPaymentProvider()!=null && payment.getPaymentProvider().getName().equals("exela")) {
                    LOG.info("Payment provider is exela and not null");
                    for (PaymentAllocation paymentAllocation : payment.getPaymentAllocation()) {
                        String allocationStatus = paymentAllocation.getPaymentAllocationStatus().getName();
                     if(allocationStatus.equals("Transferred") || allocationStatus.equals("Unidentified")) {
                         LOG.info("Allocation status is Transferred or Unidentified");
                         bulkScanningReportDto.setRespServiceId(payment.getSiteId());
                         bulkScanningReportDto.setRespServiceName(payment.getServiceType());
                         bulkScanningReportDto.setAllocationStatus(paymentAllocation.getPaymentAllocationStatus().getName());
                         bulkScanningReportDto.setReceivingOffice(paymentAllocation.getReceivingOffice());
                         bulkScanningReportDto.setAllocationReason(paymentAllocation.getUnidentifiedReason());
                         bulkScanningReportDto.setCcdExceptionReference(payment.getCaseReference());
                         bulkScanningReportDto.setCcdCaseReference(payment.getCcdCaseNumber());
                         bulkScanningReportDto.setDateBanked(payment.getBankedDate());
                         bulkScanningReportDto.setBgcBatch(payment.getGiroSlipNo());
                         bulkScanningReportDto.setPaymentAssetDCN(payment.getDocumentControlNumber());
                         bulkScanningReportDto.setPaymentMethod(payment.getPaymentMethod().getName());
                         bulkScanningReportDto.setAmount(payment.getAmount().toString());
                         bulkScanningReportDtoList.add(bulkScanningReportDto);
                     }
                    }
                    bulkScanningReportDtoList.sort(Comparator.comparing(BulkScanningReportDto::getRespServiceId).thenComparing(BulkScanningReportDto::getAllocationStatus));
                    LOG.info("Unallocated Report list size : {}",bulkScanningReportDtoList.size());
                }
        return bulkScanningReportDtoList;

    }

    public List<BulkScanningUnderOverPaymentDto> toSurplusAndShortfallReportdto(List<PaymentFeeLink> paymentFeeLinks) {
        List<BulkScanningUnderOverPaymentDto> underOverPaymentDtos = new ArrayList<>();
        LOG.info("paymentFeeLinks list size. : {}",paymentFeeLinks.size());
        for(PaymentFeeLink paymentFeeLink : paymentFeeLinks) {
            for (Payment payment : paymentFeeLink.getPayments()) {
                if (payment.getPaymentProvider() != null && payment.getPaymentProvider().getName().equals("exela")) {

                    BigDecimal feeAmount = calculateFeeAmount(paymentFeeLink.getFees());

                    BigDecimal remissionAmount = calculateRemissionAmount(paymentFeeLink.getRemissions());

                    BigDecimal paymentAmount = calculatePaymentAmount(paymentFeeLink.getPayments());

                    BigDecimal totalAmount = feeAmount.subtract((paymentAmount.add(remissionAmount)));
                    generateData(underOverPaymentDtos, paymentFeeLink, payment, paymentAmount, totalAmount);
                }
            }
        }
        underOverPaymentDtos.sort(Comparator.comparing(BulkScanningUnderOverPaymentDto::getRespServiceId).thenComparing(BulkScanningUnderOverPaymentDto::getSurplusShortfall));
        LOG.info("Surplus and Shortfall Report list size : {}",underOverPaymentDtos.size());
    return underOverPaymentDtos;
    }

    private void generateData(List<BulkScanningUnderOverPaymentDto> underOverPaymentDtos, PaymentFeeLink paymentFeeLink, Payment payment, BigDecimal paymentAmount, BigDecimal totalAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) != 0)
        {
            LOG.info("totalAmount not Zero");
            for (PaymentAllocation paymentAllocation : payment.getPaymentAllocation()) {
                String allocationStatus = paymentAllocation.getPaymentAllocationStatus().getName();
                if (allocationStatus.equals("Allocated")) {
                    BulkScanningUnderOverPaymentDto bulkScanningUnderOverPaymentDto = new BulkScanningUnderOverPaymentDto();
                    bulkScanningUnderOverPaymentDto.setRespServiceId(payment.getSiteId());
                    bulkScanningUnderOverPaymentDto.setRespServiceName(payment.getServiceType());
                    bulkScanningUnderOverPaymentDto.setCcdCaseReference(payment.getCcdCaseNumber());
                    bulkScanningUnderOverPaymentDto.setBalance(totalAmount);
                    bulkScanningUnderOverPaymentDto.setPaymentAmount(paymentAmount);
                    bulkScanningUnderOverPaymentDto.setSurplusShortfall(totalAmount.compareTo(BigDecimal.ZERO) > 0 ? "Shortfall" : "Surplus");
                    bulkScanningUnderOverPaymentDto.setProcessedDate(paymentFeeLink.getDateUpdated());
                    bulkScanningUnderOverPaymentDto.setReason(paymentAllocation.getReason());
                    bulkScanningUnderOverPaymentDto.setExplanation(paymentAllocation.getExplanation());
                    bulkScanningUnderOverPaymentDto.setUserName(paymentAllocation.getUserName());
                    boolean caseReferenceCheck= underOverPaymentDtos.stream().anyMatch(c -> c.getCcdCaseReference().equals(payment.getCcdCaseNumber()));
                    if(!caseReferenceCheck) {
                        underOverPaymentDtos.add(bulkScanningUnderOverPaymentDto);
                    }
                }
            }
        }
    }

    private BigDecimal calculatePaymentAmount(List<Payment> payments) {

        BigDecimal paymentAmount = new BigDecimal(0);
        for(Payment payment : payments)
        {
           BigDecimal calculatedAmount = payment.getAmount();
            paymentAmount= paymentAmount.add(calculatedAmount);

        }
        return paymentAmount;
    }

    private BigDecimal calculateRemissionAmount(List<Remission> remissions) {

        BigDecimal remissionAmount = new BigDecimal(0);
        for(Remission remission : remissions)
        {
            BigDecimal calculatedAmount = remission.getHwfAmount() ;
            remissionAmount= remissionAmount.add(calculatedAmount);
        }
        return remissionAmount;
    }

    private BigDecimal calculateFeeAmount(List<PaymentFee> fees) {
        BigDecimal feeAmount = new BigDecimal(0);
        for(PaymentFee paymentFee : fees)
        {
            BigDecimal calculatedAmount = paymentFee.getCalculatedAmount() ;
            feeAmount= feeAmount.add(calculatedAmount);

        }
        return feeAmount;
    }
}
