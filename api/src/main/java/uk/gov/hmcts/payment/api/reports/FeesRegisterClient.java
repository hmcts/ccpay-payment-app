package uk.gov.hmcts.payment.api.reports;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@FeignClient(name = "fees-register-client", url = "${fees.register.url}")
public interface FeesRegisterClient {
    @GetMapping(value = "/fees-register/fees")
    List<Fee2Dto> getFeesData();

    default Optional<Map<String,Fee2Dto>> getFeesDataAsMap() {
        return Optional.ofNullable(getFeesData().stream().collect(Collectors.toMap(fee->fee.getCode(),fee ->fee)));
        }

}
