package uk.gov.hmcts.payment.api.model;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomTuple implements Tuple {
    private final Map<String, Object> elements = new LinkedHashMap<>();

    public CustomTuple(Object... values) {
        elements.put("service_type", values[0]);
        elements.put("ccd_case_number", values[1]);
        elements.put("reference", values[2]);
        elements.put("code", values[3]);
        elements.put("date_created", values[4]);
        elements.put("amount", values[5]);
        elements.put("payment_status", values[6]);
    }

    @Override
    public <X> X get(String alias, Class<X> type) {
        return type.cast(elements.get(alias));
    }

    @Override
    public Object get(String alias) {
        return elements.get(alias);
    }

    @Override
    public <X> X get(int i, Class<X> type) {
        return type.cast(elements.values().toArray()[i]);
    }

    @Override
    public Object get(int i) {
        return elements.values().toArray()[i];
    }

    @Override
    public List<TupleElement<?>> getElements() {
        List<TupleElement<?>> tupleElements = new ArrayList<>();
        for (String key : elements.keySet()) {
            tupleElements.add(new TupleElement<Object>() {
                @Override
                public String getAlias() {
                    return key;
                }

                @Override
                public Class<Object> getJavaType() {
                    return Object.class;
                }
            });
        }
        return tupleElements;
    }

    @Override
    public <X> X get(TupleElement<X> tupleElement) {
        return get(tupleElement.getAlias(), tupleElement.getJavaType());
    }

    @Override
    public Object[] toArray() {
        return elements.values().toArray();
    }
}
