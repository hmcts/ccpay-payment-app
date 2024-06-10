package uk.gov.hmcts.payment.api.scheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Collectors;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.payment.api.dto.DuplicatePaymentDto;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.email.Email;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.reports.config.CardPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.DuplicatePaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PaymentReportConfig;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.model.Payment2Repository;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class PaymentsReportServiceTest {

    @InjectMocks //@InjectMocks is the class under test @Mocks
    private PaymentsReportService paymentsReportService;

    @Mock
    private PaymentsReportService paymentsReportService2;

    @Mock
    private PaymentService paymentService;

    @Mock
    private DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;
    @Mock
    private EmailService emailService;
    @Mock
    private FeesService feesService;
    @Mock
    private PaymentDtoMapper paymentDtoMapper;

    private PaymentReportConfig paymentReportConfig;

    @Mock
    private DuplicatePaymentDto duplicatePayments ;
    @Mock
    private  Payment2Repository paymentRepository = null;

    @Mock
    private Tuple duplicatePaymentsRecord;

    public PaymentsReportServiceTest() {
    }

    @Before
    public void setUp() {
        paymentReportConfig = new CardPaymentReportConfig("fromEmail", new String[]{"toEmail"}, "emailSubject", "emailMessage", true);
    }

    @After
    public void tearDown() {
        paymentReportConfig = null;
    }

    @Test
    public void shouldDelegateToCardSearch() {
        // given
        Date startDate = new Date();
        Date endDate = new Date();

        PaymentSearchCriteria criteria = PaymentSearchCriteria
            .searchCriteriaWith()
            .startDate(startDate)
            .endDate(endDate)
            .paymentMethod(PaymentMethodType.CARD.getType())
            .build();

        // when
        paymentsReportService.generateCsvAndSendEmail(startDate, endDate, PaymentMethodType.CARD, null, paymentReportConfig);

        // then
        verify(delegatingPaymentService).search(criteria);
    }

    @Test
    public void shouldDelegateToPbaSearch() {
        // given
        Date startDate = new Date();
        Date endDate = new Date();

        PaymentSearchCriteria criteria = PaymentSearchCriteria
            .searchCriteriaWith()
            .startDate(startDate)
            .endDate(endDate)
            .paymentMethod(PaymentMethodType.PBA.getType())
            .serviceType("Divorce")
            .build();

        // when
        paymentsReportService.generateCsvAndSendEmail(startDate, endDate, PaymentMethodType.PBA, "Divorce", paymentReportConfig);

        // then
        verify(delegatingPaymentService).search(criteria);
    }

    @Test
    public void shouldDelegateToEmailService() {
        // given
        paymentReportConfig = new CardPaymentReportConfig("fromEmail", new String[]{"toEmail"}, "emailSubject", "emailMessage", true);

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
    @Test
    public void shouldGenerateDuplicatePaymentReportCsvAndSendEmail() {
        // given
        paymentReportConfig = new DuplicatePaymentReportConfig("fromEmail", new String[]{"toEmail"}, "emailSubject", "emailMessage", true);
        when(duplicatePaymentsRecord.get(0, Date.class)).thenReturn(new Date());
        when(duplicatePaymentsRecord.get(1, String.class)).thenReturn("Description");
        when(duplicatePaymentsRecord.get(2, String.class)).thenReturn("Account");
        when(duplicatePaymentsRecord.get(3, BigDecimal.class)).thenReturn(new BigDecimal("100.00"));
        when(duplicatePaymentsRecord.get(4, String.class)).thenReturn("Currency");
        when(duplicatePaymentsRecord.get(5, String.class)).thenReturn("Reference");
        when(duplicatePaymentsRecord.get(6, Integer.class)).thenReturn(2);
        when(duplicatePaymentsRecord.get(7, BigInteger.class)).thenReturn(BigInteger.valueOf(12345));
        List<Tuple> findDuplicatePaymentsByDate = new ArrayList<>();
        findDuplicatePaymentsByDate.add(duplicatePaymentsRecord);
        when(paymentRepository.findDuplicatePaymentsByDate(any(), any())).thenReturn(findDuplicatePaymentsByDate);

        // when
        paymentsReportService.generateDuplicatePaymentsCsvAndSendEmail(new Date(), new Date(), paymentReportConfig);

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

    @Test
    public void shouldntGenerateDuplicatePaymentReportAsNoDuplicatesWereFound() {
        // given
        paymentReportConfig = new DuplicatePaymentReportConfig("fromEmail", new String[]{"toEmail"}, "emailSubject", "emailMessage", true);
        List<Tuple> findDuplicatePaymentsByDate = new ArrayList<>();
        when(paymentRepository.findDuplicatePaymentsByDate(any(), any())).thenReturn(findDuplicatePaymentsByDate);

        // when
        paymentsReportService.generateDuplicatePaymentsCsvAndSendEmail(new Date(), new Date(), paymentReportConfig);

        // then
        verify(emailService, never()).sendEmail(any());
    }

}
