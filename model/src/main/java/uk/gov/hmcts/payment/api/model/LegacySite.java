package uk.gov.hmcts.payment.api.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Data
@Builder(builderMethodName = "legacySiteWith")
@Table(name = "legacy_site_id")
@NoArgsConstructor
public class LegacySite{

    @Id
    @Column(name = "site_id")
    private String siteId;

    @Column(name = "site_name")
    private String siteName;

    public LegacySite(String siteId,String siteName){
        this.siteId=siteId;
        this.siteName=siteName;
    }

}

