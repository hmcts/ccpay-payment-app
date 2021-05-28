package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.dto.MigratingDataDto;

import java.util.List;

public interface MigrationService {
    List<MigratingDataDto> findMigrationDataByPaymentLinkIdAndDateCreatedForMultiRecords();

    int updatePaymentLinkWithSinglePaymentRecords();

    String updatePaymentFeeLinkWithMigratingData(MigratingDataDto migratingData);
}
