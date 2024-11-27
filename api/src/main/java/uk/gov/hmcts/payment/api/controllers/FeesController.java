package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.service.FeesService;

import java.util.Optional;

@RestController
@Tag(name = "Fees", description = "Fees REST API")
@Validated
public class FeesController {
    private static final Logger LOG = LoggerFactory.getLogger(FeesController.class);
    private final FeesService feesService;

    @Autowired
    public FeesController(FeesService feesService) {
        this.feesService = feesService;
    }

    @Operation(summary = "Delete Fees by Id", description = "Delete Fees by Id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Fees Deleted"),
        @ApiResponse(responseCode = "400", description = "Fees not found")
    })
    @DeleteMapping(value = "/fees/{feeId}")
    public ResponseEntity<Boolean> deleteFee(@NotNull @PathVariable("feeId") String feeId) throws EmptyResultDataAccessException {
        Optional<PaymentFee> paymentFee = feesService.getPaymentFee(Integer.parseInt(feeId));
        if (paymentFee.isEmpty()) {
            throw new EmptyResultDataAccessException("Fee not found", 1);
        }
        feesService.deleteFee(Integer.parseInt(feeId));
        return new ResponseEntity<>(true, HttpStatus.NO_CONTENT);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(EmptyResultDataAccessException.class)
    public String return400(EmptyResultDataAccessException ex) {
        return ex.getMessage();
    }

}
