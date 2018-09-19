package uk.gov.hmcts.payment.functional.dsl;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.v1.contract.RefundPaymentRequestDto.RefundPaymentRequestDtoBuilder;
import uk.gov.hmcts.payment.functional.tokens.ServiceTokenFactory;
import uk.gov.hmcts.payment.functional.tokens.UserTokenFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
@Scope("prototype")
public class PaymentsTestDsl {
    private final Map<String, String> headers = new HashMap<>();

    @Value("${test.url}")
    private String baseURL;

    @Autowired
    private ServiceTokenFactory serviceTokenFactory;

    private final UserTokenFactory userTokenFactory;

    private Response response;

    @Autowired
    public PaymentsTestDsl(UserTokenFactory userTokenFactory) {
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

        public PaymentGivenDsl userId(String email, String userId, String password, String role) {
            headers.put("Authorization", userTokenFactory.validTokenForUser(email, userId, password, role));
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

        public PaymentWhenDsl searchPaymentsBetweenDates(String startDate, String endDate) {
            if (startDate != null && endDate != null) {
                response = newRequest().get("/payments?start_date=" + startDate + "&end_date=" + endDate);
            } else if (startDate != null) {
                response = newRequest().get("/payments?start_date=" + startDate);
            } else if (endDate != null) {
                response = newRequest().get("/payments?end_date=" + endDate);
            }

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

        public PaymentsResponse getPayments() {
            return response.then().statusCode(200).extract().as(PaymentsResponse.class);
        }

        public PaymentThenDsl validationError(String message) {
            String validationError = response.then().statusCode(422).extract().body().asString();
            Assertions.assertThat(validationError).isEqualTo(message);
            return this;
        }

        public Response validationErrorFor400() {
            return response.then().statusCode(400).extract().response();
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

}
