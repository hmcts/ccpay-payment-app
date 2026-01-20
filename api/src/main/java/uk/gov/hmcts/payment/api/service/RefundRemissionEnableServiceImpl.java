package uk.gov.hmcts.payment.api.service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
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

    boolean isRoles=false;

    List<String> roles = null;

    public Boolean returnRefundEligible(Payment payment) {

        boolean refundEligibleDate;
        boolean refundLagTimeFeature = featureToggler.getBooleanValue("refund-remission-lagtime-feature",false);
        LOG.debug("RefundEnableFeature Flag Value in RefundRemissionEnableServiceImpl : {}", refundLagTimeFeature);

        if(refundLagTimeFeature){
            refundEligibleDate = calculateLagDate(payment);

            return payment.getPaymentStatus().getName().equalsIgnoreCase(STATUS) && refundEligibleDate
                && validateRefundRoleWithServiceName(payment.getPaymentLink().getEnterpriseServiceName());
        }
        else{
            return payment.getPaymentStatus().getName().equalsIgnoreCase(STATUS) && validateRefundRoleWithServiceName(payment.getPaymentLink().getEnterpriseServiceName());
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

        if(remissionLagTimeFeature){
            Optional<List<FeePayApportion>> feePayApportion = FeePayApportionRepository.findByFeeId(
                fee.getId());

            if (feePayApportion.isPresent()) {
                Optional<FeePayApportion> result = feePayApportion.get().stream().findFirst();
                if(result.isPresent()) {
                    Payment payment = paymentService.getPaymentById(result.get().getPaymentId());
                    remissionEligible = calculateLagDate(payment);
                }
            }

            return !isRemission && remissionEligible && validateRefundRoleWithServiceName(fee.getPaymentLink().getEnterpriseServiceName());
        }
        else{
            return !isRemission && validateRefundRoleWithServiceName(fee.getPaymentLink().getEnterpriseServiceName());
        }

    }

    public void setUserRoles(MultiValueMap<String, String> headers) {

        if (!headers.isEmpty()) {
            IdamUserIdResponse uid = idamService.getUserId(headers);
            if(!uid.getRoles().isEmpty()){
                roles = new ArrayList<String>();
                roles.addAll(uid.getRoles());
            }
        }
    }

    private boolean validateRefundRoleWithServiceName(String serviceName) {

        boolean isRefundRoleForService = true;
        String serviceNameRefundRole = AUTHORISED_REFUNDS_ROLE + "-" + serviceName.replace(" ","-")
            .toLowerCase();
        String serviceNameRefundApprovalRole = AUTHORISED_REFUNDS_APPROVER_ROLE + "-" + serviceName.replace(" ","-")
            .toLowerCase();
        List<String> refundServiceRoles = roles.stream().filter(role ->
                role.toLowerCase().contains(serviceNameRefundRole.toLowerCase())
                    || role.toLowerCase().contains(serviceNameRefundApprovalRole.toLowerCase()))
            .collect(Collectors.toList());

        LOG.info("Validate Refund Role With Service Name ---> roles {}, serviceName {}, refundServiceRoles {}", roles, serviceName, refundServiceRoles);
        if (refundServiceRoles == null || refundServiceRoles.isEmpty()) {
            isRefundRoleForService = false;
        }
        return isRefundRoleForService;
    }
}




