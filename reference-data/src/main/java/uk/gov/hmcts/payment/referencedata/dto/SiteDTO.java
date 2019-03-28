package uk.gov.hmcts.payment.referencedata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.referencedata.model.Site;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "siteDTOwith")
public class SiteDTO {
    private String siteID;
    private String name;
    private String sopReference;
    private String service;

    public static List<SiteDTO> fromSiteList(List<Site> siteList) {
        return siteList.stream().map(SiteDTO::toSiteDTO).collect(Collectors.toList());
    }

    private static SiteDTO toSiteDTO(Site site) {
        return SiteDTO.siteDTOwith()
            .siteID(site.getSiteId())
            .name(site.getName())
            .sopReference(site.getSopReference())
            .service(site.getService())
            .build();
    }
}
