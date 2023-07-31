package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.LiberataServiceInaccessibleException;
import uk.gov.hmcts.payment.api.service.AccountService;

@RestController
@Tag(name = "AccountController", description = "Account REST API")
public class AccountController {



    private static final Logger LOG = LoggerFactory.getLogger(AccountController.class);

    private final AccountService<AccountDto, String> accountService;

    @Autowired
    public AccountController(
        AccountService<AccountDto, String> accountService) {
        this.accountService = accountService;
    }


    @Operation(summary = "Get the account status and available balance for a PBA account number", description = "Get the account status and available balance for a PBA account number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account details retrieved"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "503", description = "Failed to connect with Liberata")
    })
    @GetMapping(value = "/accounts/{accountNumber}")
    public AccountDto getAccounts(@PathVariable("accountNumber") String accountNumber) {
        try {
            return accountService.retrieve(accountNumber);
        } catch (HttpClientErrorException ex) {
            LOG.error("Error while  calling account", ex);
            throw new AccountNotFoundException("Account not found");
        } catch (Exception ex) {
            throw new LiberataServiceInaccessibleException("Failed to connect with Liberata. " + ex.getMessage());
        }


    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(AccountNotFoundException.class)
    public String return404(AccountNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(LiberataServiceInaccessibleException.class)
    public String return503(LiberataServiceInaccessibleException ex) {
        return ex.getMessage();
    }
}
