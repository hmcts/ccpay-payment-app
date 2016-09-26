package uk.gov.justice.payment.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.api.CreatePaymentResponse;
import uk.gov.justice.payment.api.json.external.GDSViewPaymentResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;


@Controller
public class PaymentController {
    //private static Map<String,String> tempStorage = new HashMap<>();


    @Value("${url}")
    private String url;

    @RequestMapping("/index.html")
    String home() {

        return "index";
    }
    @RequestMapping( value="/payment-intiate" , method = RequestMethod.POST )
    String intiatePayment(@RequestParam("amount") Double amount,
                          @RequestParam("description") String description,
                          @RequestParam("reference") String reference,
                          HttpServletResponse httpServletResponse,
                          Model model) {
        System.out.println("amount="+amount);
        amount*=100;

        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(amount.intValue());
        paymentRequest.setDescription(description);
        paymentRequest.setPaymentReference(reference);
        paymentRequest.setReturnUrl("https://localhost:8443/payment-result");
        paymentRequest.setApplicationReference("TEST_SERVICE");




        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity entity = new HttpEntity(paymentRequest, headers);
        CreatePaymentResponse paymentResponse = restTemplate.exchange(url, HttpMethod.POST ,entity, CreatePaymentResponse.class).getBody();
        System.out.println("next url "+paymentResponse.getLinks().getNextUrl().getHref());
        String paymentId = paymentResponse.getPaymentId();
        System.out.println("payment id "+paymentId);
        model.addAttribute("paymentId",paymentId);
        model.addAttribute("nextUrl",paymentResponse.getLinks().getNextUrl().getHref());
        //tempStorage.put(reference,paymentId);
        Cookie  cookie = new Cookie("paymentId",paymentId);
        cookie.setMaxAge(60*60*24);
        cookie.setSecure(true);
        httpServletResponse.addCookie(cookie);
        //return "payment-display";
        return "redirect:"+paymentResponse.getLinks().getNextUrl().getHref();
    }



    @RequestMapping("/payment-result")
    String viewPaymentResult(@CookieValue("paymentId") String paymentId,
                                Model model) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity entity = new HttpEntity(headers);
        GDSViewPaymentResponse response = restTemplate.exchange(url+"/"+paymentId, HttpMethod.GET , entity, GDSViewPaymentResponse.class).getBody();
        model.addAttribute("paymentStatus",response.getState().getStatus());
        model.addAttribute("isFinished",response.getState().getFinished());
        model.addAttribute("paymentId",paymentId);
        return "payment-result";
    }
}
