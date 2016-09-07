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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.json.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.CreatePaymentResponse;
import uk.gov.justice.payment.api.json.ViewPaymentResponse;

import java.util.HashMap;
import java.util.Map;


@Controller
public class PaymentController {
    private static Map<String,String> tempStorage = new HashMap<>();


    @Value("${url}")
    private String url;

    @RequestMapping("/")
    String home() {

        return "index";
    }
    @RequestMapping( value="/payment-intiate" , method = RequestMethod.POST )
    String intiatePayment(@RequestParam("amount") Integer amount,
                          @RequestParam("description") String description,
                          @RequestParam("reference") String reference,
                          Model model) {
        System.out.println("amount="+amount);
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(amount);
        paymentRequest.setDescription(description);
        paymentRequest.setReference(reference);
        paymentRequest.setReturnUrl("https://localhost:8443/payment-result?reference="+reference);




        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity entity = new HttpEntity(paymentRequest, headers);
        CreatePaymentResponse response = restTemplate.exchange(url, HttpMethod.POST ,entity, CreatePaymentResponse.class).getBody();
        System.out.println("next url "+response.getLinks().getNextUrl().getHref());
        String paymentId = response.getPaymentId();
        System.out.println("payment id "+paymentId);
        model.addAttribute("paymentId",paymentId);
        model.addAttribute("nextUrl",response.getLinks().getNextUrl().getHref());
        tempStorage.put(reference,paymentId);
        return "payment-display";
    }

    @RequestMapping("/payment-result")
    String processPaymentResult(@RequestParam("reference") String reference,
                                Model model) {
        String paymentId = tempStorage.get(reference);

        //-------------------------
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity entity = new HttpEntity( headers);
        Map<String, String> params = new HashMap<String, String>();
        params.put("paymentId", paymentId);

        ViewPaymentResponse response = restTemplate.exchange(url, HttpMethod.GET ,entity, ViewPaymentResponse.class,params).getBody();
        //String response = restTemplate.exchange(url, HttpMethod.GET ,entity, String.class,params).getBody();
        //-------------------------



        model.addAttribute("paymentStatus",response.getState().getStatus());
        model.addAttribute("isFinished",response.getState().getFinished());
        model.addAttribute("reference",reference);
        model.addAttribute("paymentId",paymentId);
        return "payment-result";
    }

}
