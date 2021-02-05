package uk.gov.hmcts.payment.api.componenttests.mapper;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.mapper.BulkScanningReportMapper;
import uk.gov.hmcts.payment.api.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BulkScanningReportMapperTest {

    @Autowired
    private BulkScanningReportMapper bulkScanningReportMapper;

    @Before
    public void beforeEach() {
        List<Payment> payments = new ArrayList<Payment>();
        Integer[] num = new Integer[5];
        for(Integer number:num){
            Payment payment = Payment.paymentWith()
                .amount(new BigDecimal("99.99"))
                .caseReference("Reference" + number)
                .ccdCaseNumber("ccdCaseNumber" + number)
                .description("Test payments statuses for " + number)
                .serviceType(Service.DIVORCE.getName())
                .s2sServiceName("ccd_gw")
                .currency("GBP")
                .siteId("AA0" + number)
                .userId("USER_ID")
                .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
                .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
                .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
                .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
                .externalReference("e2kkddts5215h9qqoeuth5c0v"+)
                .reference("RC-1519-9028-2432-000" + number)
                .statusHistories(Arrays.asList(statusHistory))
                .build();
        }


    }

    public void testToBulkScanningUnallocatedReportDto(){


    }
}
