package uk.gov.hmcts.payment.api.service;

public interface CcdDataStoreService<T, I> {
    T retrieve(I id);
}
