package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.payment.api.email.Email;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.DuplicateServiceRequestDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.reports.ServiceRequestReportService;
import uk.gov.hmcts.payment.api.reports.config.ServiceRequestReportConfig;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class ServiceRequestReportServiceTest {

    @InjectMocks
    private ServiceRequestReportService serviceRequestReportService;

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Mock
    private EmailService emailService;

    @Mock
    ServiceRequestReportConfig reportConfig;

    private ServiceRequestReportConfig serviceRequestReportConfig;

    private DuplicateServiceRequestDto duplicateServiceRequestDto;

    @Before
    public void setup() {
        serviceRequestReportConfig = new ServiceRequestReportConfig("fromEmail", new String[]{"toEmail"}, "emailSubject", "emailMessage", true);

        duplicateServiceRequestDto = new DuplicateServiceRequestDto() {
            @Override
            public String getCcd_case_number() {
                return "123";
            }

            @Override
            public Integer getCount() {
                return 3;
            }
        };

        when(reportConfig.getFrom()).thenReturn("fromEmail");
        when(reportConfig.getTo()).thenReturn(new String[]{"toEmail"});
        when(reportConfig.getSubject()).thenReturn("emailSubject");
        when(reportConfig.getMessage()).thenReturn("emailMessage");
        when(reportConfig.getDuplicateSRCsvRecord(duplicateServiceRequestDto)).thenReturn("123,3");
    }

    @Test
    public void generateDuplicateSRCsvAndSendEmailWithValidDate() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        List<DuplicateServiceRequestDto> duplicateServiceRequestDtos = new ArrayList<>();
        duplicateServiceRequestDtos.add(duplicateServiceRequestDto);

        when(paymentFeeLinkRepository.getDuplicates(any(LocalDate.class))).thenReturn( Optional.of(duplicateServiceRequestDtos));

        serviceRequestReportService.generateDuplicateSRCsvAndSendEmail(date);

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
