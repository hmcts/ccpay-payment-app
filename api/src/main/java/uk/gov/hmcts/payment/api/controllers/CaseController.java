package uk.gov.hmcts.payment.api.controllers;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.PaymentDeletionAuditRequest;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupResponse;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.RefundListDtoResponse;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.BarPaymentDeletionService;
import uk.gov.hmcts.payment.api.service.CasePaymentDeletionService;
import uk.gov.hmcts.payment.api.service.PaymentDeletionAuditDetails;
import uk.gov.hmcts.payment.api.service.PaymentDeletionAuditService;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;
import uk.gov.hmcts.payment.api.service.PaymentRefundsService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.service.RefundRemissionEnableService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentGroupNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.util.ArrayList;
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
    private final CasePaymentDeletionService casePaymentDeletionService;
    private final BarPaymentDeletionService barPaymentDeletionService;
    private final PaymentDeletionAuditService paymentDeletionAuditService;

    @Autowired
    private ServiceRequestDomainService orderDomainService;

    @Autowired
    private RefundRemissionEnableService refundRemissionEnableService;

    @Autowired
    private PaymentRefundsService paymentRefundsService;

    @Autowired
    public CaseController(PaymentService<PaymentFeeLink, String> paymentService,
                          PaymentGroupService paymentGroupService,
                          PaymentDtoMapper paymentDtoMapper,
                          PaymentGroupDtoMapper paymentGroupDtoMapper,
                          CasePaymentDeletionService casePaymentDeletionService,
                          BarPaymentDeletionService barPaymentDeletionService,
                          PaymentDeletionAuditService paymentDeletionAuditService) {
        this.paymentService = paymentService;
        this.paymentGroupService = paymentGroupService;
        this.paymentDtoMapper = paymentDtoMapper;
        this.paymentGroupDtoMapper = paymentGroupDtoMapper;
        this.casePaymentDeletionService = casePaymentDeletionService;
        this.barPaymentDeletionService = barPaymentDeletionService;
        this.paymentDeletionAuditService = paymentDeletionAuditService;
    }

    @Operation(summary = "Get payments for a case", description = "Get payments for a case")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payments retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    @RateLimiter(name = "default-rate-limiter")
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

    @Operation(summary = "Delete payments for a case",
        description = "Delete all payment and Digital BAR payment data associated with a CCD case number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Payments deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Service authentication failed"),
        @ApiResponse(responseCode = "500", description = "Unexpected error during deletion")
    })
    @RateLimiter(name = "default-rate-limiter")
    @DeleteMapping(value = "/cases/{ccd-case-number}/payments")
    @PaymentExternalAPI
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCasePayments(@PathVariable(name = "ccd-case-number") String ccdCaseNumber) {
        LOG.info("Received request to delete payments for CCD case number: {}", ccdCaseNumber);
        barPaymentDeletionService.deleteBarPaymentsByCcdCaseNumber(ccdCaseNumber);
        casePaymentDeletionService.deletePaymentsByCcdCaseNumber(ccdCaseNumber);
        paymentDeletionAuditService.recordDeletionAudits(List.of(
            new PaymentDeletionAuditDetails(ccdCaseNumber, "Payment Disposer", null, true)
        ));
    }

    @Operation(summary = "Record payment deletion audits",
        description = "Persist payment_audit_deletion rows for retain-and-dispose success or failure")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Deletion audits recorded"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "403", description = "Service authentication failed"),
        @ApiResponse(responseCode = "500", description = "Unexpected error while recording audit")
    })
    @RateLimiter(name = "default-rate-limiter")
    @PostMapping(value = "/cases/payments/deletion-audit")
    @PaymentExternalAPI
    @ResponseStatus(HttpStatus.CREATED)
    public void recordPaymentDeletionAudits(
        @Valid @RequestBody @NotEmpty List<@Valid PaymentDeletionAuditRequest> requests) {
        LOG.info("Received request to record {} payment deletion audit entr{}",
            requests.size(), requests.size() == 1 ? "y" : "ies");
        paymentDeletionAuditService.recordDeletionAudits(
            requests.stream()
                .map(request -> new PaymentDeletionAuditDetails(
                    request.getCcdCaseNumber(),
                    request.getSourceSystem(),
                    request.getErrorMessage(),
                    request.getSuccess()
                ))
                .toList()
        );
    }

    @Operation(summary = "Get payment groups for a case", description = "Get payment groups for a case")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment Groups retrieved"),
        @ApiResponse(responseCode = "204", description = "Payment Groups not found"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "403", description = "Payment Info Forbidden"),
        @ApiResponse(responseCode = "404", description = "Payment Groups not found")
    })
    @RateLimiter(name = "default-rate-limiter")
    @RequestMapping(value = "/cases/{ccdcasenumber}/paymentgroups", method = GET)
    public ResponseEntity<PaymentGroupResponse> retrieveCasePaymentGroups(@PathVariable(name = "ccdcasenumber") String ccdCaseNumber,
        @RequestHeader(required = false) MultiValueMap<String, String> headers) {

        refundRemissionEnableService.setUserRoles(headers);
        List<PaymentGroupDto> paymentGroups = paymentGroupService
            .search(ccdCaseNumber)
            .stream()
            .map(paymentGroupDtoMapper::toPaymentGroupDto)
            .map(paymentGroupDtoMapper::calculateOverallBalance)
            .collect(Collectors.toList());

        if (paymentGroups == null || paymentGroups.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        PaymentGroupResponse paymentGroupResponse = new PaymentGroupResponse(paymentGroups);

        paymentGroupResponse = paymentRefundsService.checkRefundAgainstRemissionV2(headers, paymentGroupResponse, ccdCaseNumber);

        RefundListDtoResponse refundListDtoResponse = paymentRefundsService.getRefundsApprovedFromRefundService(ccdCaseNumber,headers);
        addRefunds(refundListDtoResponse, paymentGroups);

        return ResponseEntity.ok(paymentGroupResponse);
    }

    private void addRefunds(RefundListDtoResponse refundListDtoResponse, List<PaymentGroupDto> paymentGroups){
        if (refundListDtoResponse != null) {
            refundListDtoResponse.getRefundList().stream().forEach(refund -> {

                    String paymentReference = refund.getPaymentReference();
                    paymentGroups.stream().forEach(paymentGroup -> {
                        if (isThereAnyPaymentReferenceInCurrentServiceRequest(paymentGroup.getPayments(),paymentReference ) ) {
                            if (paymentGroup.getRefunds()==null){
                                paymentGroup.setRefunds(new ArrayList<>());
                            }
                            paymentGroup.getRefunds().add(refund);
                        }
                    });
                }
            );
        }

    }

    private boolean isThereAnyPaymentReferenceInCurrentServiceRequest(List<PaymentDto> payments, String paymentReference){
        if (paymentReference == null || payments == null) {
            return false;
        }
        return payments.stream()
            .anyMatch(payment -> payment.getReference().equals(paymentReference));
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
