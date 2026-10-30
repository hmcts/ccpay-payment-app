package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;

@Service
@Profile("liberataMock")
public class MockAccountServiceImpl implements AccountService<AccountDto, String> {

    private static final Logger LOG = LoggerFactory.getLogger(MockAccountServiceImpl.class);
    private static final String ACCOUNT_NAME = "CAERPHILLY COUNTY BOROUGH COUNCIL";
    private static final BigDecimal CREDIT_LIMIT = BigDecimal.valueOf(28879);
    private static final BigDecimal AVAILABLE_BALANCE = BigDecimal.valueOf(30000);

    @Override
    public AccountDto retrieve(String pbaCode) {
        LOG.info("Called mock liberata account service");
        if (matches(pbaCode, "PBAFUNC12345", "PBAFUNC345", "PBAFUNC245", "PBA0082126")) {
            return buildAccountDto(pbaCode, AccountStatus.ACTIVE);
        } else if (matches(pbaCode, "PBAFUNC12350", "PBAFUNC350", "PBAFUNC250", "PBA0087741")) {
            return buildAccountDto(pbaCode, AccountStatus.DELETED);
        } else if (matches(pbaCode, "PBAFUNC12355", "PBAFUNC355", "PBAFUNC255", "PBA0078303")) {
            return buildAccountDto(pbaCode, AccountStatus.ON_HOLD);
        } else if (matches(pbaCode, "PBAFUNC360", "PBAFUNC260")) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                "An Error for the purposes of testing an Error in Series 500...");
        }
        throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "UnKnown test pba account number");
    }

    private AccountDto buildAccountDto(String pbaCode, AccountStatus status) {
        return AccountDto.accountDtoWith()
            .accountNumber(pbaCode)
            .accountName(ACCOUNT_NAME)
            .creditLimit(CREDIT_LIMIT)
            .availableBalance(AVAILABLE_BALANCE)
            .status(status)
            .build();
    }

    private boolean matches(String pbaCode, String... accountNumbers) {
        for (String accountNumber : accountNumbers) {
            if (accountNumber.equalsIgnoreCase(pbaCode)) {
                return true;
            }
        }
        return false;
    }
}
