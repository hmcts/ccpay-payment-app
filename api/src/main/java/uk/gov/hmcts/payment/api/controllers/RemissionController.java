package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = {"Remission"})
@SwaggerDefinition(tags = {@Tag(name = "RemissionController", description = "Remission API")})
public class RemissionController {

}
