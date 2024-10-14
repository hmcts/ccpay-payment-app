package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
import uk.gov.hmcts.payment.api.util.AccountStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.HttpStatus.GONE;

@RestController
@Tag(name = "AccountController", description = "Account REST API")
public class AccountController {

    private static final String STATUS_DELETED = "Deleted";
    private static final String STATUS_ON_HOLD = "On-Hold";

    private static final String JSON_ERROR_MESSAGE_GONE = "The account has been deleted and is no longer available.";
    private static final String JSON_ERROR_MESSAGE_ON_HOLD = "The account is on hold and temporarily unavailable.";

    private final AccountService<AccountDto, String> accountService;

    @Autowired
    public AccountController(
        AccountService<AccountDto, String> accountService) {
        this.accountService = accountService;
    }


    @Operation(summary = "Get the account status and available balance for a PBA account number", description = "Get the account status and available balance for a PBA account number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account details retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorised"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "410", description = "PBA account was available but has now been deleted"),
        @ApiResponse(responseCode = "412", description = "PBA account has been put on hold"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error"),
        @ApiResponse(responseCode = "503", description = "Failed to connect with Liberata")
    })
    @GetMapping(value = "/accounts/{accountNumber}")
    public AccountDto getAccounts(@PathVariable("accountNumber") String accountNumber) {
        try {
            AccountDto accountDto =  accountService.retrieve(accountNumber);
            if(accountDto!=null && accountDto.getStatus()== AccountStatus.DELETED){
                throw new HttpClientErrorException(GONE);
            } else if (accountDto!=null && accountDto.getStatus()== AccountStatus.ON_HOLD) {
                throw new HttpClientErrorException(PRECONDITION_FAILED);
            }
            else{
                return accountDto;
            }

        } catch (HttpClientErrorException ex) {
            throw ex;
        } catch (Exception exception) {
            throw new LiberataServiceInaccessibleException("Failed to connect with Liberata. " + exception.getMessage());
        }
    }


    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Object> returnAccountStatusError(HttpClientErrorException ex) {

            Object responseBody;

        HttpStatusCode statusCode = ex.getStatusCode();
        if (statusCode.equals(GONE)) {
            responseBody = AccountStatusError.accountStatusErrorWith()
                .status(STATUS_DELETED)
                .errorMessage(JSON_ERROR_MESSAGE_GONE)
                .build();
        } else if (statusCode.equals(PRECONDITION_FAILED)) {
            responseBody = AccountStatusError.accountStatusErrorWith()
                .status(STATUS_ON_HOLD)
                .errorMessage(JSON_ERROR_MESSAGE_ON_HOLD)
                .build();
        } else if (statusCode.equals(NOT_FOUND)) {
            responseBody = "Account not found";
        } else {
            throw ex;
        }
            return ResponseEntity.status(ex.getStatusCode()).body(responseBody);
    }

    @ResponseStatus(NOT_FOUND)
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
