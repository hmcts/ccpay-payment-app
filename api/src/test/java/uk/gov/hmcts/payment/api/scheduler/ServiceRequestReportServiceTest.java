package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.PaymentApiApplication;
import uk.gov.hmcts.payment.api.email.Email;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.DuplicateServiceRequestDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.reports.ServiceRequestReportService;
import uk.gov.hmcts.payment.api.reports.config.CardPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.ServiceRequestReportConfig;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@ActiveProfiles({"local", "test"})
@SpringBootTest(webEnvironment = MOCK)
public class ServiceRequestReportServiceTest {

    @Autowired
    private ServiceRequestReportService serviceRequestReportService;
    @MockBean
    private EmailService emailService;
    @MockBean
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @MockBean
    private ServiceRequestReportConfig serviceRequestReportConfig;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void shouldDelegateToEmailService() {

        when(paymentFeeLinkRepository.getNextPaymentReference()).thenReturn("123");

        // given
        //serviceRequestReportConfig = new ServiceRequestReportConfig("fromEmail", new String[]{"toEmail"}, "emailSubject", "emailMessage", true);

        // when
        serviceRequestReportService.generateDuplicateSRCsvAndSendEmail(LocalDate.of(2024, 06, 01));


        List<DuplicateServiceRequestDto> duplicateServiceRequestDtos = new ArrayList<>();
        DuplicateServiceRequestDto duplicateServiceRequestDto = new DuplicateServiceRequestDto() {
            @Override
            public String getCcd_case_number() {
                return "123";
            }

            @Override
            public Integer getCount() {
                return 3;
            }
        };
        duplicateServiceRequestDtos.add(duplicateServiceRequestDto);
        when(paymentFeeLinkRepository.getDuplicates(any(LocalDate.class))).thenReturn( Optional.of(duplicateServiceRequestDtos));
        // then
        ArgumentCaptor<Email> argument = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(argument.capture());
        assertThat(argument.getValue().getFrom()).isEqualTo("fromEmail");
        assertThat(argument.getValue().getTo()).containsExactly("toEmail");
        assertThat(argument.getValue().getSubject()).isEqualTo("emailSubject");
        assertThat(argument.getValue().getMessage()).isEqualTo("emailMessage");
        assertThat(argument.getValue().hasAttachments()).isTrue();
        assertThat(argument.getValue().getAttachments().get(0).getFilename()).startsWith(serviceRequestReportConfig.DUPLICATE_SR_CSV_FILE_PREFIX);
    }

}
