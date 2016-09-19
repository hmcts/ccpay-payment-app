package uk.gov.justice.payment.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.regex;


@SpringBootApplication
@EnableSwagger2
public class PaymentApiApplication {

	private static final Logger logger = LoggerFactory
			.getLogger(PaymentApiApplication.class);


	public static void main(String[] args) {
		logger.debug("Payment API Started");
        SpringApplication.run(PaymentApiApplication.class, args);
	}


    @Bean
    public Docket newsApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("payment")
                .apiInfo(apiInfo())
                .select()
                .paths(regex("/payment.*"))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Payment API documentation")
                .description("Payment API documentation")
                .build();
    }
}
