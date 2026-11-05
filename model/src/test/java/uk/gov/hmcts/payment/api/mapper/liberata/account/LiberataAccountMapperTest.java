package uk.gov.hmcts.payment.api.mapper.liberata.account;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.liberata.account.LiberataAccountData;
import uk.gov.hmcts.payment.api.dto.liberata.account.LiberataAccountResponse;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.LiberataAccountException;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LiberataAccountMapperTest {

    private static final String PBA_REFERENCE = "PBA1234567";
    private static final String TRADING_NAME = "Test Account";
    private static final BigDecimal CREDIT_LIMIT = new BigDecimal("1000.00");
    private static final BigDecimal AVAILABLE_BALANCE = new BigDecimal("750.50");
    private static final String STATUS = "Active";
    private static final String REASON = null;
    private static final Date EFFECTIVE_DATE = new Date(1735732800000L);

    private final LiberataAccountMapper mapper = new LiberataAccountMapper();

    @Test
    void toAccountDtoMapsLiberataAccountResponse() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse(
            "200",
            validAccountData()
        );

        AccountDto accountDto = mapper.toAccountDto(liberataAccountResponse);

        assertEquals(PBA_REFERENCE, accountDto.getAccountNumber());
        assertEquals(TRADING_NAME, accountDto.getAccountName());
        assertEquals(CREDIT_LIMIT, accountDto.getCreditLimit());
        assertEquals(AVAILABLE_BALANCE, accountDto.getAvailableBalance());
        assertEquals(AccountStatus.ACTIVE, accountDto.getStatus());
        assertEquals(EFFECTIVE_DATE, accountDto.getEffectiveDate());
    }

    @Test
    void toAccountDtoThrowsLiberataAccountExceptionWhenResponseIsNull() {
        assertThrows(LiberataAccountException.class, () -> mapper.toAccountDto(null));
    }

    @Test
    void toAccountDtoThrowsLiberataAccountExceptionWhenDataIsNull() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200", null);
        assertThrows(LiberataAccountException.class, () -> mapper.toAccountDto(liberataAccountResponse));
    }

    @Test
    void toAccountDtoThrowsLiberataAccountExceptionWhenPbaReferenceIsNull() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200",
            new LiberataAccountData(null, TRADING_NAME, CREDIT_LIMIT, AVAILABLE_BALANCE, STATUS, REASON, EFFECTIVE_DATE));
        assertThrows(LiberataAccountException.class, () -> mapper.toAccountDto(liberataAccountResponse));
    }

    @Test
    void toAccountDtoThrowsLiberataAccountExceptionWhenTradingNameIsNull() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200",
            new LiberataAccountData(PBA_REFERENCE, null, CREDIT_LIMIT, AVAILABLE_BALANCE, STATUS, REASON, EFFECTIVE_DATE));
        assertThrows(LiberataAccountException.class, () -> mapper.toAccountDto(liberataAccountResponse));
    }

    @Test
    void toAccountDtoThrowsLiberataAccountExceptionWhenCreditLimitIsNull() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200",
            new LiberataAccountData(PBA_REFERENCE, TRADING_NAME, null, AVAILABLE_BALANCE, STATUS, REASON, EFFECTIVE_DATE));
        assertThrows(LiberataAccountException.class, () -> mapper.toAccountDto(liberataAccountResponse));
    }

    @Test
    void toAccountDtoThrowsLiberataAccountExceptionWhenAvailableBalanceIsNull() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200",
            new LiberataAccountData(PBA_REFERENCE, TRADING_NAME, CREDIT_LIMIT, null, STATUS, REASON, EFFECTIVE_DATE));
        assertThrows(LiberataAccountException.class, () -> mapper.toAccountDto(liberataAccountResponse));
    }

    @Test
    void toAccountDtoThrowsLiberataAccountExceptionWhenStatusIsNull() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200",
            new LiberataAccountData(PBA_REFERENCE, TRADING_NAME, CREDIT_LIMIT, AVAILABLE_BALANCE, null, REASON, EFFECTIVE_DATE));
        assertThrows(LiberataAccountException.class, () -> mapper.toAccountDto(liberataAccountResponse));
    }

    @Test
    void toAccountDtoThrowsLiberataAccountExceptionWhenEffectiveDateIsNull() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200",
            new LiberataAccountData(PBA_REFERENCE, TRADING_NAME, CREDIT_LIMIT, AVAILABLE_BALANCE, STATUS, REASON, null));
        assertThrows(LiberataAccountException.class, () -> mapper.toAccountDto(liberataAccountResponse));
    }

    private static LiberataAccountData validAccountData() {
        return new LiberataAccountData(
            PBA_REFERENCE,
            TRADING_NAME,
            CREDIT_LIMIT,
            AVAILABLE_BALANCE,
            STATUS,
            REASON,
            EFFECTIVE_DATE
        );
    }

    @Test
    void transformStatusMapsInactiveWithDeletedReasonToDeleted() {
        LiberataAccountResponse response = new LiberataAccountResponse(
            "200",
            new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                "Inactive",
                "DELETED",
                EFFECTIVE_DATE
            )
        );

        AccountDto accountDto = mapper.toAccountDto(response);
        assertEquals(AccountStatus.DELETED, accountDto.getStatus());
    }

    @Test
    void transformStatusMapsInactiveWithDeletedReasonCaseInsensitive() {
        LiberataAccountResponse response = new LiberataAccountResponse(
            "200",
            new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                "inactive",
                "deleted",
                EFFECTIVE_DATE
            )
        );

        AccountDto accountDto = mapper.toAccountDto(response);
        assertEquals(AccountStatus.DELETED, accountDto.getStatus());
    }

    @Test
    void transformStatusMapsInactiveWithOnHoldReasonToOnHold() {
        LiberataAccountResponse response = new LiberataAccountResponse(
            "200",
            new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                "Inactive",
                "CUSTOMER ON HOLD - EXCEEDED CREDIT LIMIT",
                EFFECTIVE_DATE
            )
        );

        AccountDto accountDto = mapper.toAccountDto(response);
        assertEquals(AccountStatus.ON_HOLD, accountDto.getStatus());
    }

    @Test
    void transformStatusMapsInactiveWithOnHoldReasonCaseInsensitive() {
        LiberataAccountResponse response = new LiberataAccountResponse(
            "200",
            new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                "inactive",
                "customer on hold - mandate cancelled",
                EFFECTIVE_DATE
            )
        );

        AccountDto accountDto = mapper.toAccountDto(response);
        assertEquals(AccountStatus.ON_HOLD, accountDto.getStatus());
    }

    @Test
    void transformStatusMapsInactiveWithoutOnHoldReasonToInactive() {
        LiberataAccountResponse response = new LiberataAccountResponse(
            "200",
            new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                "Inactive",
                "Some other reason",
                EFFECTIVE_DATE
            )
        );

        AccountDto accountDto = mapper.toAccountDto(response);
        assertEquals(AccountStatus.INACTIVE, accountDto.getStatus());
    }

    @Test
    void transformStatusMapsActiveToActive() {
        LiberataAccountResponse response = new LiberataAccountResponse(
            "200",
            new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                "Active",
                null,
                EFFECTIVE_DATE
            )
        );

        AccountDto accountDto = mapper.toAccountDto(response);
        assertEquals(AccountStatus.ACTIVE, accountDto.getStatus());
    }

    @Test
    void transformStatusMapsActiveCaseInsensitive() {
        LiberataAccountResponse response = new LiberataAccountResponse(
            "200",
            new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                "active",
                null,
                EFFECTIVE_DATE
            )
        );

        AccountDto accountDto = mapper.toAccountDto(response);
        assertEquals(AccountStatus.ACTIVE, accountDto.getStatus());
    }

    @Test
    void transformStatusMapsUnknownStatusToInactive() {
        LiberataAccountResponse response = new LiberataAccountResponse(
            "200",
            new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                "Unknown",
                null,
                EFFECTIVE_DATE
            )
        );

        AccountDto accountDto = mapper.toAccountDto(response);
        assertEquals(AccountStatus.INACTIVE, accountDto.getStatus());
    }
}
