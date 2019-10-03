package uk.gov.hmcts.payment.api.dto.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.BulkScanningReportDto;
import uk.gov.hmcts.payment.api.contract.BulkScanningUnderOverPaymentDto;
import uk.gov.hmcts.payment.api.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class BulkScanningReportMapper {

    public BulkScanningReportDto toBulkScanningUnallocatedReportdto(Payment payment) {

        BulkScanningReportDto bulkScanningReportDto = new BulkScanningReportDto();

                if(payment.getPaymentProvider().getName().equals("exela")) {
                    for (PaymentAllocation paymentAllocation : payment.getPaymentAllocation()) {
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
                    }
                }
        return bulkScanningReportDto;

    }

    public List<BulkScanningUnderOverPaymentDto> toSurplusAndShortfallReportdto(List<PaymentFeeLink> paymentFeeLinks) {
        BigDecimal totalAmount = new BigDecimal(0);
        List<BulkScanningUnderOverPaymentDto> underOverPaymentDtos = new ArrayList<>();

        for(PaymentFeeLink paymentFeeLink : paymentFeeLinks) {
            BulkScanningUnderOverPaymentDto bulkScanningUnderOverPaymentDto = new BulkScanningUnderOverPaymentDto();
            BigDecimal feeAmount = calculateFeeAmount(paymentFeeLink.getFees());
            //totalFee = totalFee.add(feeAmount);

            BigDecimal remissionAmount = calculateRemissionAmount(paymentFeeLink.getRemissions());
            //totalRemission = totalRemission.add(remissionAmount);

            BigDecimal paymentAmount = calculatePaymentAmount(paymentFeeLink.getPayments());
            //totalPayment = totalPayment.add(paymentAmount);

            totalAmount = feeAmount.subtract((paymentAmount.add(remissionAmount)));
            if(totalAmount.compareTo(BigDecimal.ZERO) != 0)
          {
            for (Payment payment : paymentFeeLink.getPayments()) {
                bulkScanningUnderOverPaymentDto.setRespServiceId(payment.getSiteId());
                bulkScanningUnderOverPaymentDto.setRespServiceName(payment.getServiceType());
                bulkScanningUnderOverPaymentDto.setCcdCaseReference(payment.getCcdCaseNumber());
            }
              bulkScanningUnderOverPaymentDto.setBalance(totalAmount);
              bulkScanningUnderOverPaymentDto.setPaymentAmount(paymentAmount);
              bulkScanningUnderOverPaymentDto.setSurplusShortfall(totalAmount.compareTo(BigDecimal.ZERO) > 0 ? "Surplus" : "Shortfall");
              bulkScanningUnderOverPaymentDto.setProcessedDate(paymentFeeLink.getDateUpdated());
              underOverPaymentDtos.add(bulkScanningUnderOverPaymentDto);
        }
        }
    return underOverPaymentDtos;
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
