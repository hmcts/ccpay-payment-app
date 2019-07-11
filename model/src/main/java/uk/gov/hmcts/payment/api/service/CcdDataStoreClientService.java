package uk.gov.hmcts.payment.api.service;

public interface CcdDataStoreClientService<T, I> {
    T getCase(String userAuthToken, String serviceAuthToken, I id);
}
