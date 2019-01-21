package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.RemissionRepository;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;

import java.util.Optional;

@Service
public class RemissionServiceImpl implements RemissionService {

    private final RemissionRepository remissionRepository;
    private final ReferenceUtil referenceUtil;

    @Autowired
    public RemissionServiceImpl(RemissionRepository remissionRepository,
                                ReferenceUtil referenceUtil) {
        this.remissionRepository = remissionRepository;
        this.referenceUtil = referenceUtil;
    }

    @Override
    public void create(Remission remission) throws CheckDigitException {
        remission.setRemissionReference(referenceUtil.getNext("RM"));
        remissionRepository.save(remission);
    }

    @Override
    public Remission retrieve(String hwfReference) {
        Optional<Remission> remission = remissionRepository.findByHwfReference(hwfReference);
        return remission.orElse(null);
    }
}
