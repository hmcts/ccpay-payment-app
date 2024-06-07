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
        paymentReportConfig = new CardPaymentReportConfig("fromEmail", new String[]{"toEmail"}, "emailSubject", "emailMessage", true);

        // when

        List<DuplicatePaymentDto> duplicatePaymentsList = new ArrayList<>();

        paymentsReportService.generateDuplicatePaymentReportCsvAndSendEmail( duplicatePaymentsList,  paymentReportConfig);
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
    public void shouldGenerateDuplicatePaymentsCsvAndSendEmailisEmpty() {
        // Arrange
        Date startDate = new Date();
        Date endDate = new Date();

         int[] counter = new int[1]; // Initialize counter
            counter[0] = 0;


        List<DuplicatePaymentDto> duplicatePaymentsList = new ArrayList<>(); // Empty list to simulate no duplicates
        paymentReportConfig = new CardPaymentReportConfig("fromEmail", new String[]{"toEmail"}, "emailSubject", "emailMessage", true);


        // Mock the method findDuplicatePaymentsBy to return an empty list
        when(paymentsReportService.findDuplicatePaymentsBy(startDate, endDate)).thenReturn(duplicatePaymentsList);

        // Mock the method generateDuplicatePaymentReportCsvAndSendEmail
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                counter[0]++;
                return null;
            }
        }).when(paymentsReportService2).generateDuplicatePaymentsCsvAndSendEmail(any(), any(), any());

        // Act
        paymentsReportService.generateDuplicatePaymentReportCsvAndSendEmail(duplicatePaymentsList, paymentReportConfig);

        // Assert
        assertThat(counter[0] == 0);
    }

   @Test
   public void shouldFindDuplicatePaymentsBy() {
        // Given
        Date startDate = new Date();
        Date endDate = new Date();


       Tuple tupleMock = mock(Tuple.class);
       when(tupleMock.get(0, Date.class)).thenReturn(new Date());
       when(tupleMock.get(1, String.class)).thenReturn("Description");
       when(tupleMock.get(2, String.class)).thenReturn("Account");
       when(tupleMock.get(3, BigDecimal.class)).thenReturn(new BigDecimal("100.00"));
       when(tupleMock.get(4, String.class)).thenReturn("Currency");
       when(tupleMock.get(5, String.class)).thenReturn("Reference");
       when(tupleMock.get(6, Integer.class)).thenReturn(1);
       when(tupleMock.get(7, BigInteger.class)).thenReturn(BigInteger.valueOf(12345));

       List<Tuple> mockTuples = new ArrayList<>();
       mockTuples.add(tupleMock);

       when(paymentRepository.findDuplicatePaymentsByDate(startDate, endDate)).thenReturn(mockTuples);

       // Act
       List<DuplicatePaymentDto> result = paymentsReportService.findDuplicatePaymentsBy(startDate, endDate);

       // Assert
       assertThat(result).isNotNull();
       assertThat(result.size()).isEqualTo(1);

       DuplicatePaymentDto dto = result.get(0);

       assertThat(tupleMock.get(0, Date.class)).isNotNull();
       assertThat(tupleMock.get(1, String.class)).isEqualTo("Description");
       assertThat(tupleMock.get(2, String.class)).isEqualTo("Account");
       assertThat(tupleMock.get(3, BigDecimal.class)).isEqualTo("100.00");
       assertThat(tupleMock.get(4, String.class)).isEqualTo("Currency");
       assertThat(tupleMock.get(5, String.class)).isEqualTo("Reference");
       assertThat(tupleMock.get(6, Integer.class)).isEqualTo(1);
       assertThat(tupleMock.get(7, BigInteger.class)).isEqualTo(12345);

    }


}
