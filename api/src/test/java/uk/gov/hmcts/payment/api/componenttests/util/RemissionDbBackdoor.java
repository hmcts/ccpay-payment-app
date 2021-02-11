package uk.gov.hmcts.payment.api.componenttests.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.RemissionRepository;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;

@Component
public class RemissionDbBackdoor {
    @Autowired
    private RemissionRepository remissionRepository;

    public Remission findByHwfReference(String hwfReference) {
        return remissionRepository.findByHwfReference(hwfReference).orElseThrow(RemissionNotFoundException::new);
    }

    public Remission findByRemissionReference(String remissionReference) {
        return remissionRepository.findByRemissionReference(remissionReference).orElseThrow(RemissionNotFoundException::new);
    }
}
