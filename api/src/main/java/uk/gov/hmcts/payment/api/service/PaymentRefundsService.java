package uk.gov.hmcts.payment.api.service;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.dto.*;

public interface PaymentRefundsService {

    ResponseEntity<RefundResponse> createRefund(PaymentRefundRequest paymentRefundRequest, MultiValueMap<String, String> headers);

    ResponseEntity<RefundResponse> createAndValidateRetrospectiveRemissionRequest(
            RetrospectiveRemissionRequest retrospectiveRemissionRequest, MultiValueMap<String, String> headers);

    ResponseEntity updateTheRemissionAmount(String paymentReference, ResubmitRefundRemissionRequest request);

    PaymentGroupResponse checkRefundAgainstRemission(MultiValueMap<String, String> headers, PaymentGroupResponse paymentGroupResponse, String ccdCaseNumber);

    PaymentGroupDto checkRefundAgainstRemissionFeeApportion(MultiValueMap<String, String> headers, PaymentGroupDto paymentGroupDto, String paymentRef);
    PaymentGroupDto setOverpaymnt(MultiValueMap<String, String> headers, PaymentGroupDto paymentGroupDto, String paymentRef);

}
