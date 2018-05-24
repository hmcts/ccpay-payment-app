package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.util.PaymentMethodUtil;

import java.time.LocalDate;
import java.util.List;

public interface PaymentService<T, ID> {

    T retrieve(ID id);

    List<T> search(LocalDate startDate, LocalDate endDate, PaymentMethodUtil type);
}
