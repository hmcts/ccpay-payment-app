package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.payment.api.dto.MigratingDataDto;
import uk.gov.hmcts.payment.api.dto.Reference;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface Payment2Repository extends CrudRepository<Payment, Integer>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByReferenceAndPaymentMethod(String reference, PaymentMethod paymentMethod);

    Optional<Payment> findByReferenceAndPaymentProvider(String reference, PaymentProvider paymentProvider);

    Optional<Payment> findByReference(String reference);

    List<Reference> findReferencesByPaymentProviderAndPaymentStatusNotInAndDateCreatedLessThan(
        PaymentProvider paymentProvider, List<PaymentStatus> paymentStatuses, Date targetTime);

    Optional<List<Payment>> findAllByDateCreatedBetween(Date fromDate, Date toDate);

    Optional<List<Payment>> findByDocumentControlNumber(String documentControlNumber);

    Optional<List<Payment>> findByCcdCaseNumber(String ccdCaseNumber);

    Optional<List<Payment>> findByPaymentLinkId(Integer id);


    @Query("SELECT new uk.gov.hmcts.payment.api.dto.MigratingDataDto(p.paymentLink.id, p.ccdCaseNumber, p.caseReference, p.siteId, p.serviceType) from Payment p " +
        "WHERE (p.paymentLink.id,p.dateCreated ) in (" +
        "\tSELECT p.paymentLink.id, MAX(p.dateCreated) from Payment p GROUP BY p.paymentLink.id HAVING count(*)>1)")
    List<MigratingDataDto> findMigrationDataByPaymentLinkIdAndDateCreatedForMultiRecords();


    @Modifying
    @Query(value = "" +
        "WITH audit_record as ( UPDATE payment_fee_link as pfl SET ccd_case_number = p.ccd_case_number, case_reference = p.case_reference, org_id = p.site_id, enterprise_service_name = p.service_type FROM payment p WHERE pfl.id = p.payment_link_id AND p.payment_link_id  IN ( SELECT p1.payment_link_id FROM payment p1 GROUP BY p1.payment_link_id HAVING count( p1.id ) = 1 )" +
        " RETURNING pfl.ccd_case_number AS ccd_case_no," +
        "'order_updated' AS audit_type," +
        "CONCAT('PaymentFeeLink( id =', pfl.id , ', caseReference=', pfl.case_reference, ', ccdCaseNumber=' , pfl.ccd_case_number, ', orgId=' , pfl.org_id, ', enterpriseServiceName=', pfl.enterprise_service_name, ')') as audit_payload," +
        "'Order Updated successfully' AS audit_description," +
        "current_date AS date_created," +
        "current_date AS date_updated )" +
        "insert" +
        "       into" +
        "       payment_audit_history (ccd_case_no," +
        "       audit_type," +
        "       audit_payload," +
        "       audit_description," +
        "       date_created," +
        "       date_updated) ((" +
        "       select" +
        "              ccd_case_no," +
        "              audit_type," +
        "              audit_payload," +
        "              audit_description," +
        "              date_created," +
        "              date_updated" +
        "       from" +
        "              audit_record));",nativeQuery = true)
    int updatePaymentLinkWithSinglePaymentRecords();

}
