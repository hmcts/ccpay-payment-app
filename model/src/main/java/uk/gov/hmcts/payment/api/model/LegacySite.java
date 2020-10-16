package uk.gov.hmcts.payment.api.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Data
@Builder(builderMethodName = "legacySiteWith")
@Table(name = "legacy_site_id")
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

