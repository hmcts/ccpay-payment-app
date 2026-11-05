package uk.gov.hmcts.payment.api.jpaaudit.listner;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Static bean lookup for JPA entity listeners, which are not Spring-managed.
 *
 * <p>In production there is exactly one context. In tests, Spring's context cache can hold several, and a
 * context that registered last may since have been closed (e.g. by {@code @DirtiesContext}) while an earlier
 * cached one is still in use - so lookups go to the most recently registered context that is still active,
 * rather than whichever one happened to register last.
 */
@Service
public class BeanUtil implements ApplicationContextAware {

    private static final Deque<ApplicationContext> CONTEXTS = new ConcurrentLinkedDeque<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CONTEXTS.remove(applicationContext);
        CONTEXTS.addFirst(applicationContext);
    }

    public static <T> T getBean(Class<T> beanClass) {
        for (ApplicationContext context : CONTEXTS) {
            if (context instanceof ConfigurableApplicationContext configurable && !configurable.isActive()) {
                CONTEXTS.remove(context);
                continue;
            }
            return context.getBean(beanClass);
        }
        throw new IllegalStateException("No active ApplicationContext available to look up " + beanClass.getName());
    }

}
