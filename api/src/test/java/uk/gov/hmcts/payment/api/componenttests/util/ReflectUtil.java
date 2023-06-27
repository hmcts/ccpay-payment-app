package uk.gov.hmcts.payment.api.componenttests.util;

public class ReflectUtil {

    public static void setField(Object object, String fieldName, Object value) {
        try {
            var field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set " + fieldName + " of object", e);
        }
    }

}
