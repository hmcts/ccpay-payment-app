package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.minidev.json.JSONObject;
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
import uk.gov.hmcts.payment.api.exception.PbaAccountStatusException;
import uk.gov.hmcts.payment.api.service.AccountService;

@RestController
@Tag(name = "AccountController", description = "Account REST API")
public class AccountController {

    private static final String STATUS_DELETED = "Deleted";
    private static final String STATUS_ON_HOLD = "On-Hold";
    private static final String JSON_STATUS = "status";
    private static final String JSON_ERROR_MESSAGE = "error_message";

    private static final String JSON_ERROR_MESSAGE_GONE = "The account has been deleted and is no longer available.";
    private static final String JSON_ERROR_MESSAGE_ON_HOLD = "The account is on hold and temporarily unavailable.";

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
            return accountService.retrieve(accountNumber);
        } catch (HttpClientErrorException ex) {
            LOG.error("Error while  calling account", ex);
            if (ex.getStatusCode() == HttpStatus.GONE || ex.getStatusCode() == HttpStatus.PRECONDITION_FAILED) {

                JSONObject jsonResponse = new JSONObject();

                switch (ex.getStatusCode()) {
                    case GONE:
                        jsonResponse.appendField(JSON_STATUS, STATUS_DELETED);
                        jsonResponse.appendField(JSON_ERROR_MESSAGE, JSON_ERROR_MESSAGE_GONE);
                        throw new PbaAccountStatusException(jsonResponse.toJSONString());
                    case PRECONDITION_FAILED:
                        jsonResponse.appendField(JSON_STATUS, STATUS_ON_HOLD);
                        jsonResponse.appendField(JSON_ERROR_MESSAGE, JSON_ERROR_MESSAGE_ON_HOLD);
                        throw new PbaAccountStatusException(jsonResponse.toJSONString());
                    default:
                        throw ex;
                }
            } else {
                throw new AccountNotFoundException("Account not found");
            }

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
