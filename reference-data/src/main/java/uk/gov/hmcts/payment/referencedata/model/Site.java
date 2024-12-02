package uk.gov.hmcts.payment.referencedata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Data
@Builder(builderMethodName = "siteWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "site_id")
    private String siteId;

    @Column(name = "name")
    private String name;

    @Column(name = "sop_reference")
    private String sopReference;

    @Column(name = "service")
    private String service;
}
