package uk.gov.hmcts.payment.api.controllers.provider;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.payment.api.model.DuplicateServiceRequestDto;
import org.springframework.data.repository.query.FluentQuery;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class PayFeeLinkRepositoryStub implements PaymentFeeLinkRepository {
    @Override
    public <S extends PaymentFeeLink> S save(S entity) {
        Date createDate = new Date();
        entity.setId(1000);
        entity.getPayments().forEach(p -> p.setId(10001));
        entity.getPayments().forEach(payment -> {
            payment.setDateCreated(createDate);
            payment.getStatusHistories().forEach(history -> {
                history.setDateCreated(createDate);
                history.setDateUpdated(createDate);
            });
        });
        return entity;
    }

    @Override
    public <S extends PaymentFeeLink> Iterable<S> saveAll(Iterable<S> entities) {
        return entities;
    }

    @Override
    public Optional<PaymentFeeLink> findById(Integer integer) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Integer integer) {
        return false;
    }

    @Override
    public Iterable<PaymentFeeLink> findAll() {
        return null;
    }

    @Override
    public Iterable<PaymentFeeLink> findAllById(Iterable<Integer> integers) {
        return null;
    }

    @Override
    public Optional<List<DuplicateServiceRequestDto>> getDuplicates(final @Param("date") LocalDate date) { return Optional.empty(); }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(Integer integer) {

    }

    @Override
    public void delete(PaymentFeeLink entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends Integer> integers) {

    }

    @Override
    public void deleteAll(Iterable<? extends PaymentFeeLink> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public Optional<PaymentFeeLink> findByPaymentReference(String id) {
        return Optional.empty();
    }

    @Override
    public Optional<PaymentFeeLink> findByPaymentReferenceAndCcdCaseNumber(String id, String ccdCaseNumber) {
        return Optional.empty();
    }

    @Override
    public Optional<PaymentFeeLink> findByPaymentReferenceAndFeeId(String id, Integer feeId) {
        return Optional.empty();
    }

    @Override
    public Optional<List<PaymentFeeLink>> findByCcdCaseNumber(String ccdCaseNumber) {
        return Optional.empty();
    }

    @Override
    public Optional<PaymentFeeLink> findOne(Specification<PaymentFeeLink> spec) {
        return Optional.empty();
    }

    @Override
    public List<PaymentFeeLink> findAll(Specification<PaymentFeeLink> spec) {
        return null;
    }

    @Override
    public Page<PaymentFeeLink> findAll(Specification<PaymentFeeLink> spec, Pageable pageable) {
        return null;
    }

    @Override
    public List<PaymentFeeLink> findAll(Specification<PaymentFeeLink> spec, Sort sort) {
        return null;
    }

    @Override
    public long count(Specification<PaymentFeeLink> spec) {
        return 0;
    }

    @Override
    public boolean exists(Specification<PaymentFeeLink> spec) {
        return false;
    }

    @Override
    public long delete(Specification<PaymentFeeLink> spec) {
        return 0;
    }

    @Override
    public <S extends PaymentFeeLink, R> R findBy(Specification<PaymentFeeLink> spec,
                                                  Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public String getNextPaymentReference() {
        return "2021-0000000001";
    }
}
