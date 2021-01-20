package uk.gov.hmcts.payment.api.v1.componenttests.backdoors;

import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.AuthCheckerException;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;

import java.util.concurrent.ConcurrentHashMap;

public class ServiceResolverBackdoor implements SubjectResolver<Service> {
    private final ConcurrentHashMap<String, String> tokenToServiceMap = new ConcurrentHashMap<>();

    public ServiceResolverBackdoor() {
        tokenToServiceMap.put("Bearer service-cmc", "cmc");
    }

    @Override
    public Service getTokenDetails(String token) {
        String serviceId = tokenToServiceMap.get(token);

        if (serviceId == null) {
            throw new AuthCheckerException("Token not found");
        }

        return new Service(serviceId);
    }

    public void registerToken(String token, String serviceId) {
        tokenToServiceMap.put(token, serviceId);
    }
}

