package uk.gov.hmcts.payment.api.mapper.liberata.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.liberata.account.LiberataAccountData;
import uk.gov.hmcts.payment.api.dto.liberata.account.LiberataAccountResponse;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.LiberataAccountException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LiberataAccountMapperTest {

    private static final String PBA_REFERENCE = "PBA1234567";
    private static final String TRADING_NAME = "Test Account";
    private static final BigDecimal CREDIT_LIMIT = new BigDecimal("1000.00");
    private static final BigDecimal AVAILABLE_BALANCE = new BigDecimal("750.50");
    private static final AccountStatus STATUS = AccountStatus.ACTIVE;
    private static final Date EFFECTIVE_DATE = new Date(1735732800000L);

    private final LiberataAccountMapper mapper = new LiberataAccountMapper();

    @Test
    void toAccountDtoMapsLiberataAccountResponse() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse(
            "200",
            validAccountData()
        );

        AccountDto accountDto = mapper.toAccountDto(liberataAccountResponse);

        assertAll(
            () -> assertEquals(PBA_REFERENCE, accountDto.getAccountNumber()),
            () -> assertEquals(TRADING_NAME, accountDto.getAccountName()),
            () -> assertEquals(CREDIT_LIMIT, accountDto.getCreditLimit()),
            () -> assertEquals(AVAILABLE_BALANCE, accountDto.getAvailableBalance()),
            () -> assertEquals(STATUS, accountDto.getStatus()),
            () -> assertEquals(EFFECTIVE_DATE, accountDto.getEffectiveDate())
        );
    }

    @Test
    void toAccountDtoThrowsLiberataAccountExceptionWhenResponseIsNull() {
        assertThrows(LiberataAccountException.class, () -> mapper.toAccountDto(null));
    }

    @Test
    void toAccountDtoThrowsLiberataAccountExceptionWhenDataIsNull() {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200", null);

        assertThrows(
            LiberataAccountException.class,
            () -> mapper.toAccountDto(liberataAccountResponse)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAccountData")
    void toAccountDtoThrowsLiberataAccountExceptionWhenRequiredDataFieldIsNull(LiberataAccountData accountData) {
        LiberataAccountResponse liberataAccountResponse = new LiberataAccountResponse("200", accountData);

        assertThrows(
            LiberataAccountException.class,
            () -> mapper.toAccountDto(liberataAccountResponse)
        );
    }

    private static Stream<Arguments> invalidAccountData() {
        return Stream.of(
            Arguments.of(new LiberataAccountData(
                null,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                STATUS,
                EFFECTIVE_DATE
            )),
            Arguments.of(new LiberataAccountData(
                PBA_REFERENCE,
                null,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                STATUS,
                EFFECTIVE_DATE
            )),
            Arguments.of(new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                null,
                AVAILABLE_BALANCE,
                STATUS,
                EFFECTIVE_DATE
            )),
            Arguments.of(new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                null,
                STATUS,
                EFFECTIVE_DATE
            )),
            Arguments.of(new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                null,
                EFFECTIVE_DATE
            )),
            Arguments.of(new LiberataAccountData(
                PBA_REFERENCE,
                TRADING_NAME,
                CREDIT_LIMIT,
                AVAILABLE_BALANCE,
                STATUS,
                null
            ))
        );
    }

    private static LiberataAccountData validAccountData() {
        return new LiberataAccountData(
            PBA_REFERENCE,
            TRADING_NAME,
            CREDIT_LIMIT,
            AVAILABLE_BALANCE,
            STATUS,
            EFFECTIVE_DATE
        );
    }
}
