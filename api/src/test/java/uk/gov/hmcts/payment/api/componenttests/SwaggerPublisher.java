package uk.gov.hmcts.payment.api.componenttests;

import com.azure.opentelemetry.exporters.azuremonitor.AzureMonitorExporterBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest", "mockcallbackservice"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
public class SwaggerPublisher {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WebApplicationContext webAppContext;

    private Tracer tracer;

    @Value("${azure.application-insights.instrumentation-key}")
    private String instrumentationKey;

    @Before
    public void setup() {
        this.tracer = configureCustomTracer();
        this.mvc = webAppContextSetup(this.webAppContext)
            .apply(springSecurity())
            .build();
    }

    @After
    public void tearDown() {
        this.mvc = null;
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void generateDocs() throws Exception {
        Span span = tracer.spanBuilder("generateDocs").startSpan();
        try {
            generateDocsForGroup("payment2");
            generateDocsForGroup("payment-external-api");
            generateDocsForGroup("reference-data");
        } finally {
            span.end();
        }
    }

    private void generateDocsForGroup(String groupName) throws Exception {
        Span span = tracer.spanBuilder("generateDocsForGroup").startSpan();
        try {
            byte[] specs = mvc.perform(
                    get("/v3/api-docs?group=" + groupName)
                        .header("Authorization", "Bearer spoof")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

            try (OutputStream outputStream = Files.newOutputStream(Paths.get("/tmp/swagger-specs." + groupName + ".json"))) {
                outputStream.write(specs);
            }
        } finally {
            span.end();
        }
    }

    private Tracer configureCustomTracer() {
        //Creates an Azure Monitor Exporter using the instrumentationKey
        var applicationInsightsExporter = new AzureMonitorExporterBuilder()
            .instrumentationKey(instrumentationKey)
            .buildExporter();

        //Configures a TracerProvider to batch-process spans and send them to the Azure Monitor Exporter
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(applicationInsightsExporter).build())
            .build();

        //Initializes the OpenTelemetry SDK allowing SwaggerPublisherTracer to be retrieved anywhere
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal();

        return openTelemetrySdk.getTracer("SwaggerPublisherTracer");
    }
}
