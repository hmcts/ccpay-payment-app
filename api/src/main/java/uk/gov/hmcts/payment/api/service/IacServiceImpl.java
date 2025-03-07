package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.IacSupplementaryRequest;
import uk.gov.hmcts.payment.api.dto.LiberataSupplementaryDetails;
import uk.gov.hmcts.payment.api.dto.LiberataSupplementaryInfo;
import uk.gov.hmcts.payment.api.dto.MissingSupplementaryInfo;
import uk.gov.hmcts.payment.api.dto.SupplementaryDetailsResponse;
import uk.gov.hmcts.payment.api.dto.SupplementaryInfo;
import uk.gov.hmcts.payment.api.dto.SupplementaryPaymentDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IacServiceImpl implements IacService {
    private static final Logger LOG = LoggerFactory.getLogger(IacServiceImpl.class);

    @Value("${iac.supplementary.info.url}")
    private String iacSupplementaryInfoUrl;

    @Autowired
    @Qualifier("restTemplateIacSupplementaryInfo")
    private RestTemplate restTemplateIacSupplementaryInfo;

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Override
    public ResponseEntity<SupplementaryPaymentDto> getIacSupplementaryInfo(List<PaymentDto> paymentDtos, String serviceName) {
       HttpStatusCode paymentResponseHttpStatus = HttpStatus.OK;
        boolean isExceptionOccur = false;

        List<PaymentDto> iacPayments = getIacPayments(serviceName, paymentDtos);
        LOG.info("No of Iac payment retrieved  : {}", iacPayments.size());

        List<String> iacCcdCaseNos = iacPayments.stream().map(paymentDto -> paymentDto.getCcdCaseNumber()).
            collect(Collectors.toList());

        ResponseEntity<SupplementaryDetailsResponse> responseEntitySupplementaryInfo = null;

        List<SupplementaryInfo> lstSupplementaryInfo = null;
        List<LiberataSupplementaryInfo> lstLiberataSupplementaryInfo = null;
        SupplementaryPaymentDto supplementaryPaymentDto = null;

        if (!iacCcdCaseNos.isEmpty()) {
            LOG.info("List of IAC Ccd Case numbers : {}", iacCcdCaseNos.toString());
            try {
                responseEntitySupplementaryInfo = getIacSupplementaryInfoResponse(iacCcdCaseNos);
            } catch (HttpClientErrorException ex) {
                LOG.info("IAC Supplementary information could not be found, exception: {}", ex.getMessage());
                paymentResponseHttpStatus = HttpStatus.PARTIAL_CONTENT;
                isExceptionOccur = true;
            } catch (Exception ex) {
                LOG.info("Unable to retrieve IAC Supplementary Info information, exception: {}", ex.getMessage());
                paymentResponseHttpStatus = HttpStatus.PARTIAL_CONTENT;
                isExceptionOccur = true;
            }
            if (!isExceptionOccur) {
                paymentResponseHttpStatus = responseEntitySupplementaryInfo.getStatusCode();
                ObjectMapper objectMapperSupplementaryInfo = new ObjectMapper();
                SupplementaryDetailsResponse supplementaryDetailsResponse = objectMapperSupplementaryInfo.convertValue(responseEntitySupplementaryInfo.getBody(), SupplementaryDetailsResponse.class);
                lstSupplementaryInfo = supplementaryDetailsResponse.getSupplementaryInfo();
                MissingSupplementaryInfo lstMissingSupplementaryInfo = supplementaryDetailsResponse.getMissingSupplementaryInfo();

                if (responseEntitySupplementaryInfo.getStatusCodeValue() == HttpStatus.PARTIAL_CONTENT.value() && lstMissingSupplementaryInfo == null) {
                    LOG.info("No missing supplementary info received from IAC for any CCD case numbers, however response is 206");
                } else if (lstMissingSupplementaryInfo != null && lstMissingSupplementaryInfo.getCcdCaseNumbers() != null) {
                    LOG.info("Missing supplementary info from IAC for CCD case numbers : {}", lstMissingSupplementaryInfo.getCcdCaseNumbers().toString());
                }

                if (lstSupplementaryInfo != null) {
                    Map<String, String> caseReferenceMap = lstSupplementaryInfo.stream()
                        .collect(Collectors.toMap(SupplementaryInfo::getCcdCaseNumber,
                            info -> info.getSupplementaryDetails().getCaseReferenceNumber()));

                    for (PaymentDto paymentDto : paymentDtos) {
                        String caseReferenceNumber = caseReferenceMap.get(paymentDto.getCcdCaseNumber());
                        if (caseReferenceNumber != null) {
                            paymentDto.setCaseReference(caseReferenceNumber);
                            LOG.info("Setting caseReference {} for CCD Case {}", caseReferenceNumber, paymentDto.getCcdCaseNumber());
                        }
                    }

                    lstLiberataSupplementaryInfo = lstSupplementaryInfo.stream()
                        .map(info -> LiberataSupplementaryInfo.liberataSupplementaryInfoWith()
                            .ccdCaseNumber(info.getCcdCaseNumber())
                            .supplementaryDetails(LiberataSupplementaryDetails.supplementaryDetailsWith()
                                .surname(info.getSupplementaryDetails().getSurname())
                                .build())
                            .build())
                        .collect(Collectors.toList());
                }
            }

            supplementaryPaymentDto = SupplementaryPaymentDto.supplementaryPaymentDtoWith()
                .payments(paymentDtos)
                .supplementaryInfo(lstLiberataSupplementaryInfo)
                .build();
        }else{
            LOG.info("No Iac payments retrieved");
             supplementaryPaymentDto = SupplementaryPaymentDto.supplementaryPaymentDtoWith().payments(paymentDtos).
                build();
        }

       return new ResponseEntity(supplementaryPaymentDto, paymentResponseHttpStatus);
    }

    private List<PaymentDto> getIacPayments(String serviceName, List<PaymentDto> paymentDtos) {
        return paymentDtos.stream().filter(payment -> (payment.getServiceName().equalsIgnoreCase(serviceName))).
            collect(Collectors.toList());
    }

    private ResponseEntity<SupplementaryDetailsResponse> getIacSupplementaryInfoResponse(List<String> iacCcdCaseNos) throws RestClientException {

        IacSupplementaryRequest iacSupplementaryRequest = IacSupplementaryRequest.createIacSupplementaryRequestWith()
            .ccdCaseNumbers(iacCcdCaseNos).build();

        MultiValueMap<String, String> headerMultiValueMapForIacSuppInfo = new LinkedMultiValueMap<String, String>();
        List<String> serviceAuthTokenPaymentList = new ArrayList<>();

        //Generate token for payment api and replace
        serviceAuthTokenPaymentList.add(authTokenGenerator.generate());

        headerMultiValueMapForIacSuppInfo.put("ServiceAuthorization", serviceAuthTokenPaymentList);
        LOG.info("IAC Supplementary info URL: {}", iacSupplementaryInfoUrl + "/supplementary-details");

        HttpHeaders headers = new HttpHeaders(headerMultiValueMapForIacSuppInfo);
        final HttpEntity<IacSupplementaryRequest> entity = new HttpEntity<>(iacSupplementaryRequest, headers);
        return this.restTemplateIacSupplementaryInfo.exchange(iacSupplementaryInfoUrl + "/supplementary-details", HttpMethod.POST, entity, SupplementaryDetailsResponse.class);
    }

}








