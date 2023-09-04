package uk.gov.hmcts.payment.referencedata.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.util.List;

@RestController
@Tag(name = "ReferenceDataController", description = "Reference Data REST API")
public class ReferenceDataController {

    @Autowired
    private SiteService<Site, String> siteService;

    @Operation(summary = "Get allowed sites")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sites retrieved successfully")
    })
    @GetMapping(value = "/reference-data/sites")
    @ResponseBody
    public ResponseEntity<List<SiteDTO>> getSites() {
        return new ResponseEntity<>(SiteDTO.fromSiteList(siteService.getAllSites()), HttpStatus.OK);
    }

}
