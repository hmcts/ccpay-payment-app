package uk.gov.hmcts.payment.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.path.json.config.JsonPathConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.PostConstruct;

import static io.restassured.config.JsonConfig.jsonConfig;

@Configuration
@ComponentScan("uk.gov.hmcts.payment.functional")
@PropertySource("classpath:application-functional-tests.properties")
@ActiveProfiles({"local", "functionaltest", "liberataMock"})
public class TestContextConfiguration {

    @Value("${test.url:http://localhost:8080}")
    private String baseURL;

    @PostConstruct
    public void initialize() {
        RestAssured.config = RestAssured.config()
            .objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> new ObjectMapper())
            )
        .jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.baseURI = baseURL;
    }

}
