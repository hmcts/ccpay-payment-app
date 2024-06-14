package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupResponse;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.RefundListDtoResponse;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;
import uk.gov.hmcts.payment.api.service.PaymentRefundsService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.service.RefundRemissionEnableService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentGroupNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@Tag(name = "CaseController", description = "Case REST API")
@Validated
public class CaseController {

    private static final Logger LOG = LoggerFactory.getLogger(CaseController.class);

    private final PaymentService<PaymentFeeLink, String> paymentService;
    private final PaymentGroupService<PaymentFeeLink, String> paymentGroupService;
    private final PaymentDtoMapper paymentDtoMapper;
    private final PaymentGroupDtoMapper paymentGroupDtoMapper;

    @Autowired
    private ServiceRequestDomainService orderDomainService;

    @Autowired
    private RefundRemissionEnableService refundRemissionEnableService;

    @Autowired
    private PaymentRefundsService paymentRefundsService;

    @Autowired
    public CaseController(PaymentService<PaymentFeeLink, String> paymentService, PaymentGroupService paymentGroupService,
                          PaymentDtoMapper paymentDtoMapper, PaymentGroupDtoMapper paymentGroupDtoMapper) {
        this.paymentService = paymentService;
        this.paymentGroupService = paymentGroupService;
        this.paymentDtoMapper = paymentDtoMapper;
        this.paymentGroupDtoMapper = paymentGroupDtoMapper;
    }

    @Operation(summary = "Get payments for a case", description = "Get payments for a case")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payments retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    @RequestMapping(value = "/cases/{case}/payments", method = GET)
    @PaymentExternalAPI
    public PaymentsResponse retrieveCasePayments(@PathVariable(name = "case") String ccdCaseNumber) {

        List<PaymentDto> payments = paymentService
            .search(PaymentSearchCriteria.searchCriteriaWith().ccdCaseNumber(ccdCaseNumber).build())
            .stream()
            .map(paymentDtoMapper::toReconciliationResponseDto)
            .collect(Collectors.toList());

        if (payments == null || payments.isEmpty()) {
            throw new PaymentNotFoundException();
        }

        return new PaymentsResponse(payments);
    }


    @Operation(summary = "Get payment groups for a case", description = "Get payment groups for a case")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment Groups retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "403", description = "Payment Info Forbidden"),
        @ApiResponse(responseCode = "404", description = "Payment Groups not found")
    })
    @RequestMapping(value = "/cases/{ccdcasenumber}/paymentgroups", method = GET)
    public PaymentGroupResponse retrieveCasePaymentGroups(@PathVariable(name = "ccdcasenumber") String ccdCaseNumber,
        @RequestHeader(required = false) MultiValueMap<String, String> headers) {

        refundRemissionEnableService.setUserRoles(headers);
        List<PaymentGroupDto> paymentGroups = paymentGroupService
            .search(ccdCaseNumber)
            .stream()
            .map(paymentGroupDtoMapper::toPaymentGroupDto)
            .map(paymentGroupDtoMapper::calculateOverallBalance)
            .collect(Collectors.toList());

        if (paymentGroups == null || paymentGroups.isEmpty()) {
            throw new PaymentGroupNotFoundException("No Service found for given CaseType or HMCTS Org Id");
        }
        PaymentGroupResponse paymentGroupResponse = new PaymentGroupResponse(paymentGroups);

        paymentGroupResponse = paymentRefundsService.checkRefundAgainstRemissionV2(headers, paymentGroupResponse, ccdCaseNumber);


        RefundListDtoResponse refundListDtoResponse = paymentRefundsService.getRefundsApprovedFromRefundService(ccdCaseNumber,headers);
        if (refundListDtoResponse != null) {
            paymentGroups.stream().forEach(paymentGroup -> paymentGroup.setRefunds(refundListDtoResponse.getRefundList()));
        }

        LOG.info("Refund " + paymentGroupResponse.getPaymentGroups().get(0).getRefunds());
        LOG.info("END case number:-----"+ ccdCaseNumber);


        return paymentGroupResponse;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentException.class)
    public String notFound(PaymentException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentGroupNotFoundException.class)
    public String notFound(PaymentGroupNotFoundException ex) {
        return ex.getMessage();
    }

}
