package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.service.RemissionService;

import javax.validation.Valid;

@RestController
@Api(tags = {"Remission"}, description = "Remission REST API")
@SwaggerDefinition(tags = {@Tag(name = "RemissionController", description = "Remission API")})
public class RemissionController {
    private static final Logger LOG = LoggerFactory.getLogger(RemissionController.class);

    @Autowired
    private RemissionService remissionService;

    @ApiOperation(value = "Create remission record", notes = "Create remission record")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Remission created"),
        @ApiResponse(code = 400, message = "Remission creation failed")
    })
    @PostMapping(value = "/remission")
    @ResponseBody
    public ResponseEntity createRemission(@Valid @RequestBody RemissionRequest remissionRequest) {
        remissionService.create(remissionRequest.toRemission());

        return new ResponseEntity(HttpStatus.CREATED);
    }
}
