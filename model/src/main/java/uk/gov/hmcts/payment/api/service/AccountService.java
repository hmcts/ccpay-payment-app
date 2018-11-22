package uk.gov.hmcts.payment.api.service;

public interface AccountService<T, I> {
    T retrieve(I id);
}
