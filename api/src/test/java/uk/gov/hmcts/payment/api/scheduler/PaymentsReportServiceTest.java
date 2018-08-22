package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.email.Email;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.reports.config.CardPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PaymentReportConfig;
import uk.gov.hmcts.payment.api.service.CardPaymentService;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.util.Date;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class PaymentsReportServiceTest {

    @InjectMocks
    private PaymentsReportService paymentsReportService;

    @Mock
    private CardPaymentService<PaymentFeeLink, String> cardPaymentService;
    @Mock
    private EmailService emailService;
    @Mock
    private FeesService feesService;
    @Mock
    private PaymentDtoMapper paymentDtoMapper;

    private PaymentReportConfig paymentReportConfig;

    @Before
    public void setUp() {
        paymentReportConfig = new CardPaymentReportConfig("fromEmail", new String []{"toEmail"}, "emailSubject", "emailMessage", true);
    }

    @Test
    public void shouldDelegateToCardSearch()  {
        // given
        Date startDate = new Date();
        Date endDate = new Date();

        // when
        paymentsReportService.generateCsvAndSendEmail(startDate, endDate, PaymentMethodType.CARD, null, paymentReportConfig);

        // then
        verify(cardPaymentService).search(startDate,endDate, "card", null, null);
    }

    @Test
    public void shouldDelegateToPbaSearch()  {
        // given
        Date startDate = new Date();
        Date endDate = new Date();

        // when
        paymentsReportService.generateCsvAndSendEmail(startDate, endDate, PaymentMethodType.PBA, Service.DIVORCE, paymentReportConfig);

        // then
        verify(cardPaymentService).search(startDate,endDate, "payment by account", "Divorce", null);
    }

    @Test
    public void shouldInvokeFreeRefresh()  {
        // given
        Date startDate = new Date();
        Date endDate = new Date();

        // when
        paymentsReportService.generateCsvAndSendEmail(startDate, endDate, PaymentMethodType.PBA, Service.DIVORCE, paymentReportConfig);

        // then
        verify(feesService).dailyRefreshOfFeesData();
    }

    @Test
    public void shouldDelegateToEmailService()  {
        // given
        paymentReportConfig = new CardPaymentReportConfig("fromEmail", new String []{"toEmail"}, "emailSubject", "emailMessage", true);

         // when
        paymentsReportService.generateCsvAndSendEmail(new Date(), new Date(), PaymentMethodType.CARD, null, paymentReportConfig);

        // then
        ArgumentCaptor<Email> argument = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(argument.capture());
        assertThat(argument.getValue().getFrom()).isEqualTo("fromEmail");
        assertThat(argument.getValue().getTo()).containsExactly("toEmail");
        assertThat(argument.getValue().getSubject()).isEqualTo("emailSubject");
        assertThat(argument.getValue().getMessage()).isEqualTo("emailMessage");
        assertThat(argument.getValue().hasAttachments()).isTrue();
        assertThat(argument.getValue().getAttachments().get(0).getFilename()).startsWith(paymentReportConfig.getCsvFileNamePrefix());
    }

}
