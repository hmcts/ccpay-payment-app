package uk.gov.hmcts.payment.api.util;

import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;

import java.math.BigDecimal;

public class ServiceRequestUtil {

    public String getServiceRequestStatus(PaymentGroupDto paymentGroupDto){
        BigDecimal orderFeeTotal = BigDecimal.ZERO;
        BigDecimal orderRemissionTotal = BigDecimal.ZERO;
        BigDecimal orderPaymentTotal = BigDecimal.ZERO;

        //Calculate the fee total in a payment group
        if(paymentGroupDto.getFees() != null) {
            if (!paymentGroupDto.getFees().isEmpty()) {
            for (int i = 0; i < paymentGroupDto.getFees().size(); i++) {
                if (paymentGroupDto.getFees().get(i).getFeeAmount() != null) {
                    orderFeeTotal = orderFeeTotal.add(paymentGroupDto.getFees().get(i).getFeeAmount());
                    }
                }
            }
        }

        //Calculate remission total in a payment group
        if(paymentGroupDto.getRemissions() != null){
            if (!paymentGroupDto.getRemissions().isEmpty()) {
                for (int i = 0; i < paymentGroupDto.getRemissions().size(); i++) {
                    if (paymentGroupDto.getRemissions().get(i).getHwfAmount() != null) {
                        orderRemissionTotal = orderRemissionTotal.add(paymentGroupDto.getRemissions().get(i).getHwfAmount());
                    }
                }
            }
        }

        //Calculate the payment total in a payment group
        if(paymentGroupDto.getPayments() != null) {
            if (!paymentGroupDto.getPayments().isEmpty()) {
                for (int i = 0; i < paymentGroupDto.getPayments().size(); i++) {
                    if (paymentGroupDto.getPayments().get(i).getStatus().equals("Success") && paymentGroupDto.getPayments().get(i).getAmount() != null) {
                        orderPaymentTotal = orderPaymentTotal.add(paymentGroupDto.getPayments().get(i).getAmount());
                    }
                }
            }
        }

        //Calculate the pending amount for a payment group
        BigDecimal orderPendingTotal = (orderFeeTotal.subtract(orderRemissionTotal)).subtract(orderPaymentTotal);

        if(orderPendingTotal.compareTo(BigDecimal.ZERO) <= 0 && orderPendingTotal.compareTo(BigDecimal.ZERO) > 0){
            return "Paid";
        }
        else if(orderFeeTotal.compareTo(BigDecimal.ZERO) > 0 && (orderPaymentTotal.compareTo(BigDecimal.ZERO) > 0
            || orderRemissionTotal.compareTo(BigDecimal.ZERO) > 0) && orderPendingTotal.compareTo(BigDecimal.ZERO) > 0){
            return "Partially paid";
        }
        else{
            return "Not paid";
        }
    }
}
