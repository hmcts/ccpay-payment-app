package uk.gov.hmcts.payment.referencedata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.referencedata.model.LegacySite;

import java.util.List;
import java.util.stream.Collectors;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "legacySiteDTOwith")
public class LegacySiteDTO {

    private String siteId;
    private String siteName;

    public static List<LegacySiteDTO> fromLegacySiteList(List<LegacySite> legacySiteList) {
        return legacySiteList.stream().map(LegacySiteDTO::toLegacySiteDTO).collect(Collectors.toList());
    }

    private static LegacySiteDTO toLegacySiteDTO(LegacySite legacySite) {
        return LegacySiteDTO.legacySiteDTOwith()
            .siteId(legacySite.getSiteId())
            .siteName(legacySite.getSiteName())
            .build();
    }
}
