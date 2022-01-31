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

    @Override
    public AccountDto retrieve(String pbaCode) {
        LOG.info("Called mock liberata account service");
        if ("PBAFUNC12345".equalsIgnoreCase(pbaCode) || "PBAFUNC345".equalsIgnoreCase(pbaCode)) {
            return AccountDto.accountDtoWith()
                .accountNumber("PBAFUNC12345")
                .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
                .creditLimit(BigDecimal.valueOf(28879))
                .availableBalance(BigDecimal.valueOf(30000))
                .status(AccountStatus.ACTIVE)
                .build();
        } else if ("PBAFUNC12350".equalsIgnoreCase(pbaCode) || "PBAFUNC350".equalsIgnoreCase(pbaCode)) {
            return AccountDto.accountDtoWith()
                .accountNumber("PBAFUNC12350")
                .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
                .creditLimit(BigDecimal.valueOf(28879))
                .availableBalance(BigDecimal.valueOf(30000))
                .status(AccountStatus.DELETED)
                .build();
        } else if ("PBAFUNC12355".equalsIgnoreCase(pbaCode) || "PBAFUNC355".equalsIgnoreCase(pbaCode)) {
            return AccountDto.accountDtoWith()
                .accountNumber("PBAFUNC12355")
                .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
                .creditLimit(BigDecimal.valueOf(28879))
                .availableBalance(BigDecimal.valueOf(30000))
                .status(AccountStatus.ON_HOLD)
                .build();
        } else if ("PBAFUNC360".equalsIgnoreCase(pbaCode)) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                "An Error for the purposes of testing an Error in Series 500...");
        }
        throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "UnKnown test pba account number");
    }
}
