package uk.gov.hmcts.payment.api.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.DomainEvents;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.audit.AuditRepository;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.domain.mapper.OrderDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.OrderPaymentDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.OrderPaymentDtoDomainMapper;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.model.CaseDetails;
import uk.gov.hmcts.payment.api.model.CaseDetailsRepository;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "orderBoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Component
public class OrderBo {
    //-- All CRUD & Validation operations for Orders to be implemented

    private String reference;

    private String ccdCaseNumber;

    private String caseReference;

    private String orgId;

    private String enterpriseServiceName;

    private List<OrderFeeBo> fees;

    private PaymentStatus status;

    private BigDecimal orderBalance;


    @Autowired
    private OrderDomainDataEntityMapper orderDomainDataEntityMapper;

    @Autowired
    private CaseDetailsRepository caseDetailsRepository;

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Autowired
    private PaymentGroupService paymentGroupService;

    @Transactional
    public String createOrder(OrderBo orderBo) {

        CaseDetails caseDetailsEntity;
        if (!caseDetailsRepository.existsByCcdCaseNumber(orderBo.getCcdCaseNumber())) {
            caseDetailsEntity = orderDomainDataEntityMapper.toCaseDetailsEntity(orderBo);
            caseDetailsRepository.save(caseDetailsEntity);
        } else {
            caseDetailsEntity = caseDetailsRepository.findByCcdCaseNumber(orderBo.getCcdCaseNumber());
        }

        PaymentFeeLink paymentFeeLinkAliasOrderEntity = orderDomainDataEntityMapper.toOrderEntity(orderBo);

        paymentFeeLinkAliasOrderEntity.getCaseDetails().add(caseDetailsEntity);

        PaymentFeeLink orderSavedWithFees = paymentFeeLinkRepository.save(paymentFeeLinkAliasOrderEntity);

        return orderSavedWithFees.getPaymentReference();
    }

    public void canAcceptPayment() {
        // Validate if Order has outstanding Balance
        // false : reject Payment
        // true : Continue
    }

    public void getStatus() {

    }


}
