package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.reports.ServiceRequestReportService;
import uk.gov.hmcts.payment.api.scheduler.Clock;

import uk.gov.hmcts.payment.api.util.DateUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.slf4j.LoggerFactory.getLogger;

@RestController
@Tag(name = "ServiceRequestReportController", description = "Service Request report REST API")
public class ServiceRequestReportController {

    private static final Logger LOG = getLogger(ServiceRequestReportController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE;
    private final ServiceRequestReportService serviceRequestReportService;


    @Autowired
    public ServiceRequestReportController(ServiceRequestReportService serviceRequestReportService) {
        this.serviceRequestReportService = serviceRequestReportService;
    }

    @Operation(summary = "Email duplicate service request csv reports", description = "fetch duplicates service requests for a date, enter the date in format YYYY-MM-DD")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reports sent")
    })
    @PostMapping(value = "/jobs/email-duplicate-reports")
    public void generateAndEmailReport(@RequestParam(name = "date", required = true) String dateString) {


        LOG.info("Inside /jobs/email-duplicate-reports");
        //validator.validateDate(dateString);

        // cw todo - through an exception? cant figure out what org code did
        //LocalDateTime date = LocalDateTime.parse(dateString, dateUtil.getIsoDateTimeFormatter());


        LocalDate date = LocalDate.parse(dateString, FORMATTER);
        serviceRequestReportService.generateCsvAndSendEmail(date);
    }
}
