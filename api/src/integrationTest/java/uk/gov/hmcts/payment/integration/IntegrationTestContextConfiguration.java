package uk.gov.hmcts.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.path.json.config.JsonPathConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;

import static io.restassured.config.JsonConfig.jsonConfig;

@Configuration
@ComponentScan("uk.gov.hmcts.payment.integration")
@PropertySource("classpath:application-integration-tests.properties")
public class IntegrationTestContextConfiguration {
}
