package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.ff4j.FF4j;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestPaymentDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class ServiceRequestDomainServiceTest  {

    @MockBean
    private TopicClientService topicClientService;

    @Autowired
    ServiceRequestDomainService serviceRequestDomainService;

    @MockBean
    private TopicClientProxy topicClientProxy;

//    @Autowired
//    ServiceRequestPaymentBo serviceRequestPaymentBo;

    @MockBean
    private UserIdSupplier userIdSupplier;

    @MockBean
    private ServiceIdSupplier serviceIdSupplier;

    @MockBean
    private PaymentStatusRepository paymentStatusRepository;

    @MockBean
    private PaymentChannelRepository paymentChannelRepository;

    @MockBean
    private PaymentMethodRepository paymentMethodRepository;

    @MockBean
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @MockBean
    private FeePayApportionService feePayApportionService;

    @MockBean
    private LaunchDarklyFeatureToggler featureToggler;

//    @MockBean
//    private ServiceRequestPaymentDomainDataEntityMapper serviceRequestPaymentDomainDataEntityMapper;

    @Autowired
    private FF4j ff4j;

    @Test
    public void deadLetterTest() throws ServiceBusException, InterruptedException, IOException {

        IMessage msg = mock(IMessage.class);
        IMessageReceiver subscriptionClient = mock(IMessageReceiver.class);
        TopicClientProxy topicClientProxy = mock(TopicClientProxy.class);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> data = new HashMap<>();

        data.put("action", "action");
        data.put("case_id", "caseId");
        data.put("order_reference", "orderReference");
        data.put("responsible_party", "responsibleParty");

        byte[] dataInBytes = mapper.writeValueAsBytes(data);
        Map <String ,String>  msgProperties = new HashMap<>();
        msgProperties.put("action", "500");

        when(subscriptionClient.receive()).thenReturn(msg,null);
        when(msg.getBody()).thenReturn(dataInBytes);
        when(msg.getProperties()).thenReturn(msgProperties);
        when(topicClientService.getTopicClientProxy()).thenReturn(topicClientProxy);
        doNothing().when(topicClientProxy).send(any(IMessage.class));
        doNothing().when(topicClientProxy).close();
        serviceRequestDomainService.deadLetterProcess(subscriptionClient);
        verify(topicClientProxy, times(1)).close();
    }

    @Test
    public void addpaymentTest() throws CheckDigitException {

        Payment payment = Payment.paymentWith().internalReference("abc")
            .reference("RC-1632-3254-9172-5888").paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .build();

        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);

        Date now = new Date();

        PaymentFeeLink serviceRequest = PaymentFeeLink.paymentFeeLinkWith().ccdCaseNumber("1234")
            .enterpriseServiceName("CMC")
            .payments(paymentList)
            .paymentReference("123456")
            .caseReference("1234567")
            .orgId("AB01")
            .dateCreated(now)
            .callBackUrl("http://localhost")
            .build();

        String serviceRequestReference= "2022-1234567890123";

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(300))
            .currency("GBP")
            .customerReference("testCustReference").
            build();
        when(userIdSupplier.get()).thenReturn("ABC");
        when(serviceIdSupplier.get()).thenReturn("ccpay_bubble");
        when(paymentMethodRepository.findByNameOrThrow(anyString())).thenReturn(PaymentMethod.paymentMethodWith().name("cheque").build());
        when(paymentChannelRepository.findByNameOrThrow(anyString())).thenReturn(PaymentChannel.paymentChannelWith().name("digital bar").build());
        when(paymentStatusRepository.findByNameOrThrow(anyString())).thenReturn(PaymentStatus.paymentStatusWith().name("success").build());
        List<String> pbaConfig1ServiceNames =new ArrayList<String>();
        pbaConfig1ServiceNames.add("abc");
        pbaConfig1ServiceNames.add("def");
        when(paymentFeeLinkRepository.save(any())).thenReturn(serviceRequest);
        when(paymentFeeLinkRepository.findByPaymentReference(any())).thenReturn(java.util.Optional.ofNullable(serviceRequest));
        doNothing().when(feePayApportionService).processApportion(any());
        when(featureToggler.getBooleanValue(eq("apportion-feature"), anyBoolean())).thenReturn(true);
        serviceRequestDomainService.addPayments(serviceRequest, serviceRequestReference, serviceRequestPaymentDto);

    }


}
