package uk.gov.hmcts.payment.api.service;

public interface CardDetailsService<T, ID> {

    T retrieve(ID id);
}
