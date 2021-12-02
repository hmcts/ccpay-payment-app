package uk.gov.hmcts.payment.api.service;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.controllers.CardPaymentController;
import uk.gov.hmcts.payment.api.domain.model.Roles;
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

    Roles roles = new Roles();

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
        if(!roles.getRoles().isEmpty()){
            isRoles = checkRoles(roles.getRoles());
        }
        boolean refundEnableFeature = featureToggler.getBooleanValue("refund-remission-feature",false);

        LOG.info("RefundEnableFeature Flag Value in RefundRemissionEnableServiceImpl : {}", refundEnableFeature);

        if(refundEnableFeature){
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

        if(!roles.getRoles().isEmpty()){
            isRoles = checkRoles(roles.getRoles());
        }

        Optional<Remission> remission = remissionRepository.findByFeeId(fee.getId());
        if(remission.isPresent()) {
            isRemission=true;
        }

        boolean refundEnableFeature = featureToggler.getBooleanValue("refund-remission-feature",false);

        LOG.info("RefundEnableFeature Flag Value in RefundRemissionEnableServiceImpl : {}", refundEnableFeature);

        if(refundEnableFeature){
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

    public Roles getRoles(MultiValueMap<String, String> headers) {

        if (!headers.isEmpty()) {
            IdamUserIdResponse uid = idamService.getUserId(headers);
            roles.setRoles(uid.getRoles());
        }
        return roles;
    }

    private Boolean checkRoles(List<String> roles) {
            return roles.contains(AUTHORISED_REFUNDS_ROLE) || roles.contains(AUTHORISED_REFUNDS_APPROVER_ROLE);
    }
}




