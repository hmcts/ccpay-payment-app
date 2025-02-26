package uk.gov.hmcts.payment.api.domain.mapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestFeeBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestOnlinePaymentBo;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.net.URL;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ServiceRequestDtoDomainMapper {

    private static final String RETURN_URL_PATH_CONFIRMATION = "/confirmation";
    @Autowired
    private ReferenceUtil referenceUtil;
    @Autowired
    private UserIdSupplier userIdSupplier;
    @Autowired
    private ServiceIdSupplier serviceIdSupplier;
    @Autowired
    private PaymentReference paymentReference;

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRequestDtoDomainMapper.class);

    public ServiceRequestBo toDomain(ServiceRequestDto serviceRequestDto, OrganisationalServiceDto organisationalServiceDto) {

        String serviceRequestReference = paymentReference.getNext();

        return ServiceRequestBo.serviceRequestBoWith()
            .callBackUrl(serviceRequestDto.getCallBackUrl())
            .enterpriseServiceName(organisationalServiceDto.getServiceDescription())
            .orgId(organisationalServiceDto.getServiceCode())
            .ccdCaseNumber(serviceRequestDto.getCcdCaseNumber())
            .caseReference(serviceRequestDto.getCaseReference())
            .reference(serviceRequestReference)
            .fees(serviceRequestDto.getFees()
                .stream()
                .map(feeDto -> toFeeDomain(feeDto, serviceRequestDto.getCcdCaseNumber())) // Will be removed after get api's work without ccd dependency
                .collect(Collectors.toList()))
            .build();
    }

    public ServiceRequestFeeBo toFeeDomain(ServiceRequestFeeDto serviceRequestFeeDto, String ccdCaseNumber) {
        return ServiceRequestFeeBo.serviceRequestFeeBoWith()
            .calculatedAmount(serviceRequestFeeDto.getCalculatedAmount())
            .amountDue(serviceRequestFeeDto.getCalculatedAmount()) //amount due = calculated amount
            .code(serviceRequestFeeDto.getCode())
            .ccdCaseNumber(ccdCaseNumber)
            .version(serviceRequestFeeDto.getVersion())
            .volume(serviceRequestFeeDto.getVolume() != null ? serviceRequestFeeDto.getVolume() : 1)
            .netAmount(serviceRequestFeeDto.getCalculatedAmount())
            .build();
    }


    public ServiceRequestOnlinePaymentBo toDomain(OnlineCardPaymentRequest request, String returnUrl, String serviceCallbackUrl) throws CheckDigitException {
        String uuid = UUID.randomUUID().toString();
        return ServiceRequestOnlinePaymentBo.serviceRequestOnlinePaymentBo()
            .internalReference(uuid)
            .paymentReference(referenceUtil.getNext("RC"))
            .s2sServiceName(serviceIdSupplier.get())
            .userId(userIdSupplier.get())
            .description("card payment")
            .returnUrl(returnUrl + "/" + uuid + RETURN_URL_PATH_CONFIRMATION)
            .currency(request.getCurrency().getCode())
            .amount(request.getAmount())
            .serviceCallbackUrl(serviceCallbackUrl)
            .language(request.getLanguage().toLowerCase())//change language to lower case before sending to gov pay
            .build();
    }

    public CreatePaymentRequest createGovPayRequest(ServiceRequestOnlinePaymentBo requestOnlinePaymentBo) {
        String reqPaymentLanguage = (requestOnlinePaymentBo.getLanguage() != null
            && requestOnlinePaymentBo.getLanguage().equalsIgnoreCase("cy")) ? "cy" : "en";
        String reqPaymentReturnUrl = UriComponentsBuilder
            .fromHttpUrl(requestOnlinePaymentBo.getReturnUrl())
            .queryParam("language", reqPaymentLanguage)
            .build()
            .toUriString();

        LOG.info("requestOnlinePaymentBo.getLanguage() {} -> {}", requestOnlinePaymentBo.getLanguage(), reqPaymentLanguage);
        LOG.info("requestOnlinePaymentBo.getReturnUrl() {} -> {}", requestOnlinePaymentBo.getReturnUrl(), reqPaymentReturnUrl);
        return new CreatePaymentRequest(requestOnlinePaymentBo.getAmount().movePointRight(2).intValue(),
            requestOnlinePaymentBo.getPaymentReference(), requestOnlinePaymentBo.getDescription(),
            reqPaymentReturnUrl, reqPaymentLanguage
        );
    }
}
