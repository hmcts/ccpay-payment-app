package uk.gov.hmcts.payment.api.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.util.LogSanitizer;

@Service
public class CasePaymentDeletionServiceImpl implements CasePaymentDeletionService {

    private static final Logger LOG = LoggerFactory.getLogger(CasePaymentDeletionServiceImpl.class);
    private static final String CCD_CASE_NUMBER_PARAM = "ccdCaseNumber";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void deletePaymentsByCcdCaseNumber(String ccdCaseNumber) {
        LOG.info("Deleting payment data for CCD case number: {}", LogSanitizer.sanitize(ccdCaseNumber));

        entityManager.createNativeQuery(
                "DELETE FROM status_history WHERE payment_id IN "
                    + "(SELECT id FROM payment WHERE ccd_case_number = :ccdCaseNumber)")
            .setParameter(CCD_CASE_NUMBER_PARAM, ccdCaseNumber)
            .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM telephony_callback WHERE payment_reference IN "
                    + "(SELECT reference FROM payment WHERE ccd_case_number = :ccdCaseNumber)")
            .setParameter(CCD_CASE_NUMBER_PARAM, ccdCaseNumber)
            .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM payment_audit_history WHERE ccd_case_no = :ccdCaseNumber")
            .setParameter(CCD_CASE_NUMBER_PARAM, ccdCaseNumber)
            .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM remission WHERE ccd_case_number = :ccdCaseNumber")
            .setParameter(CCD_CASE_NUMBER_PARAM, ccdCaseNumber)
            .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM payment_failures WHERE ccd_case_number = :ccdCaseNumber")
            .setParameter(CCD_CASE_NUMBER_PARAM, ccdCaseNumber)
            .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM fee_pay_apportion WHERE ccd_case_number = :ccdCaseNumber")
            .setParameter(CCD_CASE_NUMBER_PARAM, ccdCaseNumber)
            .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM fee WHERE ccd_case_number = :ccdCaseNumber")
            .setParameter(CCD_CASE_NUMBER_PARAM, ccdCaseNumber)
            .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM payment_allocation WHERE payment_id IN "
                    + "(SELECT id FROM payment WHERE ccd_case_number = :ccdCaseNumber)")
            .setParameter(CCD_CASE_NUMBER_PARAM, ccdCaseNumber)
            .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM payment WHERE ccd_case_number = :ccdCaseNumber")
            .setParameter(CCD_CASE_NUMBER_PARAM, ccdCaseNumber)
            .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM payment_fee_link WHERE ccd_case_number = :ccdCaseNumber")
            .setParameter(CCD_CASE_NUMBER_PARAM, ccdCaseNumber)
            .executeUpdate();

        LOG.info("Completed payment deletion for CCD case number: {}", LogSanitizer.sanitize(ccdCaseNumber));
    }
}
