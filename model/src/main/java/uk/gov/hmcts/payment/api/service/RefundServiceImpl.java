package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.RefundReferenceDto;
import uk.gov.hmcts.payment.api.dto.RefundRequest;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NonPBAPaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;

@Service
public class RefundServiceImpl implements RefundService {

    private static final Logger LOG = LoggerFactory.getLogger(RefundServiceImpl.class);

    @Autowired
    RemissionRepository remissionRepository;

    @Autowired
    Payment2Repository paymentRepository;

    @Autowired
    FeePayApportionRepository feePayApportionRepository;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired()
    @Qualifier("restTemplateRefundGroup")
    private RestTemplate restTemplateRefundGroup;

    @Value("${refund.api.url}")
    private String refundApiUrl;

    final Predicate<Payment> paymentSuccessCheck =
        payment -> payment.getPaymentStatus().getName().equals(PaymentStatus.SUCCESS.getName());

    final Predicate<Payment> checkIfPaymentIsPBA = payment -> payment.getPaymentMethod()
        .getName().equalsIgnoreCase(PaymentMethodType.PBA.getType());

    @Override
    public RefundRequest createAndValidateRetroSpectiveRemissionRequest(String remissionReference) {
        Optional<Remission> remission = remissionRepository.findByRemissionReference(remissionReference);
        PaymentFee paymentFee;
        Integer paymentId;

        if (remission.isPresent()) {
             //remissionAmount

            paymentFee = remission.get().getFee();
            //need to validate if multipleApportionment scenario present for single feeId validation needed
            Optional<FeePayApportion> feePayApportion = feePayApportionRepository.findByFeeId(paymentFee.getId());


            if (feePayApportion.isPresent()) {
                paymentId = feePayApportion.get().getPaymentId();

                Optional<Payment> payment = Optional
                    .ofNullable(paymentRepository
                        .findById(paymentId).orElseThrow(() -> new PaymentNotFoundException("Payment not found for given apportionment")));

                BigDecimal remissionAmount = remission.get().getHwfAmount();
                String paymentReference = validateThePaymentBeforeInitiatingRefund(payment);

                 return RefundRequest.refundRequestWith()
                    .paymentReference(paymentReference) //RC reference
                    .refundAmount(remissionAmount) //Refund amount
                    .refundReason("RR004")  //Refund reason category would be other
                    .build();
            }

        } else {
            throw new RemissionNotFoundException("Remission not found for given remission reference");
        }

        return null;
    }

    private String validateThePaymentBeforeInitiatingRefund(Optional<Payment> payment) {
        if (payment.isPresent()) {
            //payment success check
            if (!paymentSuccessCheck.test(payment.get())) {
                throw new PaymentNotSuccessException("Refund can be possible if payment is successful");
            }
            //payment should be PBA check
            if (!checkIfPaymentIsPBA.test(payment.get())) {
                throw new NonPBAPaymentException("Refund currently supported for PBA Payment Channel only");
            }

            return payment.get().getReference();
        }
        return null;
    }

    @Override
    public ResponseEntity<RefundReferenceDto> createRefundRequestForRetroRemission(MultiValueMap<String, String> headersMap, RefundRequest refundRequest) throws RestClientException {
        //Generate token for payment api and replace
        List<String> serviceAuthTokenPaymentList = new ArrayList<>();
        serviceAuthTokenPaymentList.add(authTokenGenerator.generate());

        MultiValueMap<String, String> headerMultiValueMapForBulkScan = new LinkedMultiValueMap<>();
        headerMultiValueMapForBulkScan.put("content-type", headersMap.get("content-type"));
        //User token
        headerMultiValueMapForBulkScan.put("Authorization", headersMap.get("authorization"));
        //Service token
        headerMultiValueMapForBulkScan.put("ServiceAuthorization", serviceAuthTokenPaymentList);

        HttpHeaders headers = new HttpHeaders(headerMultiValueMapForBulkScan);
        final HttpEntity<String> entity = new HttpEntity(refundRequest, headers);


        LOG.info("Calling refund api to create refund for retrospective remission");
        return restTemplateRefundGroup.exchange(refundApiUrl + "/refund", HttpMethod.POST, entity, RefundReferenceDto.class);
    }
}
