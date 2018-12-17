package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.RemissionRepository;

import java.util.Optional;

@Service
public class RemissionServiceImpl implements RemissionService {

    private final RemissionRepository remissionRepository;

    @Autowired
    public RemissionServiceImpl(RemissionRepository remissionRepository) {
        this.remissionRepository = remissionRepository;
    }

    @Override
    public void create(Remission remission) {
        remissionRepository.save(remission);
    }

    @Override
    public Remission retrieve(Integer id) {
        Optional<Remission> remission = remissionRepository.findById(id);
        return remission.orElse(null);
    }

    @Override
    public Remission retrieve(String hwfReference) {
        Optional<Remission> remission = remissionRepository.findByHwfReference(hwfReference);
        return remission.orElse(null);
    }
}
