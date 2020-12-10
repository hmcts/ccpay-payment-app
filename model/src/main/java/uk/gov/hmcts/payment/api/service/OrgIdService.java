package uk.gov.hmcts.payment.api.service;

import org.springframework.stereotype.Service;

@Service
public class OrgIdService {

    public String getOrgId(String caseType){
        return "org_id_1223";
    }
}
