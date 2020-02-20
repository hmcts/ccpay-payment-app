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
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BulkScanningReportMapper {

    private static final Logger LOG = LoggerFactory.getLogger(BulkScanningReportMapper.class);

    public List<BulkScanningReportDto> toBulkScanningUnallocatedReportDto(List<Payment> payments) {
        LOG.info("Payments size inside toBulkScanningUnallocatedReportDto: {}",payments.size());
        List<BulkScanningReportDto> bulkScanningReportDtos = new ArrayList<>();

        payments = Optional.ofNullable(payments)
            .orElseGet(Collections :: emptyList)
            .stream()
            .filter(payment -> Objects.nonNull(payment.getPaymentChannel()))
            .filter(payment -> Objects.nonNull(payment.getPaymentChannel().getName()))
            .filter(payment -> payment.getPaymentChannel().getName().equalsIgnoreCase("bulk scan"))
            .collect(Collectors.toList());
        LOG.info("Payments size after filtering exela payments: {}",payments.size());
        bulkScanningReportDtos = payments.stream()
            .filter(payment -> Objects.nonNull(payment.getPaymentAllocation()) && payment.getPaymentAllocation().size() > 0)
            .filter(payment -> Objects.nonNull(payment.getPaymentAllocation().get(0).getPaymentAllocationStatus()))
            .filter(payment -> Objects.nonNull(payment.getPaymentAllocation().get(0).getPaymentAllocationStatus().getName()))
            .filter(payment ->
                (payment.getPaymentAllocation().get(0).getPaymentAllocationStatus().getName().equalsIgnoreCase("Transferred") ||
                    payment.getPaymentAllocation().get(0).getPaymentAllocationStatus().getName().equalsIgnoreCase("Unidentified")))
            .map(populateTransferredOrUnidentifiedReport()).collect(Collectors.toList());

        bulkScanningReportDtos.sort(Comparator.comparing(BulkScanningReportDto::getRespServiceId).thenComparing(BulkScanningReportDto::getAllocationStatus));
        LOG.info("Final Unallocated Report list size : {}",bulkScanningReportDtos.size());
        return bulkScanningReportDtos;
    }

    private Function<Payment, BulkScanningReportDto> populateTransferredOrUnidentifiedReport() {
        return payment -> {
            LOG.info("Transferred or Unidentified");
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
        };
    }

    public List<BulkScanningUnderOverPaymentDto> toSurplusAndShortfallReportdto(List<Payment> payments) {
        List<BulkScanningUnderOverPaymentDto> underOverPaymentDtos = new ArrayList<>();
        LOG.info("SurplusAndShortfall payments size : {}",payments.size());

        payments = Optional.ofNullable(payments)
            .orElseGet(Collections :: emptyList)
            .stream()
            .filter(payment -> Objects.nonNull(payment.getPaymentChannel()))
            .filter(payment -> Objects.nonNull(payment.getPaymentChannel().getName()))
            .filter(payment -> payment.getPaymentChannel().getName().equalsIgnoreCase("bulk scan"))
            .collect(Collectors.toList());
        LOG.info("Payments size after checkPaymentsFromExela: {}",payments.size());

        payments = payments.stream()
            .filter(payment -> checkGroupOutstanding(payment))
            .collect(Collectors.toList());
        LOG.info("Payments size after checkGroupOutstanding: {}",payments.size());
        /*Filter for ignoring telephony, online payments and bulk scan payments which are Unidentified or Transferred. As we cannot
        have surplus and shortfall payments in online and telephony payments.
         */
        payments = payments.stream()
            .filter(payment -> Objects.nonNull(payment))
            .filter(payment -> Objects.nonNull(payment.getPaymentAllocation()) && payment.getPaymentAllocation().size() > 0)
            .filter(payment -> Objects.nonNull(payment.getPaymentAllocation().get(0).getPaymentAllocationStatus()))
            .filter(payment -> Objects.nonNull(payment.getPaymentAllocation().get(0).getPaymentAllocationStatus().getName()))
            .filter(payment ->
                (payment.getPaymentAllocation().get(0).getPaymentAllocationStatus().getName().equalsIgnoreCase("Allocated")))
            .collect(Collectors.toList());
        LOG.info("Payments size after filtering Allocated Payments: {}",payments.size());
        underOverPaymentDtos = payments.stream()
            .filter(payment -> getGroupOutstandingForDateRange(payment).compareTo(BigDecimal.ZERO) != 0)
            .map(populateUnderOverPaymentReport())
            .collect(Collectors.toList());
        underOverPaymentDtos.sort(Comparator.comparing(BulkScanningUnderOverPaymentDto::getRespServiceId).thenComparing(BulkScanningUnderOverPaymentDto::getSurplusShortfall));
        LOG.info("Surplus and Shortfall Report list final size : {}",underOverPaymentDtos.size());
        return underOverPaymentDtos;
    }

    private Function<Payment, BulkScanningUnderOverPaymentDto> populateUnderOverPaymentReport() {
        return payment -> {
            BulkScanningUnderOverPaymentDto bulkScanningUnderOverPaymentDto = new BulkScanningUnderOverPaymentDto();

            LOG.info("Total Outstanding is not Zero");
            bulkScanningUnderOverPaymentDto.setRespServiceId(payment.getSiteId());
            bulkScanningUnderOverPaymentDto.setRespServiceName(payment.getServiceType());
            bulkScanningUnderOverPaymentDto.setCcdCaseReference(payment.getCcdCaseNumber());
            bulkScanningUnderOverPaymentDto.setBalance(getGroupOutstandingForDateRange(payment));
            bulkScanningUnderOverPaymentDto.setPaymentAmount(payment.getAmount());
            bulkScanningUnderOverPaymentDto.setSurplusShortfall(getGroupOutstandingForDateRange(payment).compareTo(BigDecimal.ZERO) > 0 ? "Shortfall" : "Surplus");
            bulkScanningUnderOverPaymentDto.setProcessedDate(payment.getDateCreated());
            bulkScanningUnderOverPaymentDto.setReason(payment.getPaymentAllocation().get(0).getReason());
            bulkScanningUnderOverPaymentDto.setExplanation(payment.getPaymentAllocation().get(0).getExplanation());
            bulkScanningUnderOverPaymentDto.setUserName(payment.getPaymentAllocation().get(0).getUserName());
            bulkScanningUnderOverPaymentDto.setCcdExceptionReference(payment.getCaseReference());
            return bulkScanningUnderOverPaymentDto;
        };
    }

    /*This method is to check if the particular payment group has any surplus/shortfall. If the total outstanding is 0 then those payments will not be
    considered for this report.*/
    private boolean checkGroupOutstanding(Payment payment) {
        BigDecimal feeAmount = calculateFeeAmount(payment.getPaymentLink().getFees());

        BigDecimal remissionAmount = calculateRemissionAmount(payment.getPaymentLink().getRemissions());

        BigDecimal totalPaymentReceived = calculatePaymentAmount(payment.getPaymentLink().getPayments());

        BigDecimal totalOutStanding = feeAmount.subtract((totalPaymentReceived.add(remissionAmount)));

        return totalOutStanding.compareTo(BigDecimal.ZERO) != 0;
    }

    /*Method to check the group outstanding at payment level. For example if you have 1000£ fee and two payments 200£ each
     1. For 1st payment total outstanding will be 800
     2. For 2nd payment total outstanding will be 600
     */

    private BigDecimal getGroupOutstandingForDateRange(Payment payment){
        BigDecimal feeAmount = calculateFeeAmount(payment.getPaymentLink().getFees());

        BigDecimal remissionAmount = calculateRemissionAmount(payment.getPaymentLink().getRemissions());

        BigDecimal totalPaymentReceived = calculatePaymentAmount(payment.getPaymentLink().getPayments()
            .stream()
            .filter(payment1 ->
                (payment1.getDateCreated().before(payment.getDateCreated()) ||
                    payment1.getDateCreated().equals(payment.getDateCreated())))
            .collect(Collectors.toList()));
        LOG.info("TotalPaymentReceived: {}",totalPaymentReceived);
        BigDecimal totalOutStanding = feeAmount.subtract((totalPaymentReceived.add(remissionAmount)));

        LOG.info("Total Outstanding: {}",totalOutStanding);
        return totalOutStanding;
    }

    //Method to calculate the total payment amount for a particular payment group. And we are considering only payments which are success.
    private BigDecimal calculatePaymentAmount(List<Payment> payments) {

        BigDecimal paymentAmount = new BigDecimal(0);
        payments = payments.stream()
            .filter(payment -> Objects.nonNull(payment.getPaymentStatus()))
            .filter(payment -> Objects.nonNull(payment.getPaymentStatus().getName()))
            .filter(payment -> payment.getPaymentStatus().getName().equalsIgnoreCase("success"))
            .collect(Collectors.toList());
        LOG.info("Payments size after filtering success payments: {}",payments.size());
        for(Payment payment : payments)
        {
            BigDecimal calculatedAmount = payment.getAmount();
            paymentAmount= paymentAmount.add(calculatedAmount);

        }
        return paymentAmount;
    }

    //Method to calculate the total remmission amount for a particular payment group.
    private BigDecimal calculateRemissionAmount(List<Remission> remissions) {

        BigDecimal remissionAmount = new BigDecimal(0);
        for(Remission remission : remissions)
        {
            BigDecimal calculatedAmount = remission.getHwfAmount() ;
            remissionAmount= remissionAmount.add(calculatedAmount);
        }
        return remissionAmount;
    }

    //Method to calculate the total fee amount for a particular payment group.
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
