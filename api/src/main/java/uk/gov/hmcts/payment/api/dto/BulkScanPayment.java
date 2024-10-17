package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder(builderMethodName = "createPaymentRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(NON_NULL)
public class BulkScanPayment {

    @NotBlank(message = "document_control_number can't be Blank")
    @JsonProperty("document_control_number")
    @Pattern(regexp="-?\\d+(\\.\\d+)?", message = "document_control_number should be numeric")
    @Size(min = 21, max = 21, message = "document_control_number length must be 21 digits")
    private String dcnReference;
    /*
    Payment amount in GBP
     */
    @NotNull(message = "amount can't be Blank")
    @DecimalMin(value = "0.01", message = "amount must be greater than or equal to 0.01")
    @Digits(integer = 10, fraction = 2, message = "amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    /*
    The ISO currency code
     */
    @NotBlank(message = "currency can't be Blank")
    private String currency;

    /*
    The method of payment i.e. Cheque, Postal Order, CASH
     */
    @NotBlank(message = "method can't be Blank")
    private String method;

    /*
    Number of the credit slip containing the payment
     */
    @NotNull(message = "bank_giro_credit_slip_number can't be Blank")
    @JsonProperty("bank_giro_credit_slip_number")
    @Min(value = 0, message = "bank_giro_credit_slip_number must be Positive")
    @Digits(integer = 6, fraction = 0,
        message = "bank_giro_credit_slip_number length must not be greater than 6 digits")
    private Integer bankGiroCreditSlipNumber;

    /*
    Date the payment was sent for banking.
     */
    @NotBlank(message = "banked_date can't be Blank")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonProperty("banked_date")
    private String bankedDate;

    @JsonIgnore
    @AssertFalse(message = "Invalid banked_Date. Date format should be YYYY-MM-DD (e.g. 2019-01-01). should never be a future date")
    public boolean isValidBankedDateFormat() {
        if (bankedDate != null) {
            if(! bankedDate.matches("\\d{4}-\\d{2}-\\d{2}")){
                return true;
            }
            SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
            sdfrmt.setLenient(false);
            try {
                Date givenDate = sdfrmt.parse(bankedDate);
                Date currDate = new Date(System.currentTimeMillis());
                if (givenDate.after(currDate)) {
                    return true;
                }
            } catch (ParseException e) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    @AssertFalse(message = "Invalid method. Accepted value Cash/Cheque/PostalOrder")
    public boolean isValidPaymentMethod() {
        String[] validMethods = {"Cash", "Cheque", "PostalOrder"};
        return method != null && ! Arrays.asList(validMethods).stream().anyMatch(vm -> vm.equalsIgnoreCase(method));
    }

    @JsonIgnore
    @AssertFalse(message = "Invalid currency. Accepted value GBP")
    public boolean isValidCurrency() {
        String[] validCurrencys = {"GBP"};
        return currency != null && ! Arrays.asList(validCurrencys).stream().anyMatch(vm -> vm.equalsIgnoreCase(currency));
    }


}
