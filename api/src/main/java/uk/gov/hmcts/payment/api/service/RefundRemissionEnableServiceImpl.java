package uk.gov.hmcts.payment.api.service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.dto.PaymentGroupResponse;
import uk.gov.hmcts.payment.api.dto.RefundListDtoResponse;
import uk.gov.hmcts.payment.api.dto.idam.IdamUserIdResponse;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.RemissionRepository;
import uk.gov.hmcts.payment.api.util.RefundEligibilityUtil;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

@Service
public class RefundRemissionEnableServiceImpl implements RefundRemissionEnableService {

    private final String STATUS = "success";
    private static final String AUTHORISED_REFUNDS_ROLE = "payments-refund";
    private static final String AUTHORISED_REFUNDS_APPROVER_ROLE = "payments-refund-approver";
    private static final Logger LOG = LoggerFactory.getLogger(RefundRemissionEnableServiceImpl.class);
    private static final String REFUND_ENDPOINT = "/refund";

    @Autowired
    private RefundEligibilityUtil refundEligibilityUtil;
    @Autowired
    private PaymentService<PaymentFeeLink, String> paymentService;
    @Autowired
    private FeePayApportionRepository FeePayApportionRepository;
    @Autowired
    private IdamService idamService;
    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;
    @Autowired
    RemissionRepository remissionRepository;
    @Value("${refund.api.url}")
    private String refundApiUrl;
    @Autowired()
    @Qualifier("restTemplateRefundsGroup")
    private RestTemplate restTemplateRefundsGroup;
    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    boolean isRoles=false;

    public Boolean returnRefundEligible(Payment payment) {

        boolean refundEligibleDate;
        boolean refundLagTimeFeature = featureToggler.getBooleanValue("refund-remission-lagtime-feature",false);

        LOG.info("RefundEnableFeature Flag Value in RefundRemissionEnableServiceImpl : {}", refundLagTimeFeature);

        if(refundLagTimeFeature){
            refundEligibleDate = calculateLagDate(payment);
            return payment.getPaymentStatus().getName().equalsIgnoreCase(STATUS) && refundEligibleDate
                && isRoles;
        }
        else{
            return payment.getPaymentStatus().getName().equalsIgnoreCase(STATUS) && isRoles;
        }

    }
    private Boolean calculateLagDate(Payment payment) {

        long timeDuration= ChronoUnit.HOURS.between( payment.getDateUpdated().toInstant(), new Date().toInstant());
        return refundEligibilityUtil.getRefundEligiblityStatus(payment,timeDuration);
    }

    public Boolean returnRemissionEligible(PaymentFee fee) {

        Boolean remissionEligible=false;
        boolean isRemission=false;

        Optional<Remission> remission = remissionRepository.findByFeeId(fee.getId());
        if(remission.isPresent()) {
            isRemission=true;
        }

        boolean remissionLagTimeFeature = featureToggler.getBooleanValue("refund-remission-lagtime-feature",false);

        LOG.info("RefundEnableFeature Flag Value in RefundRemissionEnableServiceImpl : {}", remissionLagTimeFeature);

        if(remissionLagTimeFeature){
            Optional<FeePayApportion> FeePayApportion = FeePayApportionRepository.findByFeeId(
                fee.getId());

            if (FeePayApportion.isPresent()) {
                Payment payment = paymentService.getPaymentById(FeePayApportion.get().getPaymentId());
                remissionEligible = calculateLagDate(payment);
            }

            return !isRemission && remissionEligible && isRoles;
        }
        else{
            return !isRemission && isRoles;
        }

    }

    public boolean isRolePresent(MultiValueMap<String, String> headers) {

        if (!headers.isEmpty()) {
            IdamUserIdResponse uid = idamService.getUserId(headers);
            if(!uid.getRoles().isEmpty()){
                isRoles=uid.getRoles().contains(AUTHORISED_REFUNDS_ROLE) || uid.getRoles().contains(AUTHORISED_REFUNDS_APPROVER_ROLE);
            }
        }
        return isRoles;
    }

    @Override
    public PaymentGroupResponse checkRefundAgainstRemission(MultiValueMap<String, String> headers,
                                                            PaymentGroupResponse paymentGroupResponse, String ccdCaseNumber) {

        RefundListDtoResponse refundListDtoResponse = getRefundsFromRefundService(ccdCaseNumber, headers);

        var lambdaContext = new Object() {
            BigDecimal refundAmount = BigDecimal.ZERO;
        };

        paymentGroupResponse.getPaymentGroups().forEach(paymentGroup ->{

            paymentGroup.getRemissions().forEach(remission -> {

                remission.setAddRefund(true);

                remission.setIssueRefund(false);

                refundListDtoResponse.getRefundList().forEach(refundDto -> {

                    if (refundDto.getRefundReference()!=null && !refundDto.getRefundReference().isBlank()){

                        remission.setAddRefund(false);

                        remission.setIssueRefund(true);

                    }
                });
            });

            paymentGroup.getPayments().forEach(paymentDto -> {

                refundListDtoResponse.getRefundList().forEach(refundDto -> {

                    lambdaContext.refundAmount = lambdaContext.refundAmount.add(refundDto.getAmount());

                });

                if(paymentDto.getAmount().subtract(lambdaContext.refundAmount).compareTo(BigDecimal.ZERO)==1)
                    paymentDto.setIssueRefundAddRefundAddRemission(true);

                else
                    paymentDto.setIssueRefundAddRefundAddRemission(false);

            });
        });

        return paymentGroupResponse;
    }

    private RefundListDtoResponse getRefundsFromRefundService(String ccdCaseNumber, MultiValueMap<String, String> headers) {


        RefundListDtoResponse refundListDtoResponse;

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(refundApiUrl + REFUND_ENDPOINT).queryParam("ccdCaseNumber",ccdCaseNumber);

        LOG.debug("builder.toUriString() : {}", builder.toUriString());

        try {

            ResponseEntity<RefundListDtoResponse> refundListDtoResponseEntity  = restTemplateRefundsGroup
                .exchange(builder.toUriString(), HttpMethod.GET, createEntity(headers), RefundListDtoResponse.class);

            refundListDtoResponse = refundListDtoResponseEntity.hasBody() ? refundListDtoResponseEntity.getBody() : null;

        } catch (HttpClientErrorException e) {

            LOG.error("client err ", e);

            throw new InvalidRefundRequestException(e.getResponseBodyAsString());

        }

        return refundListDtoResponse;

    }

    private HttpEntity<HttpHeaders> createEntity(MultiValueMap<String, String> headers) {

        MultiValueMap<String, String> headerMultiValueMap = new LinkedMultiValueMap<String, String>();

        String serviceAuthorisation = authTokenGenerator.generate();

        headerMultiValueMap.put("Content-Type", headers.get("content-type"));

        String userAuthorization = headers.get("authorization") != null ? headers.get("authorization").get(0) : headers.get("Authorization").get(0);

        headerMultiValueMap.put("Authorization", Collections.singletonList(userAuthorization.startsWith("Bearer ")
            ? userAuthorization : "Bearer ".concat(userAuthorization)));

        headerMultiValueMap.put("ServiceAuthorization", Collections.singletonList(serviceAuthorisation));

        HttpHeaders httpHeaders = new HttpHeaders(headerMultiValueMap);

        return new HttpEntity<>(httpHeaders);
    }

}




