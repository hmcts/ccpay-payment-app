package uk.gov.hmcts.payment.api.scheduler;

import org.joda.time.MutableDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.dto.mapper.CardPaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.email.CardPaymentReconciliationReportEmail;
import uk.gov.hmcts.payment.api.email.CreditAccountReconciliationReportEmail;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.service.CardPaymentService;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;

import java.text.ParseException;
import java.util.Date;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PaymentsReportServiceTest {


    private PaymentsReportService paymentsReportService;

    @Mock
    private CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    @Mock
    private CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService;

    @Mock
    private EmailService emailService;

    @Mock
    private FeesService feesService;

    @Mock
    private CardPaymentDtoMapper cardPaymentDtoMapper;

    @Mock
    private CreditAccountDtoMapper creditAccountDtoMapper;

    @Mock
    private CardPaymentReconciliationReportEmail cardPaymentReconciliationReportEmail;

    @Mock
    private CreditAccountReconciliationReportEmail creditAccountReconciliationReportEmail;


    private Date startDate;
    private Date endDate;

    @Before
    public void setUp() throws ParseException {

        startDate = getYesterdaysDate();
        endDate = getTodaysDate();

        paymentsReportService = new PaymentsReportService(cardPaymentService,cardPaymentDtoMapper,
            emailService,feesService,cardPaymentReconciliationReportEmail,creditAccountReconciliationReportEmail);

    }

    @Test
    public void shouldGenerateCardPaymentsCsvAndSendEmail()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(startDate, endDate, "cmc");

        // then
        verify(cardPaymentService).search(startDate,endDate, "card", "cmc", null);
        verify(emailService).sendEmail(cardPaymentReconciliationReportEmail);


    }

    @Test
    public void shouldGenerateCreditAccountPaymentsCsvAndSendEmail()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(startDate,endDate, "cmc");

        // then
        verify(cardPaymentService).search(startDate,endDate, "payment by account", "cmc", null);
        verify(emailService).sendEmail(creditAccountReconciliationReportEmail);


    }

    private Date getYesterdaysDate() {
        Date now = new Date();
        MutableDateTime mtDtNow = new MutableDateTime(now);
        mtDtNow.addDays(-1);
        return mtDtNow.toDate();
    }

    private Date getTodaysDate() {
        return new Date();
    }

}
