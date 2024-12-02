package uk.gov.hmcts.payment.api.controllers.mock;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import uk.gov.hmcts.payment.api.contract.PaymentDto;

import java.util.List;

@RestController
@Profile("callbackMock")
@RequestMapping("/mock-api")
public class MockCallbackControllerForTesting {

    private static final Logger LOG = LoggerFactory.getLogger(MockCallbackControllerForTesting.class);

    private List<String> callbackList = Lists.newArrayList();

    @PutMapping("/serviceCallback")
    public ResponseEntity mockCallback(@RequestBody PaymentDto paymentDto) {
        LOG.info("Callback request:{}", paymentDto);
        callbackList.add(paymentDto.getReference());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/serviceCallback/{reference}")
    public ResponseEntity exists(@PathVariable("reference") String reference) {
        if (callbackList.contains(reference)) {
            callbackList.remove(reference);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
