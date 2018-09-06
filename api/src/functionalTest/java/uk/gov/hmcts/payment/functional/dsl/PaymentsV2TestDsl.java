package uk.gov.hmcts.payment.functional.dsl;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.v1.contract.PaymentOldDto;
import uk.gov.hmcts.payment.api.v1.contract.RefundPaymentRequestDto.RefundPaymentRequestDtoBuilder;
import uk.gov.hmcts.payment.functional.tokens.ServiceTokenFactory;
import uk.gov.hmcts.payment.functional.tokens.UserTokenFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@Scope("prototype")
public class PaymentsV2TestDsl {
    private final Map<String, String> headers = new HashMap<>();

    @Value("${test.url:http://pr-276-payment-api-preview-staging.service.core-compute-preview.internal}")
    private String baseURL;

    private static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final ServiceTokenFactory serviceTokenFactory;
    private final UserTokenFactory userTokenFactory;
    private Response response;

    @Autowired
    public PaymentsV2TestDsl(ServiceTokenFactory serviceTokenFactory, UserTokenFactory userTokenFactory) {
        this.serviceTokenFactory = serviceTokenFactory;
        this.userTokenFactory = userTokenFactory;
    }

    public PaymentGivenDsl given() {
        return new PaymentGivenDsl();
    }

    public class PaymentGivenDsl {
        public PaymentGivenDsl createUser(String userId, String password, String role, String userGroup) {
            userTokenFactory.setUpUser(userId, password, role, userGroup);
            return this;
        }

        public PaymentGivenDsl userId(String userId, String password) {
            headers.put("Authorization", userTokenFactory.validTokenForUser(userId, password));
            return this;
        }

        public PaymentGivenDsl serviceId(String id, String serviceSecret) {
            headers.put("ServiceAuthorization", serviceTokenFactory.validTokenForService(id, serviceSecret));
            return this;
        }

        public PaymentGivenDsl returnUrl(String url) {
            headers.put("return-url", url);
            return this;
        }

        public PaymentGivenDsl deleteUser(String userId) {
            userTokenFactory.deleteUser(userId);
            return this;
        }

        public PaymentWhenDsl when() {
            return new PaymentWhenDsl();
        }
    }

    public class PaymentWhenDsl {
        private RequestSpecification newRequest() {
            return RestAssured.given().relaxedHTTPSValidation().baseUri(baseURL).contentType(ContentType.JSON).headers(headers);
        }

        public PaymentWhenDsl getPayment(String userId, String paymentId) {
            response = newRequest().get("/users/{userId}/payments/{paymentId}", userId, paymentId);
            return this;
        }

        public PaymentWhenDsl getBuildInfo() {
            response = newRequest().get("/info");
            return this;
        }

        public PaymentWhenDsl createCardPayment(String cardPaymentRequest) {
            response = newRequest().body(cardPaymentRequest).post( "/card-payments");
            return this;
        }

        public PaymentWhenDsl getCardPayment(String reference) {
            response = newRequest().get("/card-payments/" + reference);
            return this;
        }

        public PaymentWhenDsl cancelPayment(String userId, String paymentId) {
            response = newRequest().post("/users/{userId}/payments/{paymentId}/cancel", userId, paymentId);
            return this;
        }

        public PaymentWhenDsl refundPayment(String userId, RefundPaymentRequestDtoBuilder requestDto, String paymentId) {
            response = newRequest().body(requestDto.build()).post("/users/{userId}/payments/{paymentId}/refunds", userId, paymentId);
            return this;
        }

        public PaymentThenDsl then() {
            return new PaymentThenDsl();
        }
    }

    public class PaymentThenDsl {
        public PaymentThenDsl forbidden() {
            response.then().statusCode(403);
            return this;
        }

        public PaymentThenDsl notFound() {
            response.then().statusCode(404);
            return this;
        }

        public PaymentThenDsl created(Consumer<PaymentDto> payment) {
            PaymentDto paymentDto = response.then().statusCode(201).extract().as(PaymentDto.class);
            payment.accept(paymentDto);
            return this;
        }

        public <T> PaymentThenDsl got(Class<T> type, Consumer<T> assertions) {
            T dto = response.then().statusCode(200).extract().as(type);
            assertions.accept(dto);
            return this;
        }

        public PaymentThenDsl cancelled() {
            response.then().statusCode(204);
            return this;
        }

        public PaymentThenDsl get(Consumer<PaymentDto> paymentAssertions) {
            PaymentDto paymentDto = response.then().statusCode(200).extract().as(PaymentDto.class);
            paymentAssertions.accept(paymentDto);
            return this;
        }

        public PaymentDto get() {
            return response.then().statusCode(200).extract().as(PaymentDto.class);
        }

        public PaymentThenDsl validationError(String message) {
            String validationError = response.then().statusCode(422).extract().body().asString();
            Assertions.assertThat(validationError).isEqualTo(message);
            return this;
        }

        public PaymentThenDsl validationErrorfor500(String message) {
            String validationError = response.then().statusCode(500).extract().body().asString();
            Assertions.assertThat(validationError).isEqualTo(message);
            return this;
        }

        public PaymentThenDsl refundPayment() {
            response.then().statusCode(201);
            return this;
        }

        public PaymentThenDsl refundAvailableAmountInvalid412() {
            response.then().statusCode(412);
            return this;
        }

    }

    public String generateUserId() {
        return UUID.randomUUID().toString() + "@hmcts.net";
    }

    public String generatePassword() {
        return RandomStringUtils.random(15, characters);
    }
}
