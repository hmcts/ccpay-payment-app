package uk.gov.hmcts.payment.api.controllers.mock;

import com.google.common.collect.Lists;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import uk.gov.hmcts.payment.api.contract.PaymentDto;

import java.util.List;

@RestController
@Profile("callbackMock")
@RequestMapping("/mock-api")
public class MockCallbackControllerForTesting {

    private List<String> callbackList = Lists.newArrayList();

    @PatchMapping("/serviceCallback")
    public ResponseEntity mockCallback(@RequestBody PaymentDto paymentDto) {
        callbackList.add(paymentDto.getReference());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/serviceCallback/{reference}")
    public ResponseEntity exists(@PathVariable("reference") String reference) {
        if (callbackList.contains(reference)) {
            callbackList.remove(reference);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
