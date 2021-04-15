package uk.gov.hmcts.payment.api.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.DomainEvents;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.domain.mapper.OrderDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.OrderPaymentDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.OrderPaymentDtoDomainMapper;
import uk.gov.hmcts.payment.api.model.CaseDetails;
import uk.gov.hmcts.payment.api.model.CaseDetailsRepository;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;

import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "orderBoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderBo{
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
    private PaymentGroupService paymentGroupService;

    public String createOrder(OrderBo orderBo){

        PaymentFeeLink paymentFeeLinkAliasOrderEntity = orderDomainDataEntityMapper.toOrderEntity(orderBo);

        PaymentFeeLink orderSavedWithFees = (PaymentFeeLink) paymentGroupService.addNewFeeWithPaymentGroup(paymentFeeLinkAliasOrderEntity);

        CaseDetails caseDetailsEntity = caseDetailsRepository.findByCcdCaseNumber(orderBo.getCcdCaseNumber()).orElse(orderDomainDataEntityMapper.toCaseDetailsEntity(orderBo));

        caseDetailsEntity.getOrders().add(paymentFeeLinkAliasOrderEntity);

        caseDetailsRepository.save(caseDetailsEntity);

        return orderSavedWithFees.getPaymentReference();
    }


    public void validate(){
        //--fee validation logic for duplicate Fees in Request
        //--CCD Case 16 digit check
    }

    public void canAcceptPayment(){
        // Validate if Order has outstanding Balance
        // false : reject Payment
        // true : Continue
    }

    public void getStatus() {

    }


}
