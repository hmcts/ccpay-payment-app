package uk.gov.hmcts.payment.api.service;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.dto.*;

public interface PaymentRefundsService {

    ResponseEntity<RefundResponse> createRefund(PaymentRefundRequest paymentRefundRequest, MultiValueMap<String, String> headers);

    ResponseEntity<RefundResponse> createAndValidateRetrospectiveRemissionRequest(
            RetrospectiveRemissionRequest retrospectiveRemissionRequest, MultiValueMap<String, String> headers);

    ResponseEntity updateTheRemissionAmount(String paymentReference, ResubmitRefundRemissionRequest request);

    PaymentGroupResponse checkRefundAgainstRemissionV2(MultiValueMap<String, String> headers, PaymentGroupResponse paymentGroupResponse, String ccdCaseNumber);

    PaymentGroupDto checkRefundAgainstRemissionFeeApportionV2(MultiValueMap<String, String> headers, PaymentGroupDto paymentGroupDto, String paymentRef);


    void deleteByRefundReference(String refundReference, MultiValueMap<String, String> headers);

    RefundListDtoResponse getRefundsApprovedFromRefundService(String ccdCaseNumber, MultiValueMap<String, String> headers);
}
