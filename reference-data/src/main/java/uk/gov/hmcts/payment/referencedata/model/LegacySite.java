package uk.gov.hmcts.payment.referencedata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Data
@Builder(builderMethodName = "legacySiteWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "legacy_site_id")
public class LegacySite {

    @Id
    @Column(name = "site_code")
    private String siteCode;

    @Column(name = "site_name")
    private String siteName;

}

