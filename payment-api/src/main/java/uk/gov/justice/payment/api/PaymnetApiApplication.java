package uk.gov.justice.payment.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymnetApiApplication {

	public static void main(String[] args) {
		System.out.println("Hello");
		SpringApplication.run(PaymnetApiApplication.class, args);
	}
}
