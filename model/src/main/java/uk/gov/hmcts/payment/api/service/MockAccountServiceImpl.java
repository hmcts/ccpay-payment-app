package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;

@Service
@Profile("mock")
public class MockAccountServiceImpl implements AccountService<AccountDto, String> {

    private static final Logger LOG = LoggerFactory.getLogger(MockAccountServiceImpl.class);

    @Override
    public AccountDto retrieve(String pbaCode) {
        LOG.info("Called mock liberata account service");
        if ("PBA0082848".equalsIgnoreCase(pbaCode)) {
            return AccountDto.accountDtoWith()
                .accountNumber("PBA0082848")
                .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
                .creditLimit(BigDecimal.valueOf(28879))
                .availableBalance(BigDecimal.valueOf(30000))
                .status(AccountStatus.ACTIVE)
                .build();
        }
        throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "UnKnown test pba account number");
    }
}
