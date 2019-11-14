package uk.gov.hmcts.payment.api.dto.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.BulkScanningReportDto;
import uk.gov.hmcts.payment.api.contract.BulkScanningUnderOverPaymentDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BulkScanningReportMapper {

    private static final Logger LOG = LoggerFactory.getLogger(BulkScanningReportMapper.class);

    public List<BulkScanningReportDto> toBulkScanningUnallocatedReportDto(List<Payment> payments) {
        LOG.info("Inside BulkScanningReportMapper");
        List<BulkScanningReportDto> bulkScanningReportDtoList = new ArrayList<>();

        payments = payments.stream()
            .filter(payment -> payment.getPaymentProvider().getName().equalsIgnoreCase("exela"))
            .collect(Collectors.toList());

        bulkScanningReportDtoList = payments.stream()
            .filter(payment ->
                (payment.getPaymentAllocation().get(0).getPaymentAllocationStatus().getName().equalsIgnoreCase("Transferred") ||
                    payment.getPaymentAllocation().get(0).getPaymentAllocationStatus().getName().equalsIgnoreCase("Unidentified")))
            .map(payment -> {
                BulkScanningReportDto bulkScanningReportDto = new BulkScanningReportDto();
                bulkScanningReportDto.setRespServiceId(payment.getSiteId());
                bulkScanningReportDto.setRespServiceName(payment.getServiceType());
                bulkScanningReportDto.setAllocationStatus(payment.getPaymentAllocation().get(0).getPaymentAllocationStatus().getName());
                bulkScanningReportDto.setReceivingOffice(payment.getPaymentAllocation().get(0).getReceivingOffice());
                bulkScanningReportDto.setAllocationReason(payment.getPaymentAllocation().get(0).getUnidentifiedReason());
                bulkScanningReportDto.setCcdExceptionReference(payment.getCaseReference());
                bulkScanningReportDto.setCcdCaseReference(payment.getCcdCaseNumber());
                bulkScanningReportDto.setDateBanked(payment.getBankedDate());
                bulkScanningReportDto.setBgcBatch(payment.getGiroSlipNo());
                bulkScanningReportDto.setPaymentAssetDCN(payment.getDocumentControlNumber());
                bulkScanningReportDto.setPaymentMethod(payment.getPaymentMethod().getName());
                bulkScanningReportDto.setAmount(payment.getAmount().toString());
                return bulkScanningReportDto;
            }).collect(Collectors.toList());

        bulkScanningReportDtoList.sort(Comparator.comparing(BulkScanningReportDto::getRespServiceId).thenComparing(BulkScanningReportDto::getAllocationStatus));
        LOG.info("Unallocated Report list size : {}",bulkScanningReportDtoList.size());
        return bulkScanningReportDtoList;
    }

    private boolean checkPaymentsFromExela(PaymentFeeLink paymentFeeLink){
        return paymentFeeLink.getPayments().stream()
            .anyMatch(payment ->
                payment.getPaymentProvider()
                    .getName()
                    .equalsIgnoreCase("exela")
            );
    }

    public List<BulkScanningUnderOverPaymentDto> toSurplusAndShortfallReportdto(List<Payment> payments, Date toDate) {
        List<BulkScanningUnderOverPaymentDto> underOverPaymentDtos = new ArrayList<>();
        LOG.info("SurplusAndShortfall payments size : {}",payments.size());

        payments = payments.stream()
            .filter(payment -> checkPaymentsFromExela(payment.getPaymentLink()))
            .collect(Collectors.toList());
        payments = payments.stream()
            .filter(payment -> checkGroupOutstanding(payment))
            .collect(Collectors.toList());
        payments = payments.stream()
            .filter(payment ->
                (Optional.ofNullable(payment).isPresent() && Optional.ofNullable(payment.getPaymentAllocation()).isPresent() && payment.getPaymentAllocation().size() > 0 &&
                    (! payment.getPaymentAllocation().get(0).getPaymentAllocationStatus().getName().equalsIgnoreCase("Transferred") &&
                        ! payment.getPaymentAllocation().get(0).getPaymentAllocationStatus().getName().equalsIgnoreCase("Unidentified"))))
            .collect(Collectors.toList());

        underOverPaymentDtos = payments.stream()
            .map(payment -> {
                BulkScanningUnderOverPaymentDto bulkScanningUnderOverPaymentDto = new BulkScanningUnderOverPaymentDto();

                BigDecimal feeAmount = calculateFeeAmount(payment.getPaymentLink().getFees());

                BigDecimal remissionAmount = calculateRemissionAmount(payment.getPaymentLink().getRemissions());

                BigDecimal paymentAmount = payment.getAmount();

                BigDecimal totalPaymentReceived = calculatePaymentAmount(payment.getPaymentLink().getPayments()
                    .stream()
                    .filter(payment1 ->
                        (payment1.getDateCreated().before(payment.getDateCreated()) ||
                            payment1.getDateCreated().equals(payment.getDateCreated())))
                    .collect(Collectors.toList()));

                BigDecimal totalOutStanding = feeAmount.subtract((totalPaymentReceived.add(remissionAmount)));

                if (totalOutStanding.compareTo(BigDecimal.ZERO) != 0)
                {
                    bulkScanningUnderOverPaymentDto.setRespServiceId(payment.getSiteId());
                    bulkScanningUnderOverPaymentDto.setRespServiceName(payment.getServiceType());
                    bulkScanningUnderOverPaymentDto.setCcdCaseReference(payment.getCcdCaseNumber());
                    bulkScanningUnderOverPaymentDto.setBalance(totalOutStanding);
                    bulkScanningUnderOverPaymentDto.setPaymentAmount(paymentAmount);
                    bulkScanningUnderOverPaymentDto.setSurplusShortfall(totalOutStanding.compareTo(BigDecimal.ZERO) > 0 ? "Shortfall" : "Surplus");
                    bulkScanningUnderOverPaymentDto.setProcessedDate(payment.getDateCreated());
                    bulkScanningUnderOverPaymentDto.setReason(payment.getPaymentAllocation().get(0).getReason());
                    bulkScanningUnderOverPaymentDto.setExplanation(payment.getPaymentAllocation().get(0).getExplanation());
                    bulkScanningUnderOverPaymentDto.setUserName(payment.getPaymentAllocation().get(0).getUserName());
                    bulkScanningUnderOverPaymentDto.setCcdExceptionReference(payment.getCaseReference());
                }
                return bulkScanningUnderOverPaymentDto;
            })
            .collect(Collectors.toList());
        underOverPaymentDtos.sort(Comparator.comparing(BulkScanningUnderOverPaymentDto::getRespServiceId).thenComparing(BulkScanningUnderOverPaymentDto::getSurplusShortfall));
        LOG.info("Surplus and Shortfall Report list size : {}",underOverPaymentDtos.size());
        return underOverPaymentDtos;
    }

    private boolean checkGroupOutstanding(Payment payment) {
        BigDecimal feeAmount = calculateFeeAmount(payment.getPaymentLink().getFees());

        BigDecimal remissionAmount = calculateRemissionAmount(payment.getPaymentLink().getRemissions());

        BigDecimal totalPaymentReceived = calculatePaymentAmount(payment.getPaymentLink().getPayments());

        BigDecimal totalOutStanding = feeAmount.subtract((totalPaymentReceived.add(remissionAmount)));

        return totalOutStanding.compareTo(BigDecimal.ZERO) != 0;
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
