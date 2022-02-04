package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.service.FeesService;
//422222222222
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

    @ApiOperation(value = "Delete Fees by Id", notes = "Delete Fees by Id")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Fees Deleted"),
        @ApiResponse(code = 400, message = "Fees not found")
    })
    @DeleteMapping(value = "/fees/{feeId}")
    public ResponseEntity<Boolean> deleteFee(@PathVariable("feeId") String feeId) throws EmptyResultDataAccessException {
        feesService.deleteFee(Integer.parseInt(feeId));
        return new ResponseEntity<>(true, HttpStatus.NO_CONTENT);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(EmptyResultDataAccessException.class)
    public String return400(EmptyResultDataAccessException ex) {
        return ex.getMessage();
    }

}
