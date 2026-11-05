package uk.gov.hmcts.payment.api.reports;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;

import java.util.*;

@Slf4j
@Service
public class FeesService {

    private final FeesRegisterAdapter feesRegisterAdapter;

    @Autowired
    public FeesService(@NonNull FeesRegisterAdapter feesRegisterAdapter) {
        this.feesRegisterAdapter = feesRegisterAdapter;
    }

    public Optional<FeeVersionDto> getFeeVersion(String feeCode, String version) {
        try {
            Optional<Map<String, FeeVersionDto>> feeVersionsDtoMapForAFeeCode = Optional.ofNullable(getFeesVersionsData().get(feeCode));
            FeeVersionDto matchingFeeDtoVersion = null;
            if (feeVersionsDtoMapForAFeeCode.isPresent()) {
                matchingFeeDtoVersion = feeVersionsDtoMapForAFeeCode.get().get(version);
            }
            return Optional.ofNullable(matchingFeeDtoVersion);
        } catch (Exception ex) {
            log.error("Error fetching FeeVersion by code:{} and version:{}", feeCode,  version, ex);
        }
        return Optional.empty();
    }

    public Map<String, Map<String, FeeVersionDto>> getFeesVersionsData() {

        Map<String, Map<String, FeeVersionDto>> mapOfFeeVersionsDtoMap = new HashMap<>();

        Map<String, Fee2Dto> feesDtoMap = getFeesDtoMap();
        if (feesDtoMap != null) {
            Iterator<Map.Entry<String, Fee2Dto>> iterator = feesDtoMap.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, Fee2Dto> entry = iterator.next();
                Map<String, FeeVersionDto> feeVersionsDtoMap = new HashMap<>();
                if (entry.getValue().getCurrentVersion() != null) {
                    feeVersionsDtoMap.put(entry.getValue().getCurrentVersion().getVersion().toString(),
                        entry.getValue().getCurrentVersion());
                }
                for (FeeVersionDto feeVersion : entry.getValue().getFeeVersionDtos()) {
                    feeVersionsDtoMap.put(feeVersion.getVersion().toString(), feeVersion);
                }

                mapOfFeeVersionsDtoMap.put(entry.getKey(), feeVersionsDtoMap);

            }
        }
        return mapOfFeeVersionsDtoMap;
    }

    @Nullable
    public Map<String, Fee2Dto> getFeesDtoMap() {
        return feesRegisterAdapter.getFeesDtoMap();
    }

}
