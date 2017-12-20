package uk.gov.hmcts.payment.api.componenttests.sugar;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomResultMatcher implements ResultMatcher {

    private final ObjectMapper objectMapper;
    private final Class expectedClass;
    private final List<ResultMatcher> matchers = new ArrayList<>();

    public CustomResultMatcher(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    public CustomResultMatcher(ObjectMapper objectMapper, Class expectedClass) {
        this.objectMapper = objectMapper;
        this.expectedClass = expectedClass;
    }

    public CustomResultMatcher isEqualTo(Object expected) {
        matchers.add(result -> {
            Object actual = objectMapper.readValue(result.getResponse().getContentAsByteArray(), expected.getClass());
            assertThat(actual).isEqualTo(expected);
        });
        return this;
    }

    public <T> ResultMatcher asListOf(Class<T> collectionType, Consumer<List<T>> assertions) {
        matchers.add(result -> {
            JavaType javaType = TypeFactory.defaultInstance().constructCollectionType(List.class, collectionType);
            List actual = objectMapper.readValue(result.getResponse().getContentAsByteArray(), javaType);
            assertions.equals(actual);
        });

        return this;
    }

    public <T> ResultMatcher as(Class<T> bodyType, Consumer<T> assertions) {
        matchers.add(result -> {
            T actual = objectMapper.readValue(result.getResponse().getContentAsByteArray(), bodyType);
            assertions.equals(actual);
        });

        return this;
    }

    @Override
    public void match(MvcResult result) throws Exception {
        for (ResultMatcher matcher : matchers) {
            matcher.match(result);
        }
    }
}
