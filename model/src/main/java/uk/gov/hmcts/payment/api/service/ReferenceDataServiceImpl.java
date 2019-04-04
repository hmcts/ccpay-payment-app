package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Service
public class ReferenceDataServiceImpl implements ReferenceDataService<SiteDTO> {

    private String serverAddress = "localhost";

    private int serverPort = 8080;

    @Override
    public List<SiteDTO> getSiteIDs() {
        URL url = null;
        try {
            url = new URL("http", serverAddress, serverPort, "reference-data/sites");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<SiteDTO>> response = restTemplate.exchange(
            url.toExternalForm(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<SiteDTO>>() {
            });
        return response.getBody();
    }
}
