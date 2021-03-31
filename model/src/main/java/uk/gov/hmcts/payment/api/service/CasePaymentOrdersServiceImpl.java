package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.casepaymentorders.client.CpoServiceClient;
import uk.gov.hmcts.payment.casepaymentorders.client.dto.CpoGetResponse;

@Service
public class CasePaymentOrdersServiceImpl implements CasePaymentOrdersService {
    private final CpoServiceClient cpoServiceClient;

    @Autowired
    public CasePaymentOrdersServiceImpl(CpoServiceClient cpoServiceClient) {
        this.cpoServiceClient = cpoServiceClient;
    }

    @Override
    public CpoGetResponse getCasePaymentOrders(String ids, String caseIds, String page, String size, String authorization) {
        return cpoServiceClient.getCasePaymentOrders(ids, caseIds, page, size, authorization);
    }
}
