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
import uk.gov.hmcts.payment.api.model.DuplicateServiceRequestKey;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.reports.ServiceRequestReportService;
import uk.gov.hmcts.payment.api.reports.config.ServiceRequestReportConfig;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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

    private DuplicateServiceRequestDto duplicateServiceRequestDto1;
    private DuplicateServiceRequestDto duplicateServiceRequestDto2;

    @Before
    public void setup() {
        serviceRequestReportConfig = new ServiceRequestReportConfig("fromEmail", new String[]{"toEmail"}, "emailSubject", "emailMessage", true);

        duplicateServiceRequestDto1 = new DuplicateServiceRequestDto() {
            @Override
            public String getFee_codes() {
                return "FEE0123";
            }

            @Override
            public Integer getPayment_link_id() {
                return 123;
            }

            @Override
            public String getCcd_case_number() {
                return "123";
            }

            @Override
            public String getEnterprise_service_name() {
                return "Probate";
            }
        };

        duplicateServiceRequestDto2 = new DuplicateServiceRequestDto() {
            @Override
            public String getFee_codes() {
                return "FEE0123";
            }

            @Override
            public Integer getPayment_link_id() {
                return 124;
            }

            @Override
            public String getCcd_case_number() {
                return "123";
            }

            @Override
            public String getEnterprise_service_name() {
                return "Probate";
            }
        };

    DuplicateServiceRequestKey duplicateServiceRequestKey = DuplicateServiceRequestKey.builder()
        .service("Probate")
        .feeCodes("FEE0123")
        .ccd("123")
        .build();

        when(reportConfig.getFrom()).thenReturn("fromEmail");
        when(reportConfig.getTo()).thenReturn(new String[]{"toEmail"});
        when(reportConfig.getSubject()).thenReturn("emailSubject");
        when(reportConfig.getMessage()).thenReturn("emailMessage");
        when(reportConfig.getDuplicateSRCsvRecord(duplicateServiceRequestKey, 2)).thenReturn("FEE0123,2,123,Probate");
    }

    @Test
    public void generateDuplicateSRCsvAndSendEmailWithValidDate() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        List<DuplicateServiceRequestDto> duplicateServiceRequestDtos = new ArrayList<>();
        duplicateServiceRequestDtos.add(duplicateServiceRequestDto1);
        duplicateServiceRequestDtos.add(duplicateServiceRequestDto2);

        when(paymentFeeLinkRepository.getDuplicates(any(LocalDate.class))).thenReturn(Optional.of(duplicateServiceRequestDtos));

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

    @Test
    public void generateDuplicateSRCsvAndSendEmailWithValidDateButNoDuplicates() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        List<DuplicateServiceRequestDto> duplicateServiceRequestDtos = new ArrayList<>();
        duplicateServiceRequestDtos.add(duplicateServiceRequestDto1);

        when(paymentFeeLinkRepository.getDuplicates(any(LocalDate.class))).thenReturn(Optional.of(duplicateServiceRequestDtos));

        serviceRequestReportService.generateDuplicateSRCsvAndSendEmail(date);

        verifyNoInteractions(emailService);

    }
}
