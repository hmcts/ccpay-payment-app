package uk.gov.justice.payment.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.contract.CreatePaymentRequestDto;
import uk.gov.justice.payment.api.contract.PaymentDto;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class PaymentController {
    private final String url;
    private final RestTemplate restTemplate;

    @Autowired
    public PaymentController(@Value("${url}") String url, RestTemplate restTemplate) {
        this.url = url;
        this.restTemplate = restTemplate;
    }

    @RequestMapping
    public String home() {
        return "index";
    }

    @RequestMapping(value = "/payment-intiate", method = POST)
    public String intiatePayment(@RequestParam("amount") int amount,
                                 @RequestParam("email") String email,
                                 @RequestParam("description") String description,
                                 @RequestParam("applicationReference") String applicationReference,
                                 @RequestParam("paymentReference") String paymentReference,
                                 HttpServletResponse httpServletResponse) {
        CreatePaymentRequestDto paymentRequest = new CreatePaymentRequestDto();
        paymentRequest.setAmount(amount);
        paymentRequest.setEmail(email);
        paymentRequest.setDescription(description);
        paymentRequest.setPaymentReference(paymentReference);
        paymentRequest.setApplicationReference(applicationReference);
        paymentRequest.setReturnUrl("https://localhost:8443/payment-result");

        PaymentDto paymentResponse = restTemplate.exchange(url, HttpMethod.POST, httpEntity(paymentRequest), PaymentDto.class).getBody();

        Cookie cookie = new Cookie("paymentId", paymentResponse.getPaymentId());
        cookie.setMaxAge(60 * 60 * 24);
        cookie.setSecure(true);
        httpServletResponse.addCookie(cookie);
        return "redirect:" + paymentResponse.getLinks().getNextUrl().getHref();
    }

    @RequestMapping("/payment-result")
    public String viewPaymentResult(@CookieValue("paymentId") String paymentId, Model model) {
        PaymentDto response = restTemplate.exchange(url + "/" + paymentId, HttpMethod.GET, httpEntity(null), PaymentDto.class).getBody();
        model.addAttribute("paymentStatus", response.getState().getStatus());
        model.addAttribute("isFinished", response.getState().getFinished());
        model.addAttribute("paymentId", paymentId);
        return "payment-result";
    }

    private <T> HttpEntity<T> httpEntity(Object payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("service_id", "divorce");
        return new HttpEntity(payload, headers);
    }
}
