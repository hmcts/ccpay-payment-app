package uk.gov.hmcts.payment.referencedata.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.referencedata.dto.LegacySiteDTO;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;
import uk.gov.hmcts.payment.referencedata.model.LegacySite;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.LegacySiteService;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.util.List;

@RestController
@Api(tags = {"Reference Data"})
@SwaggerDefinition(tags = {@Tag(name = "ReferenceDataController", description = "Reference Data REST API")})
public class ReferenceDataController {

    @Autowired
    private SiteService<Site, String> siteService;

    @Autowired
    private LegacySiteService<LegacySite, String> legacySiteService;

    @ApiOperation(value = "Get allowed sites")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Sites retrieved successfully")
    })
    @GetMapping(value = "/reference-data/sites")
    @ResponseBody
    public ResponseEntity<List<SiteDTO>> getSites() {
        return new ResponseEntity<>(SiteDTO.fromSiteList(siteService.getAllSites()), HttpStatus.OK);
    }

    @ApiOperation(value = "Get allowed legacy sites")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Legacy sites retrieved successfully")
    })
    @GetMapping(value = "/reference-data/legacy-sites")
    @ResponseBody
    public ResponseEntity<List<LegacySiteDTO>> getLegacySites() {
        return new ResponseEntity<>(LegacySiteDTO.fromLegacySiteList(legacySiteService.getAllSites()), HttpStatus.OK);
    }
}
