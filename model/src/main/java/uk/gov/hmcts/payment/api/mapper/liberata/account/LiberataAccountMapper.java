package uk.gov.hmcts.payment.api.mapper.liberata.account;

import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.liberata.account.LiberataAccountResponse;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.LiberataAccountException;

import java.util.Objects;

@Component
public class LiberataAccountMapper {
    public AccountDto toAccountDto(LiberataAccountResponse liberataAccountResponse) {
        if (
            Objects.nonNull(liberataAccountResponse) &&
                Objects.nonNull(liberataAccountResponse.getData()) &&
                Objects.nonNull(liberataAccountResponse.getData().getPbaReference()) &&
                Objects.nonNull(liberataAccountResponse.getData().getTradingName()) &&
                Objects.nonNull(liberataAccountResponse.getData().getCreditLimit()) &&
                Objects.nonNull(liberataAccountResponse.getData().getAvailableBalance()) &&
                Objects.nonNull(liberataAccountResponse.getData().getStatus()) &&
                Objects.nonNull(liberataAccountResponse.getData().getEffectiveDate())
        ) {

            val pbaReference = liberataAccountResponse.getData().getPbaReference();
            val tradingName = liberataAccountResponse.getData().getTradingName();
            val creditLimit = liberataAccountResponse.getData().getCreditLimit();
            val availableBalance = liberataAccountResponse.getData().getAvailableBalance();
            val liberataStatus = liberataAccountResponse.getData().getStatus();
            val effectiveDate = liberataAccountResponse.getData().getEffectiveDate();

            AccountStatus accountStatus = transformStatus(liberataStatus, liberataAccountResponse.getData().getReason());

            return new AccountDto(pbaReference, tradingName, creditLimit, availableBalance, accountStatus, effectiveDate);
        } else {
            throw new LiberataAccountException("Failed to parse LiberataIdentityResponse: " + liberataAccountResponse);
        }
    }

    private AccountStatus transformStatus(String liberataStatus, String reason) {
        String statusLower = liberataStatus.toLowerCase();
        String reasonLower = reason != null ? reason.toLowerCase() : "";

        if (statusLower.equals("inactive") && reasonLower.equals("deleted")) {
            return AccountStatus.DELETED;
        }

        if (statusLower.equals("inactive") && reasonLower.contains("on hold")) {
            return AccountStatus.ON_HOLD;
        }

        if (statusLower.equals("active")) {
            return AccountStatus.ACTIVE;
        }

        return AccountStatus.INACTIVE;
    }
}

