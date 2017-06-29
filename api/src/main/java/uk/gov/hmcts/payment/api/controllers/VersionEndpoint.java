package uk.gov.hmcts.payment.api.controllers;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "endpoints.version")
public class VersionEndpoint extends AbstractEndpoint<Map<String, Object>> {

    @Value("${git.build.version}")
    private String version;

    @Value("${git.commit.id}")
    private String commitId;

    public VersionEndpoint() {
        super("version", false, true);
    }

    @Override
    public Map<String, Object> invoke() {
        return ImmutableMap.of(
            "version", version,
            "commitId", commitId
        );
    }
}
