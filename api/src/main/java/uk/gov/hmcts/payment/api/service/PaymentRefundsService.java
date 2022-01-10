package uk.gov.hmcts.payment.api.service;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.dto.ResubmitRefundRemissionRequest;
import uk.gov.hmcts.payment.api.dto.RetroSpectiveRemissionRequest;

public interface PaymentRefundsService {

    ResponseEntity<RefundResponse> createRefund(PaymentRefundRequest paymentRefundRequest, MultiValueMap<String, String> headers);

    ResponseEntity<RefundResponse> createAndValidateRetroSpectiveRemissionRequest(
            RetroSpectiveRemissionRequest retroSpectiveRemissionRequest, MultiValueMap<String, String> headers);

    ResponseEntity updateTheRemissionAmount(String paymentReference, ResubmitRefundRemissionRequest request);

}
