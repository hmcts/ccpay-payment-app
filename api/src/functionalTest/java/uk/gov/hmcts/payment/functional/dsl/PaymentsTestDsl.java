package uk.gov.hmcts.payment.functional.dsl;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;

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
    private S2sTokenService serviceTokenFactory;

    @Autowired
    private IdamService idamService;

    private Response response;

    public PaymentGivenDsl given() {
        return new PaymentGivenDsl();
    }

    public class PaymentGivenDsl {

        public PaymentGivenDsl userToken(String userToken) {
            headers.put("Authorization", userToken);
            return this;
        }

        public PaymentGivenDsl s2sToken(String serviceToken) {
            headers.put("ServiceAuthorization", serviceToken);
            return this;
        }

        public PaymentGivenDsl returnUrl(String url) {
            headers.put("return-url", url);
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
            response = newRequest().get("/users/{userToken}/payments/{paymentId}", userId, paymentId);
            return this;
        }

        public PaymentWhenDsl getAccountInfomation(String accountNumber) {
            response = newRequest().get("/accounts/" + accountNumber);
            return this;
        }

        public PaymentWhenDsl getBuildInfo() {
            response = newRequest().get("/info");
            return this;
        }

        public PaymentWhenDsl createCardPayment(CardPaymentRequest cardPaymentRequest) {
            response = newRequest().contentType(ContentType.JSON).body(cardPaymentRequest).post("/card-payments");
            return this;
        }

        public PaymentWhenDsl createTelephonyPayment(PaymentRecordRequest paymentRecordRequest) {
            response = newRequest().contentType(ContentType.JSON).body(paymentRecordRequest).post("/payment-records");
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

        public PaymentWhenDsl searchPaymentsByServiceBetweenDates(Service serviceName, String startDate, String endDate) {
            StringBuilder sb = new StringBuilder("/payments?");
            sb.append("start_date=").append(startDate);
            sb.append("&end_date=").append(endDate);
            sb.append("&service_name=").append(serviceName);
            response = newRequest().get(sb.toString());

            return this;
        }

        public PaymentWhenDsl searchPaymentsByPBANumber(String pbaNumber) {
            StringBuilder sb = new StringBuilder("/payments?");
            sb.append("pba_number=").append(pbaNumber);
            response = newRequest().get(sb.toString());

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

        public AccountDto getAccount() {
            return response.then().statusCode(200).extract().as(AccountDto.class);
        }

        public PaymentThenDsl getPayments(Consumer<PaymentsResponse> paymentsResponseAssertions) {
            PaymentsResponse paymentsResponse = response.then().statusCode(200).extract().as(PaymentsResponse.class);
            paymentsResponseAssertions.accept(paymentsResponse);
            return this;
        }

        public PaymentThenDsl validationError(String message) {
            String validationError = response.then().statusCode(422).extract().body().asString();
            Assertions.assertThat(validationError).isEqualTo(message);
            return this;
        }

        public Response validationErrorFor400() {
            return response.then().statusCode(400).extract().response();
        }

    }

}
