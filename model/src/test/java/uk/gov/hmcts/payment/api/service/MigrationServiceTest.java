package uk.gov.hmcts.payment.api.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.dto.MigratingDataDto;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentGroupNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class MigrationServiceTest {

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @InjectMocks
    private MigrationServiceImpl migrationService;

    @Test
    public void testUpdatePaymentFeeLinkWithMigratingData(){
        MigratingDataDto migratingDataDto = MigratingDataDto.ccdLinkWith()
                                                .ccdCaseNumber("1234567890123456")
                                                .caseReference("case-reference")
                                                .siteId("AA07")
                                                .serviceType("Divorce")
                                                .build();
        PaymentFeeLink mockPaymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().build();
        when(paymentFeeLinkRepository.findById(anyInt())).thenReturn(java.util.Optional.ofNullable(mockPaymentFeeLink));
        when(paymentFeeLinkRepository.save(any(PaymentFeeLink.class))).thenReturn(mockPaymentFeeLink);
        String actualStatus = migrationService.updatePaymentFeeLinkWithMigratingData(migratingDataDto);
        assertEquals(actualStatus,"COMPLETE");
    }

    @Test
    public void testUpdatePaymentFeeLinkWithMigratingData_ThrowsException(){
        MigratingDataDto migratingDataDto = MigratingDataDto.ccdLinkWith()
            .ccdCaseNumber("1234567890123456")
            .caseReference("case-reference")
            .siteId("AA07")
            .serviceType("Divorce")
            .build();
        when(paymentFeeLinkRepository.findById(anyInt())).thenThrow(new PaymentGroupNotFoundException());
        String actualStatus = migrationService.updatePaymentFeeLinkWithMigratingData(migratingDataDto);
        assertEquals(actualStatus,"INCOMPLETE");
    }
}
