package uk.gov.hmcts.payment.functional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.restassured.parsing.Parser;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;

import java.lang.reflect.Type;

import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;

@Configuration
@ComponentScan("uk.gov.hmcts.payment.functional")
@PropertySource("classpath:application-functional-tests.properties")
public class TestContextConfiguration {

    static {
        // Set the default parser globally
        RestAssured.defaultParser = Parser.JSON;
    }


    @Value("${test.url:http://localhost:8080}")
    private String baseURL;

    @PostConstruct
    public void initialize() {


        // Create and configure the Jackson ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Register the configured ObjectMapper with RestAssured
        RestAssured.config = RestAssured.config()
            .objectMapperConfig(objectMapperConfig().jackson2ObjectMapperFactory(new Jackson2ObjectMapperFactory() {
                @Override
                public ObjectMapper create(Type cls, String charset) {
                    return objectMapper;
                }
            })).jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));

        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.baseURI = baseURL;
    }

}
