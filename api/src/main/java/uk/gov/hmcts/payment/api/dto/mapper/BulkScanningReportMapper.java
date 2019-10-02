package uk.gov.hmcts.payment.api.dto.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.BulkScanningReportDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;

@Component
public class BulkScanningReportMapper {

    public BulkScanningReportDto toBulkScanningReportdto(Payment payment) {

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
}
