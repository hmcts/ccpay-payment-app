package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.reports.ServiceRequestReportService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;

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

    @Operation(summary = "Email duplicate service request count csv reports", description = "fetch duplicates service request count for a date, enter the date in format YYYY-MM-DD")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report sent")
    })
    @PostMapping(value = "/jobs/email-duplicate-sr-report")
    public void generateAndEmailDuplicateSRReport(@RequestParam(name = "date", required = true) String dateString) throws ValidationErrorException {

        LOG.info("Inside /jobs/email-duplicate-sr-report");

        LocalDate date = null;
        try {
           date = LocalDate.parse(dateString, FORMATTER);
        }
        catch (Exception e){
            ValidationErrorDTO dto = new ValidationErrorDTO();
            dto.addFieldError("date", e.getMessage());
            throw new ValidationErrorException("Error occurred", dto);
        }
        serviceRequestReportService.generateDuplicateSRCsvAndSendEmail(date);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationErrorException.class)
    public String reportValidationNotSuccess(ValidationErrorException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String reportMissingDateNotSuccess(MissingServletRequestParameterException ex) {
        return ex.getMessage();
    }

}
