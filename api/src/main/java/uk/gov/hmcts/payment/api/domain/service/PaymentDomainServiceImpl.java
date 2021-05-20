package uk.gov.hmcts.payment.api.domain.service;

import org.ff4j.FF4j;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;

import java.util.List;

@Service
public class PaymentDomainServiceImpl implements PaymentDomainService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentDomainServiceImpl.class);

    @Autowired
    Payment2Repository payment2Repository;

    private DateTimeFormatter formatter = new DateUtil().getIsoDateTimeFormatter();

    @Autowired
    private PaymentValidator validator;
    @Autowired
    private PaymentService<PaymentFeeLink, String> paymentService;
    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;
    @Autowired
    private PaymentDtoMapper paymentDtoMapper;
    @Autowired
    private FF4j ff4j;
    @Autowired
    private PaymentFeeRepository paymentFeeRepository;
    @Autowired
    private FeesService feesService;

    @Override
    public Payment getPaymentByApportionment(FeePayApportion feePayApportion) {
        return paymentService.getPaymentById(feePayApportion.getPaymentId());
    }

    @Override
    public Payment getPaymentByReference(String reference) {
        return paymentService.findSavedPayment(reference);
    }

    @Override
    public List<FeePayApportion> getFeePayApportionByPaymentId(Integer paymentId) {
        return paymentService.findByPaymentId(paymentId);

    }

}
