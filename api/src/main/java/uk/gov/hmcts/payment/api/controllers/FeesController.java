package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.service.FeesService;

@RestController
@Api(tags = {"Fees"})
@SwaggerDefinition(tags = {@Tag(name = "Fees", description = "Fees REST API")})
@Validated
public class FeesController {
    private static final Logger LOG = LoggerFactory.getLogger(FeesController.class);
    private final FeesService feesService;

    @Autowired
    public FeesController(FeesService feesService) {
        this.feesService = feesService;
    }

    @ApiOperation(value = "Delete fees/remissions details for supplied payment group reference", notes = "Delete fees/remissions details for supplied payment group reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Fees Deleted"),
        @ApiResponse(code = 404, message = "Fees not found")
    })
    @DeleteMapping(value = "/fees/{feeId}")
    public ResponseEntity<Boolean> retrievePayment(@PathVariable("feeId") String feeId)  {
        feesService.deleteFee(Integer.parseInt(feeId));
        return new ResponseEntity<>(true, HttpStatus.NO_CONTENT);
    }

}
