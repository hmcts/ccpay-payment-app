package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.domain.service.CaseDetailsDomainService;
import uk.gov.hmcts.payment.api.domain.service.FeeDomainService;
import uk.gov.hmcts.payment.api.domain.service.PaymentDomainService;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupResponse;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.exception.CaseDetailsNotFoundException;
import uk.gov.hmcts.payment.api.model.CaseDetails;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentGroupNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@Api(tags = {"Case"})
@SwaggerDefinition(tags = {@Tag(name = "CaseController", description = "Case REST API")})
public class CaseController {

    private final PaymentService<PaymentFeeLink, String> paymentService;
    private final PaymentGroupService<PaymentFeeLink, String> paymentGroupService;
    private final PaymentDtoMapper paymentDtoMapper;
    private final PaymentGroupDtoMapper paymentGroupDtoMapper;

    @Autowired
    private CaseDetailsDomainService caseDetailsDomainService;

    @Autowired
    private FeeDomainService feeDomainService;

    @Autowired
    private PaymentDomainService paymentDomainService;

    @Autowired
    public CaseController(PaymentService<PaymentFeeLink, String> paymentService, PaymentGroupService paymentGroupService,
                          PaymentDtoMapper paymentDtoMapper, PaymentGroupDtoMapper paymentGroupDtoMapper) {
        this.paymentService = paymentService;
        this.paymentGroupService = paymentGroupService;
        this.paymentDtoMapper = paymentDtoMapper;
        this.paymentGroupDtoMapper = paymentGroupDtoMapper;
    }

    @ApiOperation(value = "Get payments for a case", notes = "Get payments for a case")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @RequestMapping(value = "/cases/{case}/payments", method = GET)
    @PaymentExternalAPI
    public PaymentsResponse retrieveCasePayments(@PathVariable(name = "case") String ccdCaseNumber) {

        List<PaymentDto> payments = paymentService
            .search(PaymentSearchCriteria.searchCriteriaWith().ccdCaseNumber(ccdCaseNumber).build())
            .stream()
            .map(paymentDtoMapper::toReconciliationResponseDto)
            .collect(Collectors.toList());

        if(payments == null || payments.isEmpty()) {
            throw new PaymentNotFoundException();
        }

        return new PaymentsResponse(payments);
    }


    @ApiOperation(value = "Get payment groups for a case", notes = "Get payment groups for a case")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment Groups retrieved"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 403, message = "Payment Info Forbidden"),
        @ApiResponse(code = 404, message = "Payment Groups not found")
    })
    @RequestMapping(value = "/cases/{ccdcasenumber}/paymentgroups", method = GET)
    public PaymentGroupResponse retrieveCasePaymentGroups(@PathVariable(name = "ccdcasenumber") String ccdCaseNumber) {

        List<PaymentGroupDto> paymentGroups = paymentGroupService
            .search(ccdCaseNumber)
            .stream()
            .map(paymentGroupDtoMapper::toPaymentGroupDto)
            .collect(Collectors.toList());

        if(paymentGroups == null || paymentGroups.isEmpty()) {
            throw new PaymentGroupNotFoundException();
        }

        return new PaymentGroupResponse(paymentGroups);
    }

    @ApiOperation(value = "Get payment groups for a case using Orders", notes = "Get payment groups for a case using Orders")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment Groups retrieved"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 403, message = "Payment Info Forbidden"),
        @ApiResponse(code = 404, message = "Payment Groups not found")
    })
    @RequestMapping(value = "/orderpoc/cases/{ccdcasenumber}/paymentgroups", method = GET)
    public  PaymentGroupResponse retrieveCasePaymentGroups_NewAPI(@PathVariable(name = "ccdcasenumber") String ccdCaseNumber) {

        CaseDetails caseDetails = caseDetailsDomainService.findByCcdCaseNumber(ccdCaseNumber);
        Set<PaymentFeeLink> paymentFeeLinks  = caseDetails.getOrders();
        List<PaymentGroupDto> paymentGroupDtoList = paymentFeeLinks.stream().map(paymentGroupDtoMapper::toPaymentGroupDtoForOrders)
            .collect(Collectors.toList());

        if(paymentGroupDtoList == null || paymentGroupDtoList.isEmpty()) {
            throw new PaymentGroupNotFoundException();
        }

        return new PaymentGroupResponse(paymentGroupDtoList);
    }

    @ApiOperation(value = "Get payments for a case by orders", notes = "Get payments for a case  by orders")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @RequestMapping(value = "/orderpoc/cases/{case}/payments", method = GET)
    @PaymentExternalAPI
    public PaymentsResponse retrieveCasePaymentsByOrders(@PathVariable(name = "case") String ccdCaseNumber) {
        CaseDetails caseDetails = caseDetailsDomainService.findByCcdCaseNumber(ccdCaseNumber);
        Set<PaymentFeeLink> paymentFeeLinks  = caseDetails.getOrders();
        List<PaymentDto> payments = paymentFeeLinks.stream().flatMap(link->toReconciliationResponseDtoForOrders(link).stream())
                                                        .collect(Collectors.toList());
        if(payments == null || payments.isEmpty()) {
            throw new PaymentNotFoundException();
        }

        return new PaymentsResponse(payments);
    }

    private List<PaymentDto> toReconciliationResponseDtoForOrders(PaymentFeeLink paymentFeeLink){
        List<FeePayApportion> feePayApportions =  paymentFeeLink.getFees()
            .stream()
            .flatMap(fee->
                feeDomainService.getFeePayApportionsByFee(fee).stream())
            .collect(Collectors.toList());
        Set<PaymentDto> paymentDtos = feePayApportions
            .stream()
            .map(feePayApportion ->
                paymentDtoMapper.toPaymentDto(paymentDomainService.getPaymentByApportionment(feePayApportion),paymentFeeLink))
            .collect(Collectors.toSet());
        return paymentDtos.stream().collect(Collectors.toList());
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

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(CaseDetailsNotFoundException.class)
    public String notFound(CaseDetailsNotFoundException ex) {
        return ex.getMessage();
    }

}
