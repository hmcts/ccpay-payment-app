package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.service.AccountService;

@RestController
@Api(tags = {"AccountController"})
@SwaggerDefinition(tags = {@Tag(name = "AccountController", description = "Account API")})
public class AccountController {
    @Autowired
    private AccountService<AccountDto, String> accountService;

    @ApiOperation(value = "Get the account status and available balance for a PBA account number", notes = "Get the account status and available balance for a PBA account number")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Account details retrieved"),
        @ApiResponse(code = 404, message = "Account not found")
    })
    @RequestMapping(value = "/accounts/{accountNumber}", method = RequestMethod.GET)
    public AccountDto getAccounts(@PathVariable("accountNumber") String accountNumber) {
        AccountDto response = accountService.retrieve(accountNumber);
        return response;
    }
}
