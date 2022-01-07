package uk.gov.hmcts.payment.api.service;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.dto.idam.IdamUserIdResponse;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.RemissionRepository;
import uk.gov.hmcts.payment.api.util.RefundEligibilityUtil;

@Service
public class RefundRemissionEnableServiceImpl implements RefundRemissionEnableService {

    private final String STATUS = "success";
    private static final String AUTHORISED_REFUNDS_ROLE = "payments-refund";
    private static final String AUTHORISED_REFUNDS_APPROVER_ROLE = "payments-refund-approver";
    private static final Logger LOG = LoggerFactory.getLogger(RefundRemissionEnableServiceImpl.class);

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

}




