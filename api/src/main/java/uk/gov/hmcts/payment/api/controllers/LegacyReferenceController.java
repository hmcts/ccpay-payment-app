package uk.gov.hmcts.payment.api.controllers;


import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.dto.LegacySiteDTO;
import uk.gov.hmcts.payment.api.model.LegacySite;
import uk.gov.hmcts.payment.api.service.LegacySiteService;

import java.util.List;

@RestController
@Api(tags = {"Legacy Reference Data"})
@SwaggerDefinition(tags = {@Tag(name = "LegacyReferenceController", description = "Legacy Reference Data REST API")})
@RequestMapping("/legacy-references")
public class LegacyReferenceController {


    @Autowired
    private LegacySiteService<LegacySite, String> legacySiteService;


    @ApiOperation(value = "Get allowed legacy sites")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Legacy sites retrieved successfully")
    })
    @GetMapping(value = "/sites")
    @ResponseBody
    public ResponseEntity<List<LegacySiteDTO>> getLegacySites() {
        return new ResponseEntity<>(LegacySiteDTO.fromLegacySiteList(legacySiteService.getAllSites()), HttpStatus.OK);
    }

}
