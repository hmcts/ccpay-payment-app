package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.util.LogSanitizer;

import java.util.List;

@Service
@ConditionalOnProperty(name = "bar.datasource.enabled", havingValue = "true")
public class BarPaymentDeletionServiceImpl implements BarPaymentDeletionService {

    private static final Logger LOG = LoggerFactory.getLogger(BarPaymentDeletionServiceImpl.class);

    private final JdbcTemplate barJdbcTemplate;

    @Autowired
    public BarPaymentDeletionServiceImpl(@Qualifier("barJdbcTemplate") JdbcTemplate barJdbcTemplate) {
        this.barJdbcTemplate = barJdbcTemplate;
    }

    @Override
    @Transactional(transactionManager = "barTransactionManager")
    public void deleteBarPaymentsByCcdCaseNumber(String ccdCaseNumber) {
        LOG.info("Deleting Digital BAR payment data for case reference: {}", LogSanitizer.sanitize(ccdCaseNumber));

        List<Integer> paymentInstructionIds = barJdbcTemplate.queryForList(
            "SELECT DISTINCT payment_instruction_id FROM case_fee_detail WHERE case_reference = ?",
            Integer.class,
            ccdCaseNumber
        );

        if (paymentInstructionIds.isEmpty()) {
            LOG.info("No Digital BAR payment instructions found for case reference: {}",
                LogSanitizer.sanitize(ccdCaseNumber));
            return;
        }

        barJdbcTemplate.update(
            "DELETE FROM payment_instruction_status WHERE payment_instruction_id IN "
                + "(SELECT payment_instruction_id FROM case_fee_detail WHERE case_reference = ?)",
            ccdCaseNumber
        );

        barJdbcTemplate.update(
            "DELETE FROM payment_instruction_payhub_reference WHERE payment_instruction_id IN "
                + "(SELECT payment_instruction_id FROM case_fee_detail WHERE case_reference = ?)",
            ccdCaseNumber
        );

        barJdbcTemplate.update(
            "DELETE FROM case_fee_detail WHERE case_reference = ?",
            ccdCaseNumber
        );

        for (Integer paymentInstructionId : paymentInstructionIds) {
            barJdbcTemplate.update(
                "DELETE FROM payment_instruction WHERE id = ?",
                paymentInstructionId
            );
        }

        LOG.info(
            "Completed Digital BAR payment deletion for case reference: {} ({} payment instruction(s))",
            LogSanitizer.sanitize(ccdCaseNumber),
            paymentInstructionIds.size()
        );
    }
}
