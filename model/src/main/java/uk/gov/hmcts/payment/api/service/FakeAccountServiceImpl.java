package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Date;

@Profile("mock-liberata")
@Primary
@Service
public class FakeAccountServiceImpl implements AccountService<AccountDto, String> {
    @Value("${payments.account.fake.account.number}")
    private String fakeAccountNumber;

    @Value("${payments.account.existing.account.number}")
    private String existingAccountNumber;

    @Override
    public AccountDto retrieve(String id) {
        if(id.equals(fakeAccountNumber)) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
        }

        if(id.equals((existingAccountNumber))){
            return AccountDto.accountDtoWith()
                .accountNumber(id)
                .accountName("fakeAccountName")
                .creditLimit(new BigDecimal("100"))
                .availableBalance(new BigDecimal("50"))
                .status(AccountStatus.ACTIVE)
                .effectiveDate(new Date())
                .build();
        }

        return AccountDto.accountDtoWith()
            .accountNumber("fakeAccountNumber")
            .accountName("fakeAccountName")
            .creditLimit(new BigDecimal("100"))
            .availableBalance(new BigDecimal("50"))
            .status(AccountStatus.ACTIVE)
            .effectiveDate(new Date())
            .build();
    }
}
