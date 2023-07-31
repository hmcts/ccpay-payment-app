package uk.gov.hmcts.payment.api.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.model.LegacySite;
import uk.gov.hmcts.payment.api.model.LegacySiteRepository;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentChannelRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentMethodRepository;
import uk.gov.hmcts.payment.api.model.PaymentProvider;
import uk.gov.hmcts.payment.api.model.PaymentProviderRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;

import java.util.List;

@RestController
@Tag(name = "Payment Reference Data")
@RequestMapping("/refdata")
public class PaymentReferenceDataController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentReferenceDataController.class);

    private final PaymentStatusRepository paymentStatusRepository;

    private final PaymentProviderRepository paymentProviderRespository;

    private final PaymentMethodRepository paymentMethodRepository;

    private final PaymentChannelRepository paymentChannelRepository;

    private final LegacySiteRepository legacySiteRepository;


    @Autowired
    public PaymentReferenceDataController(PaymentStatusRepository paymentStatusRepository, PaymentProviderRepository paymentProviderRespository,
                                          PaymentMethodRepository paymentMethodRepository, PaymentChannelRepository paymentChannelRepository, LegacySiteRepository legacySiteRepository) {
        this.paymentStatusRepository = paymentStatusRepository;
        this.paymentProviderRespository = paymentProviderRespository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentChannelRepository = paymentChannelRepository;
        this.legacySiteRepository = legacySiteRepository;
    }

    @Operation(summary = "Payment channels", description = "Get all payment channels")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment channels found"),
        @ApiResponse(responseCode = "404", description = "Payment channels not found")
    })
    @GetMapping("/channels")
    @ResponseStatus(HttpStatus.OK)
    public List<PaymentChannel> findAllPaymentChannels() {
        List<PaymentChannel> paymentChannels = paymentChannelRepository.findAll();

        return paymentChannels;
    }

    @Operation(summary = "Payment methods", description = "Get all payment methods")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment methods found"),
        @ApiResponse(responseCode = "404", description = "Payment methods not found")
    })
    @GetMapping("/methods")
    public List<PaymentMethod> findAllPaymentMethods() {
        List<PaymentMethod> paymentMethods = paymentMethodRepository.findAll();

        return paymentMethods;
    }

    @Operation(summary = "Payment providers", description = "Get all payment providers")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment providers found"),
        @ApiResponse(responseCode = "404", description = "Payment providers not found")
    })
    @GetMapping("/providers")
    @ResponseStatus(HttpStatus.OK)
    public List<PaymentProvider> findAllPaymentProviders() {
        List<PaymentProvider> paymentProviders = paymentProviderRespository.findAll();

        return paymentProviders;
    }

    @Operation(summary = "Payment status", description = "Get all payment statuses")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment status found"),
        @ApiResponse(responseCode = "404", description = "Payment status not found")
    })
    @GetMapping("/status")
    public List<PaymentStatus> findAllPaymentStatuses() {
        List<PaymentStatus> paymentStatus = paymentStatusRepository.findAll();

        return paymentStatus;
    }

    @Operation(summary = "Get allowed legacy sites")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Legacy sites retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Legacy sites not found"),
        @ApiResponse(responseCode = "401", description = "Credentials are required to access this resource")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/legacy-sites")
    public List<LegacySite> findAllLegacySites() {
        return legacySiteRepository.findAll();
    }
}
