package uk.gov.hmcts.payment.api.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.contract.CasePaymentOrdersDto;
import uk.gov.hmcts.payment.api.dto.mapper.CasePaymentOrdersMapper;
import uk.gov.hmcts.payment.api.service.CasePaymentOrdersService;
import uk.gov.hmcts.payment.casepaymentorders.client.dto.CpoGetResponse;
import uk.gov.hmcts.payment.casepaymentorders.client.exceptions.CpoBadRequestException;
import uk.gov.hmcts.payment.casepaymentorders.client.exceptions.CpoClientException;
import uk.gov.hmcts.payment.casepaymentorders.client.exceptions.CpoInternalServerErrorException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Case payment orders controller
 */

@RestController
@Tag(name = "CasePaymentOrderController", description = "Case Payment Order REST API")
public class CasePaymentOrdersController {
    private static final Logger LOG = LoggerFactory.getLogger(CasePaymentOrdersController.class);

    private final CasePaymentOrdersService casePaymentOrdersService;
    private final CasePaymentOrdersMapper casePaymentOrdersMapper;

    @Autowired
    public CasePaymentOrdersController(CasePaymentOrdersService casePaymentOrdersService,
                                       CasePaymentOrdersMapper casePaymentOrdersMapper) {
        this.casePaymentOrdersService = casePaymentOrdersService;
        this.casePaymentOrdersMapper = casePaymentOrdersMapper;
    }

    @Operation(summary = "Get payment orders for a case", description = "Get payment orders for a case")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment orders retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Credentials are required to access this resource"),
        @ApiResponse(responseCode = "403", description = "Case Payment Orders info forbidden"),
        @ApiResponse(responseCode = "404", description = "Case Payment Order does not exist"),
        @ApiResponse(responseCode = "500", description = "Downstream system error")
    })
    @RequestMapping(value = "/case-payment-orders", method = GET)
    public CasePaymentOrdersDto retrieveCasePaymentOrders(
        @Parameter(description = "Coma separated list of case ids.", required = true)
        @RequestParam(name = "case_ids") String caseIds,
        @Parameter(description = "Page number to be served. 1 based index")
        @RequestParam(name = "page_number", required = false) String pageNumber,
        @Parameter(description = "Page size - number of elements on the page.")
        @RequestParam(name = "page_size", required = false) String pageSize,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        CpoGetResponse casePaymentOrders = casePaymentOrdersService
            .getCasePaymentOrders(caseIds, pageNumber, pageSize, authorization);

        return casePaymentOrdersMapper.toCasePaymentOrdersDto(casePaymentOrders);
    }

    @ExceptionHandler(value = {CpoBadRequestException.class})
    public ResponseEntity<String> cpoBadRequestException(CpoBadRequestException e) {
        LOG.error("BadRequest - Error while calling case payment orders", e);
        return new ResponseEntity<>(e.getMessage(), BAD_REQUEST);
    }

    @ExceptionHandler(value = {CpoInternalServerErrorException.class})
    public ResponseEntity<String> cpoInternalServerErrorException(CpoInternalServerErrorException e) {
        LOG.error("InternalServerError - Error while calling case payment orders", e);
        return new ResponseEntity<>(e.getMessage(), INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {CpoClientException.class})
    public ResponseEntity<String> cpoClientException(CpoClientException e) {
        LOG.error("ClientException - Error while calling case payment orders", e);
        return new ResponseEntity<>(e.getMessage(), HttpStatus.valueOf(e.getStatus()));
    }
}
