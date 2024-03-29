package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.contract.BulkScanningReportDto;
import uk.gov.hmcts.payment.api.contract.BulkScanningUnderOverPaymentDto;
import uk.gov.hmcts.payment.api.dto.mapper.BulkScanningReportMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.util.ReportType;

import java.util.Date;
import java.util.List;

import static uk.gov.hmcts.payment.api.util.DateUtil.atEndOfDay;
import static uk.gov.hmcts.payment.api.util.DateUtil.atStartOfDay;
import static uk.gov.hmcts.payment.api.util.ReportType.PROCESSED_UNALLOCATED;
import static uk.gov.hmcts.payment.api.util.ReportType.SURPLUS_AND_SHORTFALL;

@RestController
@Tag(name = "BulkScanningReportController", description = "Bulk Scanning report REST API")
public class BulkScanningReportController {
    private final PaymentService<PaymentFeeLink, String> paymentService;
    private static final Logger LOG = LoggerFactory.getLogger(BulkScanningReportController.class);
    private final BulkScanningReportMapper bulkScanningReportMapper;

    @Autowired
    public BulkScanningReportController(PaymentService<PaymentFeeLink, String> paymentService,BulkScanningReportMapper bulkScanningReportMapper) {
        this.paymentService = paymentService;
        this.bulkScanningReportMapper = bulkScanningReportMapper;
    }
    @Operation(summary = "API to generate Report for Bulk Scan Payment System")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report Generated"),
        @ApiResponse(responseCode = "404", description = "No Data found to generate Report")
    })

    @GetMapping("/payment/bulkscan-data-report")
    public ResponseEntity<List<?>> getBulkScanReports(
        @RequestParam("date_from") Date fromDate,
        @RequestParam("date_to") Date toDate,
        @RequestParam("report_type") ReportType reportType) {

        List<Payment> payments = paymentService.getPayments(atStartOfDay(fromDate), atEndOfDay(toDate));
        LOG.info("No of payments exists for the date-range: {}", payments.size());
        if(reportType.equals(PROCESSED_UNALLOCATED)) {
            LOG.info("Processed and Unallocated report section");
            List<BulkScanningReportDto> bulkScanningReportDtoList = bulkScanningReportMapper.toBulkScanningUnallocatedReportDto(payments);
            return new ResponseEntity<>(bulkScanningReportDtoList, HttpStatus.OK);
        }
        else if(reportType.equals(SURPLUS_AND_SHORTFALL))
        {
            LOG.info("Surplus and Shortfall report section");
            List<BulkScanningUnderOverPaymentDto> underOverPaymentDtoList = bulkScanningReportMapper.toSurplusAndShortfallReportdto(payments);
            return new ResponseEntity<>(underOverPaymentDtoList, HttpStatus.OK);
        }
        return null;
    }
}
