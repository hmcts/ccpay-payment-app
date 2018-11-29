package uk.gov.hmcts.payment.functional.s2s;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface S2sApi {

    @RequestLine("POST /lease")
    @Headers({"Content-Type: application/json", "Accept: text/plain"})
    @Body("%7B\n" +
        "  \"microservice\": \"{service_name}\",\n" +
        "  \"oneTimePassword\": \"{otp}\"\n" +
        "%7D")
    String serviceToken(@Param("service_name") String serviceName,
                        @Param("otp") String otp);
}
