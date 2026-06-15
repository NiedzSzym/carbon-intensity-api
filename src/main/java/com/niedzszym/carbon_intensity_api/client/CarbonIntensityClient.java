package com.niedzszym.carbon_intensity_api.client;

import com.niedzszym.carbon_intensity_api.client.dto.CiGenerationResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CarbonIntensityClient {
    private static final String CI_URL = "https://api.carbonintensity.org.uk/";
    private final RestTemplate restTemplate;

    public CarbonIntensityClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CiGenerationResponse getCiGenerationInterval(String from, String to) {
        return callGetMethod("generation/{from}/{to}",
                CiGenerationResponse.class,
                from, to);
    }

    private <T> T callGetMethod(String url, Class<T> responseType, Object... objects) {
        return restTemplate.getForObject(CI_URL + url, responseType, objects);
    }
}
