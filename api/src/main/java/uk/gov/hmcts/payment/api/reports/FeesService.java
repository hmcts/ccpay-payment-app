package uk.gov.hmcts.payment.api.reports;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;

import java.time.LocalDate;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Service
@Transactional
public class FeesService {
    private static final Logger LOG = getLogger(FeesService.class);


    private FeesRegisterClient feesRegisterClient;

    private Map<String, Fee2Dto> feesDtoMap = Collections.emptyMap();

    private LocalDate feesDtoMapRefreshedDate = LocalDate.now();

    public FeesService(FeesRegisterClient feesRegisterClient) {
        this.feesRegisterClient = feesRegisterClient;

    }


    public Optional<FeeVersionDto> getFeeVersion(String feeCode, String version) {
        Optional<Map<String, FeeVersionDto>> feeVersionsDtoMapForAFeeCode = Optional.ofNullable(getFeesVersionsData().get(feeCode));
        FeeVersionDto matchingFeeDtoVersion = null;
        if (feeVersionsDtoMapForAFeeCode.isPresent()) {
            matchingFeeDtoVersion = feeVersionsDtoMapForAFeeCode.get().get(version);
        }
        return Optional.ofNullable(matchingFeeDtoVersion);
    }

    public Map<String, Map<String, FeeVersionDto>> getFeesVersionsData() {

        Iterator<Map.Entry<String, Fee2Dto>> iterator = getFeesDtoMap().entrySet().iterator();
        Map<String, Map<String, FeeVersionDto>> mapOfFeeVersionsDtoMap = new HashMap<>();

        while (iterator.hasNext()) {
            Map.Entry<String, Fee2Dto> entry = iterator.next();
            Map<String, FeeVersionDto> feeVersionsDtoMap = new HashMap<>();
            feeVersionsDtoMap.put(entry.getValue().getCurrentVersion().getVersion().toString(),
                entry.getValue().getCurrentVersion());
            for (FeeVersionDto feeVersion : entry.getValue().getFeeVersionDtos()) {
                feeVersionsDtoMap.put(feeVersion.getVersion().toString(), feeVersion);
            }

            mapOfFeeVersionsDtoMap.put(entry.getKey(), feeVersionsDtoMap);

        }
        return mapOfFeeVersionsDtoMap;
    }

    public Map<String, Fee2Dto> getFeesDtoMap() {
        try {
            if (feesDtoMap.isEmpty()) {
                Optional<Map<String, Fee2Dto>> optionalFeesDtoMap = feesRegisterClient.getFeesDataAsMap();
                if (optionalFeesDtoMap.isPresent()) {
                    feesDtoMap = optionalFeesDtoMap.get();
                }
            }
        } catch (Exception ex) {
            LOG.error("FeesService  -  Unable to get fees data." + ex.getMessage());
        }

        return feesDtoMap;
    }

    public void dailyRefreshOfFeesData() {
        if (feesDtoMapRefreshedDate.isBefore(LocalDate.now())) {
            feesDtoMap.clear();
            getFeesDtoMap();
            feesDtoMapRefreshedDate = LocalDate.now();
        }
    }


}
