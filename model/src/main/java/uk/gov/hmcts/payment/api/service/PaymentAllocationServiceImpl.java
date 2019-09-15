package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;


@Service
public class PaymentAllocationServiceImpl implements PaymentAllocationService {

    private final PaymentAllocationRepository paymentAllocationRepository;

    private final PaymentAllocationStatusRepository paymentAllocationStatusRepository;


    @Autowired
    public PaymentAllocationServiceImpl(PaymentAllocationRepository paymentAllocationRepository,
                                        PaymentAllocationStatusRepository paymentAllocationStatusRepository) {
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.paymentAllocationStatusRepository = paymentAllocationStatusRepository;

    }


    @Override
    public PaymentAllocation createAllocation(PaymentAllocation paymentAllocation) {

        paymentAllocation.setPaymentAllocationStatus(
            paymentAllocationStatusRepository.findByNameOrThrow(paymentAllocation.getPaymentAllocationStatus().getName()));

        return paymentAllocationRepository.save(paymentAllocation);
    }
}
