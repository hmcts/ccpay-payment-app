package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.util.ExcelGeneratorUtil;
import uk.gov.hmcts.payment.api.util.ReportType;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.payment.api.util.DateUtil.getDateForReportName;
import static uk.gov.hmcts.payment.api.util.DateUtil.getDateTimeForReportName;

@RestController
@Api(tags = {"Bulk Scanning Report"})
@SwaggerDefinition(tags = {@Tag(name = "BulkScanningReportController", description = "Bulk Scanning report REST API")})
public class BulkScanningReportController {
    private final PaymentService<PaymentFeeLink, String> paymentService;
    private static final Logger LOG = LoggerFactory.getLogger(BulkScanningReportController.class);

    @Autowired
    public BulkScanningReportController(PaymentService<PaymentFeeLink, String> paymentService) {
        this.paymentService = paymentService;
    }

    @ApiOperation("API to generate Report for Bulk Scan Payment System")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Report Generated"),
        @ApiResponse(code = 404, message = "No Data found to generate Report")
    })
    @GetMapping("/payment/bulkscan-report-download")
    public ResponseEntity retrieveBulkScanReports(
        @RequestParam("date_from") Date fromDate,
        @RequestParam("date_to") Date toDate,
        @RequestParam("report_type") ReportType reportType) {
        LOG.info("Retrieving payments for reportType : {}", reportType);
        ByteArrayInputStream in = null;
        try {

            List<PaymentFeeLink> paymentFeeLinks = paymentService
                .search(
                    PaymentSearchCriteria
                        .searchCriteriaWith()
                        .startDate(fromDate)
                        .endDate(toDate)
                        .build()
                );


            if (Optional.ofNullable(paymentFeeLinks).isPresent()) {
                LOG.info("No of Records exists : {}", paymentFeeLinks.size());

                    in =  ExcelGeneratorUtil.exportToExcel(reportType,paymentFeeLinks);

                    HttpHeaders headers = new HttpHeaders();
                String fileName = reportType.toString() + "_"
                    + getDateForReportName(fromDate) + "_To_"
                    + getDateForReportName(toDate) + "_RUN_"
                    + getDateTimeForReportName(new Date(System.currentTimeMillis()));
                String headerValue = "attachment; filename=" + fileName;
                headers.add("Content-Disposition", headerValue);

                return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(new InputStreamResource(in));
            } else {
                LOG.info("Payment Records not found for Report-Type : {}", reportType);
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        } finally {
            try {
                if(in !=null) {
                    in.close();
                }
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }
}
