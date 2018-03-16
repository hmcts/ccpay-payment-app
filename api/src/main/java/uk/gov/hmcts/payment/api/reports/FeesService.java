package uk.gov.hmcts.payment.api.reports;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Service
@Transactional
public class FeesService {
    private static final Logger LOG = getLogger(FeesService.class);


    private FeesRegisterClient feesRegisterClient;

    private Map<String, Fee2Dto> feesDataMap = Collections.emptyMap();

    private LocalDate feesDataMapRefreshedDate = LocalDate.now();

    public FeesService(FeesRegisterClient feesRegisterClient) {
        this.feesRegisterClient = feesRegisterClient;

    }


    public FeeVersionDto getFeeVersion(String feeCode, String feeVersion) {
        Fee2Dto feeFromFeesRegister = getFeesDataMap().get(feeCode);
        FeeVersionDto matchingFeeVersion = null;
        if (null != feeFromFeesRegister) {
            if (feeVersion.equals(feeFromFeesRegister.getCurrentVersion().getVersion().toString())) {
                matchingFeeVersion = feeFromFeesRegister.getCurrentVersion();
            } else {
                Optional<FeeVersionDto> optionalMatchingFeeVersionDto = feeFromFeesRegister.getFeeVersionDtos()
                    .stream().filter(versionDto -> versionDto.getVersion().equals(feeCode)).findFirst();
                if (optionalMatchingFeeVersionDto.isPresent())
                    matchingFeeVersion = optionalMatchingFeeVersionDto.get();
            }
        }
        return matchingFeeVersion;
    }

    public List<PaymentDto> getMemolineAndNacForReconciliation(List<PaymentDto> payments) {
        if (null != payments) {
            for (PaymentDto payment : payments) {
                List<FeeDto> fees = payment.getFees();
                for (FeeDto fee : fees) {
                    FeeVersionDto versionDto = getFeeVersion(fee.getCode(), fee.getVersion());
                    if (null != versionDto) {
                        fee.setMemoLine(versionDto.getMemoLine());
                        fee.setNaturalAccountCode(versionDto.getNaturalAccountCode());
                    }
                }
                payment.setFees(fees);
            }
        }

        return payments;
    }

    public Map<String, Fee2Dto> getFeesDataMap() {
        try {
            if (feesDataMap.isEmpty()) {
                if (feesRegisterClient.getFeesDataAsMap().isPresent())
                    feesDataMap = feesRegisterClient.getFeesDataAsMap().get();
            }
        } catch (Exception ex) {
            LOG.error("CardPaymentsReportScheduler - Unable to get fees data." + ex.getMessage());
        }

        return feesDataMap;
    }

    public void dailyRefreshOfFeesData() {
        if (feesDataMapRefreshedDate.isBefore(LocalDate.now())) {
            feesDataMap.clear();
            System.out.println("***** = clearing fees data");
            getFeesDataMap();
            feesDataMapRefreshedDate = LocalDate.now();
        }
    }


}
