package uk.gov.justice.payment.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@SpringBootApplication
@EnableAutoConfiguration
@Controller
public class PaymentWebApplication {

	public static void main(String[] args) {

		SpringApplication.run(PaymentWebApplication.class, args);
	}

	@RequestMapping("/test")
	@ResponseBody
	String home() {

		return "Hello World!";
	}
}
