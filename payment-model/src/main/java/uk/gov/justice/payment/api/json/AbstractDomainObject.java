package uk.gov.justice.payment.api.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by zeeshan on 06/10/2016.
 */
public class AbstractDomainObject {
    private ObjectMapper mapper;


    public String toString(Object obj) {
        try {
            mapper = new ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
