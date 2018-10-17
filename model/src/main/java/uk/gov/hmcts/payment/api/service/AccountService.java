package uk.gov.hmcts.payment.api.service;

public interface AccountService<T, ID> {
    T retrieve(ID id);
}
