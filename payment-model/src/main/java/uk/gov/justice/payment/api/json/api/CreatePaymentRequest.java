

        package uk.gov.justice.payment.api.json.api;

        import com.fasterxml.jackson.annotation.*;

        import javax.annotation.Generated;
        import java.util.HashMap;
        import java.util.Map;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Generated("org.jsonschema2pojo")
        @JsonPropertyOrder({
                "amount",
                "application_reference",
                "service_reference",
                "payment_reference",
                "description",
                "return_url"
        })
        public class CreatePaymentRequest {

            @JsonProperty(value = "amount",required=true)
            private Integer amount;

            @JsonProperty("application_reference")
            private String applicationReference;

            @JsonProperty("service_reference")
            private String serviceReference;

            @JsonProperty("payment_reference")
            private String paymentReference;

            @JsonProperty("description")
            private String description;

            @JsonProperty("return_url")
            private String returnUrl;
            @JsonIgnore
            private Map<String, Object> additionalProperties = new HashMap<String, Object>();

            /**
             *
             * @return
             * The amount
             */
            @JsonProperty(value = "amount",required=true)
            public Integer getAmount() {
                return amount;
            }

            /**
             *
             * @param amount
             * The amount
             */
            @JsonProperty(value = "amount",required=true)
            public void setAmount(Integer amount) {
                this.amount = amount;
            }

            /**
             *
             * @return
             * The paymentReference
             */
            @JsonProperty("payment_reference")
            public String getPaymentReference() {
                return paymentReference;
            }

            /**
             *
             * @param paymentReference
             * The payment_reference
             */
            @JsonProperty("payment_reference")
            public void setPaymentReference(String paymentReference) {
                this.paymentReference = paymentReference;
            }


            /**
             *
             * @return
             * The applicationReference
             */
            @JsonProperty("application_reference")
            public String getApplicationReference() {
                return applicationReference;
            }

            /**
             *
             * @param applicationReference
             * The application_reference
             */
            @JsonProperty("application_reference")
            public void setApplicationReference(String applicationReference) {
                this.applicationReference = applicationReference;
            }

            /**
             *
             * @return
             * The serviceReference
             */
            @JsonProperty("service_reference")
            public String getServiceReference() {
                return serviceReference;
            }

            /**
             *
             * @param serviceReference
             * The serviceon_reference
             */
            @JsonProperty("service_reference")
            public void setServicenReference(String serviceReference) {
                this.serviceReference = serviceReference;
            }

            /**
             *
             * @return
             * The description
             */
            @JsonProperty("description")
            public String getDescription() {
                return description;
            }

            /**
             *
             * @param description
             * The description
             */
            @JsonProperty("description")
            public void setDescription(String description) {
                this.description = description;
            }

            /**
             *
             * @return
             * The returnUrl
             */
            @JsonProperty("return_url")
            public String getReturnUrl() {
                return returnUrl;
            }

            /**
             *
             * @param returnUrl
             * The return_url
             */
            @JsonProperty("return_url")
            public void setReturnUrl(String returnUrl) {
                this.returnUrl = returnUrl;
            }

            @JsonAnyGetter
            public Map<String, Object> getAdditionalProperties() {
                return this.additionalProperties;
            }

            @JsonAnySetter
            public void setAdditionalProperty(String name, Object value) {
                this.additionalProperties.put(name, value);
            }

            @Override
            public String toString() {
                return super.toString();
            }
        }