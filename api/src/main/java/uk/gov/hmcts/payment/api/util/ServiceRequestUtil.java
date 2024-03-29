package uk.gov.hmcts.payment.api.util;

import uk.gov.hmcts.payment.api.contract.DisputeDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceRequestUtil {

    private static final String DISPUTED = "Disputed";

    public String getServiceRequestStatus(PaymentGroupDto paymentGroupDto){
        //Calculate the fee total in a payment group
        BigDecimal orderFeeTotal = getTotalFeeAmount(paymentGroupDto);

        //Calculate remission total in a payment group
        BigDecimal orderRemissionTotal = getTotalRemissionAmount(paymentGroupDto);

        //Calculate the pending amount for a payment group
        BigDecimal orderPaymentTotal = getTotalPaymentAmount(paymentGroupDto);

        //Calculate the pending amount for a payment group
        BigDecimal orderPendingTotal = (orderFeeTotal.subtract(orderRemissionTotal)).subtract(orderPaymentTotal);

        if (paymentGroupDto.isAnyPaymentDisputed()) {
                return DISPUTED;
        } else if(orderPendingTotal.compareTo(BigDecimal.ZERO) <= 0 && orderPaymentTotal.compareTo(BigDecimal.ZERO) >= 0) {
                return "Paid";
        } else if(orderFeeTotal.compareTo(BigDecimal.ZERO) > 0 && (orderPaymentTotal.compareTo(BigDecimal.ZERO) > 0
            || orderRemissionTotal.compareTo(BigDecimal.ZERO) > 0) && orderPendingTotal.compareTo(BigDecimal.ZERO) > 0){
                return "Partially paid";
            }
        else{
                return "Not paid";
        }
    }

    private BigDecimal getTotalFeeAmount(PaymentGroupDto paymentGroupDto){
        BigDecimal orderFeeTotal= BigDecimal.ZERO;
        //Calculate the fee total in a payment group
        if(paymentGroupDto.getFees() != null) {
            for (int i = 0; i < paymentGroupDto.getFees().size(); i++) {
                if (paymentGroupDto.getFees().get(i).getCalculatedAmount() != null) {
                    orderFeeTotal = orderFeeTotal.add(paymentGroupDto.getFees().get(i).getCalculatedAmount());
                }
            }
        }
        return orderFeeTotal;

    }

    private BigDecimal getTotalRemissionAmount(PaymentGroupDto paymentGroupDto){
        BigDecimal orderRemissionTotal= BigDecimal.ZERO;
        //Calculate remission total in a payment group
        if(paymentGroupDto.getRemissions() != null){
            for (int i = 0; i < paymentGroupDto.getRemissions().size(); i++) {
                if (paymentGroupDto.getRemissions().get(i).getHwfAmount() != null) {
                    orderRemissionTotal = orderRemissionTotal.add(paymentGroupDto.getRemissions().get(i).getHwfAmount());
                }
            }
        }
        return orderRemissionTotal;

    }

    private BigDecimal getTotalPaymentAmount(PaymentGroupDto paymentGroupDto){
        BigDecimal orderPaymentTotal= BigDecimal.ZERO;
        //Calculate the payment total in a payment group
        if(paymentGroupDto.getPayments() != null) {
            for (int i = 0; i < paymentGroupDto.getPayments().size(); i++) {
                if (paymentGroupDto.getPayments().get(i).getStatus().equals("Success") && paymentGroupDto.getPayments().get(i).getAmount() != null) {
                 List<DisputeDto> disputeDTO =  paymentGroupDto.getPayments().get(i).getDisputes().stream().filter(DisputeDto::isDispute).collect(Collectors.toList());
               BigDecimal totalDispute = BigDecimal.ZERO;
                 for(DisputeDto disputeDto1 :disputeDTO) {

                     totalDispute = totalDispute.add(disputeDto1.getAmount());
                 }

                    orderPaymentTotal = orderPaymentTotal.add(paymentGroupDto.getPayments().get(i).getAmount().subtract(totalDispute));
                }
            }
        }
        return orderPaymentTotal;

    }
}
