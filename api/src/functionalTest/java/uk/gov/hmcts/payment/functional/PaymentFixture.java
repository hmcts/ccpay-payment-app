package uk.gov.hmcts.payment.functional;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.math.BigDecimal;
import java.util.Arrays;

public class PaymentFixture {

    public static CardPaymentRequest aCardPaymentRequest(String amountString) {
        return CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .caseReference("aCaseReference")
            .service(Service.CMC)
            .currency(CurrencyCode.GBP)
            .siteId("AA101")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                .calculatedAmount(BigDecimal.TEN)
                .code("FEE001")
                .version("1")
                .build())
            )
            .build();
    }

    public static CreditAccountPaymentRequest aPbaPaymentRequest(String amountString) {
        return CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .description("New passport application")
            .caseReference("aCaseReference")
            .service(Service.CMC)
            .currency(CurrencyCode.GBP)
            .siteId("AA101")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("AC101010")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(BigDecimal.TEN)
                    .code("FEE001")
                    .version("1")
                    .build())
            )
            .build();
    }

    public static PaymentRecordRequest aBarPaymentRequest(String amountString) {
        return  PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal(amountString))
            .paymentMethod(PaymentMethodType.CASH)
            .reference("case_ref_123")
            .externalProvider("middle office provider")
            .service(Service.DIGITAL_BAR)
            .currency(CurrencyCode.GBP)
            .giroSlipNo("12345")
            .reportedDateOffline(DateTime.now().toString("yyyy-MM-dd"))
            .siteId("AA99")
            .fees(Lists.newArrayList(
                FeeDto.feeDtoWith()
                    .calculatedAmount(BigDecimal.TEN)
                    .code("FEE001")
                    .version("1")
                    .build())
            )
            .build();
    }
}
