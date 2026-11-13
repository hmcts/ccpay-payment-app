package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.model.PaymentAuditDeletion;
import uk.gov.hmcts.payment.api.model.PaymentAuditDeletionRepository;

import java.util.List;

@Service
public class PaymentDeletionAuditServiceImpl implements PaymentDeletionAuditService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentDeletionAuditServiceImpl.class);
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final PaymentAuditDeletionRepository paymentAuditDeletionRepository;

    @Autowired
    public PaymentDeletionAuditServiceImpl(PaymentAuditDeletionRepository paymentAuditDeletionRepository) {
        this.paymentAuditDeletionRepository = paymentAuditDeletionRepository;
    }

    @Override
    @Transactional
    public void recordDeletionAudits(List<PaymentDeletionAuditDetails> audits) {
        LOG.info("Recording {} payment deletion audit entr{}", audits.size(), audits.size() == 1 ? "y" : "ies");

        List<PaymentAuditDeletion> entities = audits.stream()
            .map(audit -> PaymentAuditDeletion.paymentAuditDeletionWith()
                .ccdCaseNumber(audit.ccdCaseNumber())
                .sourceSystem(audit.sourceSystem())
                .errorMessage(truncate(audit.errorMessage()))
                .success(audit.success())
                .build())
            .toList();

        paymentAuditDeletionRepository.saveAll(entities);
    }

    private String truncate(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() <= ERROR_MESSAGE_MAX_LENGTH
            ? errorMessage
            : errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
