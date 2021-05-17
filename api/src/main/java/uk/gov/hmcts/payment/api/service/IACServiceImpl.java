package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IACServiceImpl implements IACService {
    private static final Logger LOG = LoggerFactory.getLogger(IACServiceImpl.class);

    @Autowired
    private IACServiceResponse iacServiceResponse;

    @Override
    public ResponseEntity<SupplementaryPaymentDto> getIacSupplementaryInfo(List<PaymentDto> paymentDtos, String serviceName) {
       HttpStatus paymentResponseHttpStatus = HttpStatus.OK;
        boolean isExceptionOccure = false;
        List<PaymentDto> iacPayments = paymentDtos.stream().filter(payment -> (payment.getServiceName().equalsIgnoreCase(serviceName))).
            collect(Collectors.toList());
        LOG.info("No of Iac payment retrieved  : {}", iacPayments.size());

        List<String> iacCcdCaseNos = iacPayments.stream().map(paymentDto -> paymentDto.getCcdCaseNumber()).
            collect(Collectors.toList());

        ResponseEntity<SupplementaryDetailsResponse> responseEntitySupplementaryInfo = null;

        List<SupplementaryInfo> lstSupplementaryInfo = null;
        SupplementaryPaymentDto supplementaryPaymentDto = null;

        if (!iacCcdCaseNos.isEmpty()) {
            LOG.info("List of IAC Ccd Case numbers : {}", iacCcdCaseNos.toString());
            try {
                responseEntitySupplementaryInfo = iacServiceResponse.getIacSupplementaryInfoResponse(iacCcdCaseNos);
            } catch (HttpClientErrorException ex) {
                LOG.info("IAC Supplementary information could not be found, exception: {}", ex.getMessage());
                paymentResponseHttpStatus = HttpStatus.PARTIAL_CONTENT;
                isExceptionOccure = true;
            } catch (HystrixRuntimeException hystrixRuntimeException) {
                LOG.info("IAC Supplementary Info response not received in time, exception: {}", hystrixRuntimeException.getMessage());
                paymentResponseHttpStatus = HttpStatus.PARTIAL_CONTENT;
                isExceptionOccure = true;
            } catch (Exception ex) {
                LOG.info("Unable to retrieve IAC Supplementary Info information, exception: {}", ex.getMessage());
                paymentResponseHttpStatus = HttpStatus.PARTIAL_CONTENT;
                isExceptionOccure = true;
            }
            if (!isExceptionOccure) {
                paymentResponseHttpStatus = responseEntitySupplementaryInfo.getStatusCode();
                ObjectMapper objectMapperSupplementaryInfo = new ObjectMapper();
                SupplementaryDetailsResponse supplementaryDetailsResponse = objectMapperSupplementaryInfo.convertValue(responseEntitySupplementaryInfo.getBody(), SupplementaryDetailsResponse.class);
                lstSupplementaryInfo = supplementaryDetailsResponse.getSupplementaryInfo();
                MissingSupplementaryInfo lstMissingSupplementaryInfo = supplementaryDetailsResponse.getMissingSupplementaryInfo();

                if (responseEntitySupplementaryInfo.getStatusCodeValue() == HttpStatus.PARTIAL_CONTENT.value() && lstMissingSupplementaryInfo == null) {
                    LOG.info("No missing supplementary info received from IAC for any CCD case numbers, however response is 206");
                } else if (lstMissingSupplementaryInfo != null && lstMissingSupplementaryInfo.getCcdCaseNumbers() != null)
                    LOG.info("missing supplementary info from IAC for CCD case numbers : {}", lstMissingSupplementaryInfo.getCcdCaseNumbers().toString());
            }

            supplementaryPaymentDto = SupplementaryPaymentDto.supplementaryPaymentDtoWith().payments(paymentDtos).
                supplementaryInfo(lstSupplementaryInfo).build();
            }else{
                LOG.info("No Iac payments retrieved");
                 supplementaryPaymentDto = SupplementaryPaymentDto.supplementaryPaymentDtoWith().payments(paymentDtos).
                    build();
            }
           return new ResponseEntity(supplementaryPaymentDto, paymentResponseHttpStatus);
    }

}








