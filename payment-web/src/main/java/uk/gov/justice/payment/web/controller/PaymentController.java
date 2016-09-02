package uk.gov.justice.payment.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.json.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.CreatePaymentResponse;
import uk.gov.justice.payment.api.json.ViewPaymentResponse;


@Controller
public class PaymentController {

    @Value("${url}")
    private String url;

    @RequestMapping("/")
    String home() {

        return "index";
    }
    @RequestMapping("/payment-intiate")
    String intiatePayment(Model model) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity entity = new HttpEntity(headers);
        CreatePaymentResponse response = restTemplate.exchange(url, HttpMethod.POST ,entity, CreatePaymentResponse.class).getBody();
        System.out.println("next url "+response.getLinks().getNextUrl().getHref());
        System.out.println("payment id "+response.getPaymentId());
        model.addAttribute("paymentId",response.getPaymentId());
        model.addAttribute("nextUrl",response.getLinks().getNextUrl().getHref());
        return "payment-display";
    }

}
